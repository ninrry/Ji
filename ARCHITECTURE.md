# Ji Architecture

Ji is an Android app for automatic expense tracking via payment screen recognition.

## Package Structure

```
luzzr.ji/
├── core/
│   ├── payment/          # Payment recognition pipeline
│   │   ├── PaymentCompletionClassifier   # Facade (API surface)
│   │   ├── PaymentCompletionRuleEngine   # Data-driven rule evaluator
│   │   ├── PaymentRecognitionManager     # Orchestrator (WorkManager + dedup)
│   │   └── PaymentFingerprint            # Dedup fingerprinting
│   ├── vlm/              # Vision-Language Model integration
│   │   ├── VlmClient                     # OpenAI-compatible VLM client
│   │   └── LocalFallbackRuleEngine       # Local rule fallback
│   ├── permissions/      # Accessibility & permission management
│   └── security/         # Secure storage
├── data/                 # Room database, DAOs, repositories
├── domain/
│   └── model/            # PaymentPlatform, PaymentKind, Transaction, etc.
└── feature/
    ├── home/             # Main transaction list & input
    ├── statistics/       # Charts and analytics
    ├── extrabill/        # Manual bill entry
    └── settings/         # VLM config, permissions, debug
```

## Payment Recognition Pipeline

```
Accessibility Event
       │
       ▼
PaymentCompletionClassifier.from(packageName, rawText)
       │
       ▼
PaymentCompletionRuleEngine.classify()
  ├── common_reject rules (failure, refund, processing)
  ├── platform.reject rules (bill list, history, message cards)
  └── platform.accept rules (success keywords by kind)
       │
       ▼
PaymentClassification { signal?, trace }
       │
       ├── signal == null → skip (not a payment completion)
       │
       ▼
PaymentRecognitionManager.process()
  ├── Room duplicate check (fingerprint + time window)
  ├── Screenshot capture & encrypt
  ├── WorkManager enqueue for VLM extraction
  │      │
  │      ▼
  │   VlmClient.extractPayment()
  │      ├── OpenAI-compatible API call
  │      ├── Response validation (amount, platform, kind, confidence)
  │      └── Fallback: LocalFallbackRuleEngine
  │
  └── Auto-record write to Room
```

## Key Design Decisions

- **Rule engine is JSON-driven**: `payment_completion_rules.json` in assets defines all platform-specific rules. New platforms/UI variants can be added by editing JSON without code changes.
- **Every classification produces a trace**: `PaymentRuleTrace` records ruleId, decision, matched keywords, and reason for debugging and audit.
- **VLM is secondary to rules**: The rule engine gates what enters the VLM pipeline. VLM only processes pages that pass the rule engine's accept signal.
- **Dedup uses fingerprinting**: `PaymentFingerprint` normalizes volatile text (timestamps, whitespace) and combines with time-window dedup to prevent double-recording.

## Build & Dependencies

- **Gradle**: 9.1.0, AGP 9.0.1
- **Language**: Kotlin, Jetpack Compose
- **Database**: Room with migrations (currently schema v5)
- **Background**: WorkManager for VLM processing
- **Networking**: OkHttp for VLM API calls
- **CI**: GitHub Actions (test + lint + assemble)

## Evolution Roadmap

See [docs/adr/0001-payment-recognition-pipeline.md](docs/adr/0001-payment-recognition-pipeline.md) for the current architecture decision and planned evolution:

1. **Done**: JSON rule engine with trace and fixture tests
2. **Next**: Hilt DI migration (after rule boundaries stabilize)
3. **Later**: Multi-module Gradle split (`:core:database`, `:core:payment`, `:domain`, etc.)
