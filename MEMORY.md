# Project Memory

- Step 7 uses `TokenizedMarkdownDocument` reconstruction to preserve markdown syntax while translating only `TRANSLATABLE` tokens.
- Step 8 chunks AST-derived translation requests in `MarkdownStructureTranslator` using stable `@@MLMD_TOKEN_<id>@@` markers; token order must stay stable, so `translatableTokenMap()` uses `LinkedHashMap`.
- Keep new ML Kit code injectable/testable with fake clients so JVM unit tests can verify behavior without requiring real Android/ML Kit runtime execution.
- Regex fallback now has real token protection (`MarkdownProtectionPipeline`) and can be disabled via `MarkdownTranslationOptions.setEnableRegexFallbackProtection(false)`.
- Step 12 added fixture-based golden tests under `library/src/test/resources/fixtures` for AST reconstruction and fallback roundtrip behavior.
- README now includes v1 limitations and Android integration/migration instructions for `android-nixpkgs`.
- README now also includes Java quickstart snippets, a repository-style Java example path, JitPack setup notes, and integration guardrails (SDK/permission/threading/lifecycle/R8).
- AST parsing now enables Flexmark table extension; GFM-style table delimiters/alignment are preserved in AST translation flow, and regex fallback protects whole table blocks.
- Direct unit tests for `MarkdownStructureTranslator` are important because end-to-end translator tests can miss chunk boundary and per-token fallback error-propagation regressions.
- Sample app styling now uses a dark ML Kit-like blue palette (`mlkit_*` color tokens) applied to theme, preview surfaces, controls, and button accents.
- Whitespace preservation around protected segments is now configurable via `MarkdownTranslationOptions.setPreserveWhitespaceAroundProtectedSegments(...)` (default true) to support languages like Chinese/Japanese.
- Markwon core alone does not render pipe tables; sample app must include `io.noties.markwon:ext-tables` and register `TablePlugin` in `Markwon.builder(...)`.
- ML Kit translate dependency is now configurable via root `gradle.properties` (`mlkitTranslateVersion`), and README documents app-level override strategies for artifact integrations.
- Next performance-observability improvement is stage timing via optional listener (prepare/translate/restore/total), keeping API non-breaking and disabled by default.
- Timing report now includes `totalTokenCount` (AST token count when tokenized document exists; fallback protected-token count when regex token store is used).
- After cutting a new release tag, keep README dependency snippets/version labels aligned immediately (latest is `v0.1.2`).
