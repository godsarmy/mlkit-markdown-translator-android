# Huge Document Seed Fixture

This seed fixture is expanded in performance tests to generate a super-large markdown input.

## Seed block

Paragraph: preserving markdown structure is required while translating human-readable text.

- bullet alpha with [link](https://example.com/alpha)
- bullet beta with `inline code`
- bullet gamma with ![image alt](https://example.com/gamma.png)

> Quote section: keep delimiters stable and translate only text.

| Column A | Column B | Column C |
| --- | --- | --- |
| value a1 | `code-a1` | [ref-a1](https://example.com/a1) |
| value a2 | `code-a2` | [ref-a2](https://example.com/a2) |

```text
do-not-translate --code-like-content
```

Seed tail sentence with autolink <https://example.com/auto> and more text.
