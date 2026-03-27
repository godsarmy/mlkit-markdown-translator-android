# Performance Baseline (v0.1.0)

This file documents performance regression thresholds and update policy for
`MarkdownPerformanceRegressionTest`.

## CI regression thresholds (default profile)

These values are encoded in test constants (with optional multiplier):

- `fixtures/perf/large-prose.md` → max **5000 ms**
- `fixtures/perf/complex-structure.md` → max **7000 ms**
- `fixtures/perf/mixed-worst-case.md` → max **7000 ms**

Huge fixture sanity assertions:

- expanded seed should produce many tokens/chunks
- no failure/OOM in repeated runs
- conservative memory-growth checks under **256 MB** per run window

## Optional local benchmark profile

You can run a heavier local profile by overriding system properties:

```bash
./gradlew :library:testDebugUnitTest --tests "*MarkdownPerformanceRegressionTest" \
  -Dmlmd.perf.warmupIterations=2 \
  -Dmlmd.perf.measuredIterations=8 \
  -Dmlmd.perf.hugeRepeat=220 \
  -Dmlmd.perf.chunkLength=350 \
  -Dmlmd.perf.thresholdMultiplier=1.0 \
  -Dmlmd.perf.verbose=true
```

## Baseline update policy

Update thresholds only when one of the following is true:

1. algorithmic behavior intentionally changed
2. measurable and reviewed performance regression/improvement is accepted
3. CI runner behavior changed and the team agrees to re-baseline

When updating thresholds, include in PR description:

- before/after observed times
- reason for baseline update
- whether change is temporary or intended long-term
