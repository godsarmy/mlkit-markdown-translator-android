# Complex Structure Fixture

> Quote level 1 with a [reference](https://example.com/reference) and `inline code`.
>
> - Nested bullet inside quote
> - Another nested bullet with **strong emphasis**

## Installation

1. Download the package.
2. Run `./gradlew spotlessCheck`.
3. Verify output in the sample app.

- Bullet item one with [link](https://example.com/one)
- Bullet item two with ![image alt text](https://example.com/image.png)
- Bullet item three with `code_span()` and _italic text_.

### Table section

| Key | Value | Notes |
| :-- | :---: | ---: |
| Language | English | Source |
| Language | Spanish | Target |
| Mode | AST | Preferred |
| Mode | Regex fallback | Backup |

### Code blocks

```kotlin
fun translate(input: String): String {
    return input
}
```

```bash
./gradlew :library:testDebugUnitTest
./gradlew :sample:assembleDebug
```

### Repeated mixed section A

Paragraph with [label](https://example.com/label) and ![alt](https://example.com/alt.png) plus `inline` segments.

> Blockquote with nested `token` references and [docs](https://example.com/docs).

- list item alpha
- list item beta
- list item gamma

### Repeated mixed section B

Paragraph with [label](https://example.com/label) and ![alt](https://example.com/alt.png) plus `inline` segments.

> Blockquote with nested `token` references and [docs](https://example.com/docs).

- list item alpha
- list item beta
- list item gamma
