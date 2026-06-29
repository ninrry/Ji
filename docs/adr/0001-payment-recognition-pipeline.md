# ADR-0001: Payment Recognition Pipeline

## Status

Accepted (2026-06-29)

## Context

Ji needs to identify payment completion screens from multiple platforms (WeChat, Alipay, JD) to trigger automatic expense recording. The original implementation used hardcoded keyword lists in a single `PaymentCompletionClassifier` Kotlin object, with platform-specific `when` branches and `contains` checks.

Problems with the original approach:
- All rules were hardcoded in Kotlin, requiring code changes and APK releases for every new platform variant
- No trace/explainability for why a screen was accepted or rejected
- Rule changes couldn't be tested independently from code
- Real-world screen variants were accumulated as ad-hoc patches without systematic organization

## Decision

Extract a **data-driven rule engine** (`PaymentCompletionRuleEngine`) that loads rules from a JSON file (`payment_completion_rules.json`), with the following design:

1. **JSON rule file in assets**: All platform-specific keywords, reject patterns, and accept signals defined in a single versioned JSON file
2. **Every classification produces a trace**: `PaymentRuleTrace` records the matched rule ID, decision, keywords, and human-readable reason
3. **Facade preserves API**: `PaymentCompletionClassifier` remains the public API surface, delegating to the rule engine internally
4. **Fixture-based testing**: Real screen text samples stored as `.txt` files, with test assertions on exact ruleId matches

## Consequences

### Positive
- New platforms/UI variants can be added by editing JSON without Kotlin code changes
- Every classification is traceable (ruleId + matched keywords)
- Fixture tests catch unintended rule behavior changes
- Rule file is version-controlled and reviewable
- Foundation for future remote rule updates or A/B testing

### Negative
- JSON parsing adds a small startup cost (negligible for ~100 rules)
- Rule logic limited to keyword matching; complex temporal or contextual rules would need code extensions
- Two code paths to maintain (assets for runtime, classpath for tests)

### Risks
- Rule file could become large and hard to review if many platforms are added; mitigate with rule ID naming conventions and documentation
- JSON syntax errors would crash the app; mitigate with `require()` validation in `fromJson()`

## Alternatives Considered

1. **Keep hardcoded Kotlin rules**: Simpler, but doesn't solve the maintenance problem
2. **Full rule engine with expression language** (e.g., JSONata, CEL): Overkill for keyword matching; adds dependency and complexity
3. **Database-backed rules with remote sync**: Future possibility, but premature for current scale

## Next Steps

1. ~~Extract rule engine with JSON rules and trace~~ (done)
2. ~~Fixture test coverage for all platforms~~ (done)
3. Hilt DI migration (after rule boundaries stabilize)
4. Multi-module Gradle split
5. Consider remote rule updates if platform variants grow significantly
