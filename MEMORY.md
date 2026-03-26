# Project Memory

- Step 7 uses `TokenizedMarkdownDocument` reconstruction to preserve markdown syntax while translating only `TRANSLATABLE` tokens.
- Step 8 chunks AST-derived translation requests in `MarkdownStructureTranslator` using stable `@@MLMD_TOKEN_<id>@@` markers; token order must stay stable, so `translatableTokenMap()` uses `LinkedHashMap`.
- Keep new ML Kit code injectable/testable with fake clients so JVM unit tests can verify behavior without requiring real Android/ML Kit runtime execution.
- Regex fallback now has real token protection (`MarkdownProtectionPipeline`) and can be disabled via `MarkdownTranslationOptions.setEnableRegexFallbackProtection(false)`.
- Step 12 added fixture-based golden tests under `library/src/test/resources/fixtures` for AST reconstruction and fallback roundtrip behavior.
- README now includes v1 limitations and Android integration/migration instructions for `android-nixpkgs`.
- README now also includes Java quickstart snippets, a repository-style Java example path, JitPack setup notes, and integration guardrails (SDK/permission/threading/lifecycle/R8).
