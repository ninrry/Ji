# Testing Guide

## Quick Start

```bash
# Run all unit tests
./gradlew testDebugUnitTest

# Run lint
./gradlew lintDebug

# Build debug APK
./gradlew assembleDebug

# Run all three (what CI runs)
./gradlew testDebugUnitTest lintDebug assembleDebug
```

## Test Structure

### Unit Tests (`src/test/`)

| Test Class | Purpose |
|---|---|
| `PaymentRecognitionTest` | Payment classifier logic, VLM parsing, dedup, fingerprinting |
| `PaymentCompletionClassifierFixtureTest` | Rule engine behavior via fixture files (trace assertions) |
| `AppDatabaseMigrationTest` | Room schema migrations |
| `FeatureScreensComposeTest` | Compose UI smoke tests |
| `SettingsScreenComposeTest` | Settings screen tests |
| `ViewModelTests` | ViewModel logic |

### Fixture Tests

Fixture files live in `src/test/resources/payment-fixtures/`:

```
payment-fixtures/
├── wechat/
│   ├── immediate_merchant_success.txt    # ACCEPT
│   ├── transfer_success.txt              # ACCEPT
│   ├── red_packet_sent.txt               # ACCEPT
│   ├── withdrawal_success.txt            # ACCEPT
│   ├── bill_detail_history.txt           # REJECT
│   ├── payment_message.txt              # REJECT
│   ├── bill_list_reject.txt             # REJECT
│   └── bill_detail_with_actions.txt     # REJECT
├── alipay/
│   ├── immediate_auto_debit_success.txt  # ACCEPT
│   ├── payment_success.txt              # ACCEPT
│   ├── huabei_repayment.txt             # ACCEPT
│   ├── bill_detail_history.txt          # REJECT
│   ├── message_card.txt                 # REJECT
│   ├── payment_message_reject.txt       # REJECT
│   ├── service_message_reject.txt       # REJECT
│   └── auto_debit_message_reject.txt    # REJECT
├── jd/
│   ├── immediate_success.txt            # ACCEPT
│   ├── baitiao_repayment.txt            # ACCEPT
│   └── bill_history.txt                 # REJECT
└── common/
    ├── failed_payment.txt               # REJECT
    ├── refund_success.txt               # REJECT
    ├── processing.txt                   # REJECT
    └── unsupported_package.txt          # REJECT
```

Each fixture test asserts:
- `decision` (ACCEPT/REJECT)
- `ruleId` (exact rule that matched)
- `signal.kind` (for ACCEPT cases)
- `matchedKeywords` is non-empty (for ACCEPT cases)

### Adding a New Fixture

1. Create `.txt` file in the appropriate `payment-fixtures/<platform>/` directory
2. Add a `FixtureCase` entry in `PaymentCompletionClassifierFixtureTest`
3. Run `./gradlew testDebugUnitTest --tests '*FixtureTest*'` to verify

## Instrumented Tests (`src/androidTest/`)

Requires a connected device or emulator:

```bash
./gradlew connectedDebugAndroidTest
```

## CI

GitHub Actions runs on every push/PR to `main`:
- `testDebugUnitTest` — all unit tests
- `lintDebug` — static analysis
- `assembleDebug` — build verification

Test reports and lint reports are uploaded as artifacts.
