# Payment Gateway

A payment gateway API that lets merchants process card payments through an acquiring bank (simulated) and retrieve the details of previous payments.

## Running the solution

Requirements: JDK 17 and Docker.

```bash
docker-compose up        # start the bank simulator on port 8080
./gradlew bootRun        # start the gateway on port 8090
./gradlew test           # run the tests (the simulator is not needed)
```

API documentation (Swagger UI): http://localhost:8090/swagger-ui/index.html

The simulator authorizes card numbers ending in an odd digit, declines those ending in an even digit, and returns an error for those ending in 0.

## API

### `POST /payment`

Processes a card payment.

```json
{
  "card_number": "2222405343248877",
  "expiry_month": 4,
  "expiry_year": 2030,
  "currency": "GBP",
  "amount": 1050,
  "cvv": "123"
}
```

Response:

```json
{
  "id": "a287addb-a364-4923-be64-fbcb56eba7fe",
  "status": "Authorized",
  "card_number_last_four": "8877",
  "expiry_month": 4,
  "expiry_year": 2030,
  "currency": "GBP",
  "amount": 1050
}
```

| Scenario | Status | HTTP |
|---|---|---|
| Bank authorizes the payment | Authorized | 200 |
| Bank declines the payment | Declined | 200 |
| Invalid request (bank is not called) | Rejected | 400 |
| Bank error, timeout, or empty or invalid response | - (not stored) | 502 |

A rejected request returns every field error at once (the expiry date is checked once the fields are valid):

```json
{
  "status": "Rejected",
  "errors": ["CVV must be 3-4 digits", "Currency must be one of GBP, EUR, USD"]
}
```

### `GET /payment/{id}`

Retrieves a previously processed payment. Returns the same body as the POST response.

| Scenario | HTTP |
|---|---|
| Payment found | 200 |
| Payment id not found | 404 |
| Payment id is not a valid UUID | 400 |

## Key design considerations

- **Layered structure.** Controller (HTTP), service (payment flow), `BankClient` (bank call) and repository (storage), so each part has one job and can be tested on its own.
- **Validation.** Field rules are Bean Validation annotations on `PostPaymentRequest`. The expiry check is in a separate `PaymentValidationService`, as it depends on the current date and on two fields together, which makes it easier to unit test. Invalid requests are rejected before the bank is called.
- **Card data.** Only the last four digits of the card number are stored or returned. The full card number and CVV are only used during the request (validation and the bank call) and are never logged.
- **Error handling.** All errors are handled in one `@ControllerAdvice`, giving a consistent response format. Rejected payments have their own response type (`status` and a list of `errors`), so "Rejected" only appears when the gateway refused a payment; other errors return a simple `message`.
- **Bank integration.** `BankClient` calls the simulator using the existing `RestTemplate` (10 second timeouts), with the URL in `application.properties`. Any failure (error status, timeout, connection failure, or an empty or invalid response) becomes a `BankUnavailableException`, returned as 502 Bad Gateway: the gateway is healthy, but the bank behind it failed.
- **Storage.** The in-memory repository from the template, changed to a `ConcurrentHashMap` as requests are handled concurrently. Payments are only stored once the bank has responded.

## Assumptions

- A card expiring in the **current month** is still valid, as cards are valid until the end of their expiry month.
- Only **GBP, EUR and USD** are supported.
- Amounts must be **positive integers in minor units**. Decimal values such as `10.50` are rejected rather than truncated, so a wrong amount is never charged.
- The brief doesn't define a format for rejections, so they return **400** with status `Rejected` and a list of every validation error.
- If the bank fails, the payment is **not stored**, as the only valid statuses are Authorized and Declined. A timeout can leave the outcome unknown (the bank may have processed it); see future improvements.
- The gateway only validates what the brief asks for (the format of each field). Whether the card and CVV are genuine is left to the bank.

## Testing

`./gradlew test` runs all the tests without the bank simulator.

- **Controller integration tests** (`@SpringBootTest` + `MockMvc`) run the full application with only `BankClient` mocked. They cover a rejection for each field rule, multiple errors at once, an expired card, a decimal amount and a malformed body, as well as authorized, declined, bank unavailable (502), GET after POST, payment not found and an invalid id. The rejection tests also check that the bank is never called.
- **Unit tests:**
  - `PaymentGatewayService`: the correct details are sent to the bank, and nothing is stored if the bank fails.
  - `BankClient`, using `MockRestServiceServer`: the JSON sent to the bank, mapping the bank's response, a 503, an empty body, and a response without `authorized`.
  - `PaymentValidationService`: expiry date edge cases (current month, last month, null and out-of-range months).

The integration with the real simulator was checked manually with curl.

## Future improvements

Left out to keep the solution simple:

- **Idempotency keys and reconciliation**, so merchants can safely retry and a timed-out payment isn't charged twice.
- **Retries and a circuit breaker** around the bank call.
- **A separate payment entity** for storage (e.g. to keep the bank's authorization code), rather than storing the API response.
- **A persistent database** instead of in-memory storage, so payments survive a restart.
- **Merchant authentication**, so merchants can only see their own payments.
- **Observability**: metrics such as the number of authorized, declined and rejected payments and how long the bank takes to respond, and a request id in every log line so a single payment can be traced. Currently each processed payment is logged with its id and status (never card data).
- **Packaging and deployment**: a Dockerfile so the gateway runs in a container alongside the simulator, and a CI pipeline that builds and tests every push. The bank URL can already be changed per environment with a `BANK_URL` environment variable.
- **Automated end-to-end tests** against the real bank simulator (e.g. using Testcontainers to start it).