# Performance Test Steps (Large/Complex Markdown)

This file defines a practical step-by-step plan to add performance testing for very large and complex Markdown inputs.

## Goal

Detect performance regressions early for the Markdown pipeline (preprocess → AST tokenization → chunking/reconstruction), without depending on network/ML Kit runtime variability.

## Scope

- Include: pure Java markdown pipeline performance
- Exclude: real ML Kit model download/network latency

---

## Step 1 - Add performance fixtures

Create fixture files under:

`library/src/test/resources/fixtures/perf/`

Add at least:

1. `large-prose.md` (many long paragraphs)
2. `complex-structure.md` (headings/lists/quotes/code/links/images)
3. `mixed-worst-case.md` (nested + token-dense + tables)
4. `huge-document.md` (multi-megabyte stress input)

---

## Step 2 - Add deterministic mock translation engine

In test code, use a fake engine that returns deterministic output immediately, for example:

`[[TRANSLATED:<input>]]`

Why: removes network/device noise and benchmarks only pipeline cost.

---

## Step 3 - Add JUnit performance regression test

Create test class:

`library/src/test/java/.../MarkdownPerformanceRegressionTest.java`

For each fixture:

1. warm up once
2. run N iterations (e.g., 5)
3. capture elapsed time using `System.nanoTime()`
4. compute avg/p95-like max
5. assert upper bounds with safe CI thresholds

Also capture:

- token count
- chunk count
- output length matches expected constraints

---

## Step 4 - Add memory sanity checks

Track approximate memory before/after run with `Runtime.getRuntime()` and assert:

- no unbounded growth across iterations
- no OOM under large fixture

Keep thresholds conservative to avoid flaky CI.

---

## Step 5 - Add optional local benchmark profile

Add an opt-in benchmark test path (manual/local) for deeper profiling:

- larger iteration count
- detailed logs (fixture size, tokens, chunks, elapsed)

Do not make this required for every PR.

---

## Step 6 - Wire CI for regression guard

In GitHub Actions, add a job (or step in unit-test job) to run:

`./gradlew :library:testDebugUnitTest --tests "*MarkdownPerformanceRegressionTest"`

Policy recommendation:

- fail on clear regression (e.g., >25-30% over baseline threshold)
- keep thresholds stable per runner type

---

## Step 7 - Define baseline and update policy

Store baseline numbers in docs (or test constants) and update only when:

- algorithm intentionally changes
- improvement/regression is reviewed and accepted

Document baseline update rationale in PR description.

---

## Step 8 - Acceptance criteria

Performance testing setup is complete when:

1. all perf fixtures are present
2. regression test runs in CI and is stable
3. thresholds are documented
4. large/complex markdown path has no crash or OOM
5. results are reproducible enough to catch meaningful regressions
