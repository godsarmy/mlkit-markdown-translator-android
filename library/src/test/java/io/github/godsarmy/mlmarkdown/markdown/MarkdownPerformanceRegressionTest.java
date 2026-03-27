package io.github.godsarmy.mlmarkdown.markdown;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.github.godsarmy.mlmarkdown.api.TranslationCallback;
import io.github.godsarmy.mlmarkdown.engine.TranslationEngine;
import io.github.godsarmy.mlmarkdown.model.TokenizedMarkdownDocument;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

public class MarkdownPerformanceRegressionTest {
    private static final int WARMUP_ITERATIONS =
            Integer.getInteger("mlmd.perf.warmupIterations", 1);
    private static final int MEASURED_ITERATIONS =
            Integer.getInteger("mlmd.perf.measuredIterations", 3);
    private static final int HUGE_REPEAT = Integer.getInteger("mlmd.perf.hugeRepeat", 120);
    private static final int CHUNK_LENGTH = Integer.getInteger("mlmd.perf.chunkLength", 350);
    private static final double THRESHOLD_MULTIPLIER =
            doubleProperty("mlmd.perf.thresholdMultiplier", 1.0d);
    private static final boolean VERBOSE = Boolean.getBoolean("mlmd.perf.verbose");

    @Test
    public void largeAndComplexFixtures_completeWithinRegressionThresholds() {
        Map<String, Long> maxElapsedMillisByFixture = new LinkedHashMap<>();
        maxElapsedMillisByFixture.put("fixtures/perf/large-prose.md", 5000L);
        maxElapsedMillisByFixture.put("fixtures/perf/complex-structure.md", 7000L);
        maxElapsedMillisByFixture.put("fixtures/perf/mixed-worst-case.md", 7000L);

        AstTokenModelBuilder builder = new AstTokenModelBuilder();
        MarkdownStructureTranslator translator =
                new MarkdownStructureTranslator(
                        new DeterministicMockTranslationEngine(), CHUNK_LENGTH);

        for (Map.Entry<String, Long> entry : maxElapsedMillisByFixture.entrySet()) {
            String fixturePath = entry.getKey();
            long maxElapsedMillis = entry.getValue();
            String markdown = readFixture(fixturePath);

            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                executePipeline(builder, translator, markdown);
            }

            long maxObservedNanos = 0L;
            int observedTokenCount = 0;
            int observedChunkCount = 0;
            for (int i = 0; i < MEASURED_ITERATIONS; i++) {
                PipelineResult result = executePipeline(builder, translator, markdown);
                maxObservedNanos = Math.max(maxObservedNanos, result.elapsedNanos);
                observedTokenCount = result.tokenCount;
                observedChunkCount = result.chunkCount;
            }

            long maxObservedMillis = nanosToMillis(maxObservedNanos);
            long allowedMillis = Math.round(maxElapsedMillis * THRESHOLD_MULTIPLIER);
            assertTrue(
                    fixturePath
                            + " expected max <="
                            + allowedMillis
                            + "ms but was "
                            + maxObservedMillis
                            + "ms",
                    maxObservedMillis <= allowedMillis);
            assertTrue(fixturePath + " should produce tokens", observedTokenCount > 0);
            assertTrue(fixturePath + " should produce translatable chunks", observedChunkCount > 0);

            log(
                    "fixture=%s observedMaxMs=%d allowedMs=%d tokens=%d chunks=%d",
                    fixturePath,
                    maxObservedMillis,
                    allowedMillis,
                    observedTokenCount,
                    observedChunkCount);
        }
    }

    @Test
    public void hugeFixtureSeed_expandedInput_hasStableMemoryAndNoFailure() {
        String hugeSeed = readFixture("fixtures/perf/huge-document.md");
        String hugeMarkdown = repeatSeed(hugeSeed, HUGE_REPEAT);

        AstTokenModelBuilder builder = new AstTokenModelBuilder();
        MarkdownStructureTranslator translator =
                new MarkdownStructureTranslator(
                        new DeterministicMockTranslationEngine(), CHUNK_LENGTH);

        Runtime runtime = Runtime.getRuntime();
        long memoryBefore = usedMemory(runtime);
        PipelineResult firstRun = executePipeline(builder, translator, hugeMarkdown);
        long afterFirstRun = usedMemory(runtime);
        PipelineResult secondRun = executePipeline(builder, translator, hugeMarkdown);
        long afterSecondRun = usedMemory(runtime);

        assertTrue("huge input should produce many tokens", firstRun.tokenCount > 100);
        assertTrue("huge input should produce many chunks", firstRun.chunkCount > 10);
        assertNotNull("translation output should not be null", firstRun.output);
        assertTrue("translation output should not be empty", !firstRun.output.isEmpty());

        long growthFirstRun = Math.max(0L, afterFirstRun - memoryBefore);
        long growthSecondRun = Math.max(0L, afterSecondRun - afterFirstRun);

        // Conservative sanity threshold to avoid flaky CI while still catching runaway growth.
        long maxAllowedGrowthBytes = 256L * 1024L * 1024L;
        assertTrue(
                "first run memory growth too high: " + growthFirstRun,
                growthFirstRun < maxAllowedGrowthBytes);
        assertTrue(
                "second run memory growth too high: " + growthSecondRun,
                growthSecondRun < maxAllowedGrowthBytes);
        assertTrue("second run should complete", secondRun.elapsedNanos > 0L);

        log(
                "huge seed repeat=%d elapsed1Ms=%d elapsed2Ms=%d tokens=%d chunks=%d growth1Bytes=%d growth2Bytes=%d",
                HUGE_REPEAT,
                nanosToMillis(firstRun.elapsedNanos),
                nanosToMillis(secondRun.elapsedNanos),
                firstRun.tokenCount,
                firstRun.chunkCount,
                growthFirstRun,
                growthSecondRun);
    }

    private static PipelineResult executePipeline(
            AstTokenModelBuilder builder, MarkdownStructureTranslator translator, String markdown) {
        long start = System.nanoTime();
        TokenizedMarkdownDocument document = builder.build(markdown);
        int chunkCount = translator.chunkTranslatableTokens(document).size();

        TestCallback callback = new TestCallback();
        translator.translate(document, "en", "es", callback);
        long elapsedNanos = System.nanoTime() - start;

        if (callback.error != null) {
            throw new AssertionError("Unexpected translation error", callback.error);
        }

        return new PipelineResult(
                elapsedNanos, document.getTokens().size(), chunkCount, callback.output);
    }

    private static String repeatSeed(String seed, int times) {
        StringBuilder builder = new StringBuilder(seed.length() * times);
        for (int i = 0; i < times; i++) {
            builder.append(seed).append("\n\n");
        }
        return builder.toString();
    }

    private static long usedMemory(Runtime runtime) {
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private static long nanosToMillis(long nanos) {
        return nanos / 1_000_000L;
    }

    private static double doubleProperty(String key, double defaultValue) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private static void log(String format, Object... args) {
        if (!VERBOSE) {
            return;
        }
        System.out.println("[PERF] " + String.format(format, args));
    }

    private static String readFixture(String path) {
        try (InputStream stream =
                MarkdownPerformanceRegressionTest.class
                        .getClassLoader()
                        .getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing fixture: " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read fixture: " + path, e);
        }
    }

    private static final class PipelineResult {
        private final long elapsedNanos;
        private final int tokenCount;
        private final int chunkCount;
        private final String output;

        private PipelineResult(long elapsedNanos, int tokenCount, int chunkCount, String output) {
            this.elapsedNanos = elapsedNanos;
            this.tokenCount = tokenCount;
            this.chunkCount = chunkCount;
            this.output = output;
        }
    }

    private static final class TestCallback implements TranslationCallback {
        private String output;
        private Exception error;

        @Override
        public void onSuccess(String translatedText) {
            this.output = translatedText;
        }

        @Override
        public void onFailure(Exception error) {
            this.error = error;
        }
    }

    private static final class DeterministicMockTranslationEngine implements TranslationEngine {
        private static final Pattern MARKER_PATTERN = Pattern.compile("@@MLMD_TOKEN_[^@]+@@");

        @Override
        public void translate(
                String text,
                String sourceLanguage,
                String targetLanguage,
                TranslationCallback callback) {
            Matcher matcher = MARKER_PATTERN.matcher(text);
            StringBuilder translated = new StringBuilder();
            int lastEnd = 0;

            while (matcher.find()) {
                String segment = text.substring(lastEnd, matcher.start());
                if (!segment.isEmpty()) {
                    translated.append("[[TR:").append(segment).append("]] ");
                }
                translated.append(matcher.group());
                lastEnd = matcher.end();
            }

            String trailing = text.substring(lastEnd);
            if (!trailing.isEmpty()) {
                translated.append("[[TR:").append(trailing).append("]] ");
            }

            callback.onSuccess(translated.toString());
        }
    }
}
