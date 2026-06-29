# Payment Recognition System

## Overview

Ji's payment recognition identifies when the user is viewing a payment completion screen and automatically extracts transaction details. The system uses a layered approach: deterministic rule engine first, VLM (Vision-Language Model) second.

## Architecture

### Layer 1: Rule Engine Gate

**Purpose**: Fast, deterministic classification of screen text into accept/reject/unknown.

**Entry point**: `PaymentCompletionClassifier.from(packageName, rawText)`

**Rule file**: `app/src/main/assets/payment_completion_rules.json`

**Rule structure**:
```json
{
  "version": 1,
  "common_reject": [
    {
      "id": "common.not_completed",
      "any": ["失败", "取消", "关闭", "处理中", "待支付"]
    }
  ],
  "platforms": [
    {
      "id": "wechat",
      "platform": "WECHAT",
      "packages": ["com.tencent.mm"],
      "reject": [...],
      "accept": [
        {"id": "wechat.merchant_payment", "kind": "MERCHANT_PAYMENT", "any": ["付款成功", "支付成功"]}
      ]
    }
  ]
}
```

**Rule matching logic**:
- `all`: ALL keywords must be present in normalized text
- `any`: At least `min_match` (default 1) keywords must be present
- Rules are evaluated in order; first match wins
- Common reject rules run before platform-specific rules

**Output**: `PaymentClassification { signal?, trace }`
- `signal`: Non-null only on accept (contains platform + kind)
- `trace`: Always populated with ruleId, decision, matchedKeywords, reason

### Layer 2: VLM Extraction

**Purpose**: Extract structured transaction data (amount, category, note, trade_id) from the screen.

**Entry point**: `VlmClient.extractPayment(bitmap, platform, kind)`

**Flow**:
1. Encode screenshot as base64
2. Send to OpenAI-compatible VLM endpoint with structured prompt
3. Parse JSON response with strict validation:
   - `status == SUCCESS`
   - `platform`/`kind` must match expected values
   - `confidence` must be in `0.85..1.0`
   - `amount` must be a valid cent amount
   - `category` must be a known enum value
4. Fallback: `LocalFallbackRuleEngine` parses common wallet text patterns

### Layer 3: Dedup & Recording

**Purpose**: Prevent duplicate transaction recording.

**Mechanism**:
- `PaymentFingerprint`: Normalizes screen text (strip volatile content like timestamps), combines with platform + kind
- Time-window dedup: Same fingerprint within 15 minutes = duplicate
- Room `Transaction` table as single source of truth

## Adding a New Platform

1. Add platform enum to `PaymentPlatform`
2. Add platform rules to `payment_completion_rules.json`:
   ```json
   {
     "id": "new_platform",
     "platform": "NEW_PLATFORM",
     "packages": ["com.new.app"],
     "reject": [...],
     "accept": [...]
   }
   ```
3. Add fixture test cases to `PaymentCompletionClassifierFixtureTest`
4. Add fixture text files to `src/test/resources/payment-fixtures/new_platform/`

## Adding a New Payment Kind

1. Add enum to `PaymentKind`
2. Add accept rule with `"kind": "NEW_KIND"` to the platform's accept array in JSON
3. Update VLM prompt to recognize the new kind
4. Add fixture test coverage

## Test Strategy

- **Fixture tests**: 23+ fixture files covering all platforms, accept/reject scenarios, and edge cases. Each fixture asserts exact ruleId for traceability.
- **Inline tests**: `PaymentRecognitionTest` covers integration scenarios including dedup, fingerprinting, and VLM response parsing.
- **Rule trace assertions**: Every fixture test verifies the exact ruleId that matched, catching unintended rule reordering.

## Known Limitations

- Rules are bundled in assets; hot-reload or remote rule updates require a new APK release
- VLM extraction depends on external API availability; local fallback covers limited patterns
- No CI-based instrumented test; emulator smoke tests are manual
