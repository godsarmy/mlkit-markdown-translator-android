# ML Kit Markdown Translator

Reusable Android library for translating Markdown content with Google ML Kit while preserving Markdown structure (code blocks, links, headings, lists, and spacing) as much as possible.

## Project status

This repository is being built incrementally from the TODO roadmap in `ML-markdown-lib.TODO.md`.

## Planned modules

- `library/` — Android library module (core deliverable)
- `sample/` — Android sample app for manual verification
- `docs/` — design notes and API documentation

## API draft

See `docs/api.md` for the initial public API definition.

## Limitations (v1)

This library is designed to preserve Markdown structure during translation, but v1 still has known limits:

- advanced nested Markdown edge cases may not be perfectly preserved
- raw HTML blocks beyond currently supported patterns may not round-trip cleanly
- tables are not guaranteed unless explicitly handled by the active pipeline path
- reference-style link definitions may be imperfect if tokenization does not fully cover a case
- translator-driven punctuation drift can still occur in plain text regions

For production use, validate your own Markdown fixtures with the provided golden-test pattern under
`library/src/test/resources/fixtures`.

## License

Apache License 2.0. See `LICENSE`.
