# Project Memory

- Step 7 uses `TokenizedMarkdownDocument` reconstruction to preserve markdown syntax while translating only `TRANSLATABLE` tokens.
- Step 8 chunks AST-derived translation requests in `MarkdownStructureTranslator` using stable `@@MLMD_TOKEN_<id>@@` markers; token order must stay stable, so `translatableTokenMap()` uses `LinkedHashMap`.
- Keep new ML Kit code injectable/testable with fake clients so JVM unit tests can verify behavior without requiring real Android/ML Kit runtime execution.
