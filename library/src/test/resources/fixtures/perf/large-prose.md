# Large Prose Fixture

This fixture is designed to represent long prose-heavy markdown with repeated paragraphs and moderate inline syntax.

Paragraph 1: Markdown translation should preserve structure while translating only human-readable text. The pipeline must keep links like [project home](https://example.com/home) stable and avoid modifying inline code such as `./gradlew :library:testDebugUnitTest`.

Paragraph 2: The purpose of this fixture is to stress tokenization and chunking logic under realistic narrative content. It includes punctuation, list references, and short technical snippets to validate predictable performance in repeated runs.

Paragraph 3: For deterministic tests, translation is mocked and no network call is made. This isolates parser and token-stream costs. It also helps detect regressions introduced by AST traversal or reconstruction changes.

Paragraph 4: Markdown structures should remain renderable by CommonMark-compatible engines after translation. Any changes to token identity assignment (`T1`, `T2`, ...) should remain stable across equivalent input.

Paragraph 5: The fixture is intentionally repetitive. Markdown translation should preserve structure while translating only human-readable text. The pipeline must keep links like [project home](https://example.com/home) stable and avoid modifying inline code such as `./gradlew :library:testDebugUnitTest`.

Paragraph 6: The fixture is intentionally repetitive. The purpose of this fixture is to stress tokenization and chunking logic under realistic narrative content. It includes punctuation, list references, and short technical snippets to validate predictable performance in repeated runs.

Paragraph 7: The fixture is intentionally repetitive. For deterministic tests, translation is mocked and no network call is made. This isolates parser and token-stream costs. It also helps detect regressions introduced by AST traversal or reconstruction changes.

Paragraph 8: The fixture is intentionally repetitive. Markdown structures should remain renderable by CommonMark-compatible engines after translation. Any changes to token identity assignment (`T1`, `T2`, ...) should remain stable across equivalent input.

Paragraph 9: Markdown translation should preserve structure while translating only human-readable text. The pipeline must keep links like [project home](https://example.com/home) stable and avoid modifying inline code such as `./gradlew :library:testDebugUnitTest`.

Paragraph 10: The purpose of this fixture is to stress tokenization and chunking logic under realistic narrative content. It includes punctuation, list references, and short technical snippets to validate predictable performance in repeated runs.
