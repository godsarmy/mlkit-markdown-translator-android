# Translation Architecture

This document defines the markdown translation architecture using `flexmark-java` while preserving syntax and layout.

## Goals

- Translate only human-readable content.
- Preserve Markdown syntax and structure.
- Keep output renderable by CommonMark-compatible engines.

## Processing pipeline

1. Parse source markdown with `flexmark-java` into AST.
2. Convert AST into internal token stream/model.
3. Mark each token as one of:
   - `translatable`
   - `protected` (literal, non-translatable)
   - `structural` (layout/syntax delimiters)
4. Send only `translatable` tokens to translation engine.
5. Rebuild final markdown from original token order with translated text substituted.

## Node classification

### Translatable nodes

These contain user-visible language content and should be translated:

- `Text` inside paragraphs
- `Text` inside headings
- `Text` inside blockquotes
- `Text` inside list items
- link text (label) in `[label](url)`
- image alt text in `![alt](url)`
- emphasis/strong contained text

### Protected nodes (non-translatable)

These must remain unchanged:

- fenced code blocks (content and fence markers)
- inline code spans
- link destinations/URLs
- image destinations/URLs
- autolinks/raw URLs
- raw HTML/code-like blocks (v1 preserved as-is)

### Structural nodes

These define markdown layout and delimiters and are never translated directly:

- heading markers (`#`, `##`, ...)
- list containers (`BulletList`, `OrderedList`)
- list item delimiters/indentation
- blockquote markers (`>`)
- emphasis delimiters (`*`, `_`, `**`, `__`)
- paragraph and block boundaries
- newline/blank-line separators

## Mapping translated text back into nodes

### Token identity

- Every translatable region gets a stable token id (`T1`, `T2`, ...).
- The token id is associated with source span metadata where possible.

### Replacement rules

- Replace only token payload text, never neighboring structural/protected tokens.
- Preserve token order exactly.
- Preserve all original protected token values exactly.

### Failure strategy

- If chunk marker parsing fails (markers missing/mutated after translation), fallback to per-token translation for that chunk.
- If translation engine returns failure for a chunk/token call, propagate failure via callback.

## Implementation constraints

- Prefer AST-driven tokenization over regex-only logic.
- Regex is allowed only for pre-normalization (for example `<block>...</block>` conversion) and targeted fallback edge cases.
- Reconstruction fidelity is prioritized over aggressive text transformations.

## Hybrid fallback mode

v1 uses this hybrid path:

1. Normalize line endings and custom `<block>...</block>` tags.
2. Try AST/token-stream preparation first.
3. If AST preparation fails, fallback to regex protection mode.

Operational behavior:

- Preferred mode: `AST_TOKEN_STREAM`
  - Source is tokenized by Flexmark-based model builder.
  - Reconstruction is driven by ordered tokens.
  - Translation runs in chunks with token markers; if markers are lost/mutated by the translation engine,
    the pipeline falls back to per-token translation for that chunk.
- Fallback mode: `REGEX_FALLBACK`
  - Triggered when AST/token model build throws at preparation stage.
  - Legacy protection/restoration pipeline is used when regex fallback protection is enabled.
  - If regex fallback protection is disabled, fallback uses normalized markdown text directly.

For option-level behavior (`normalizeCustomBlockTags`, `protectAutolinks`,
`enableRegexFallbackProtection`), see [`docs/api.md`](api.md).
