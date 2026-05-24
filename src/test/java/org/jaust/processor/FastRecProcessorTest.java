package org.jaust.processor;

import org.jaust.Context;
import org.jaust.Processor;
import org.jaust.Signal;
import org.jaust.context.DefaultContext;
import org.jaust.processor.array.DefaultProcessorArray;
import org.jaust.signal.SignalArray;
import org.jaust.signal.array.DefaultArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link FastRecProcessor} – an optimized version of {@link RecProcessor}.
 * Includes correctness checks and a performance comparison using a Long recursive counter
 * for 100 million iterations.
 */
class FastRecProcessorTest {

    private Context ctx;

    @BeforeEach
    void setUp() {
        ctx = new DefaultContext(44100);
    }

    // ─── Correctness Tests ───────────────────────────────────────────────────────

    @Test
    void accumulator_externalConstantOne_double() {
        Processor p1  = ctx.binD(Double::sum);
        Processor p2  = ctx.wire(Signal.Type.DOUBLE);
        Processor rec = new FastRecProcessor(p1, p2);

        Processor combined = ctx.seq(DefaultProcessorArray.of(ctx.valD(1.0), rec));
        SignalArray out = combined.apply();

        assertEquals(1.0,  out.at(0).doubleAt(0), 1e-9);
        assertEquals(2.0,  out.at(0).doubleAt(1), 1e-9);
        assertEquals(3.0,  out.at(0).doubleAt(2), 1e-9);
        assertEquals(10.0, out.at(0).doubleAt(9), 1e-9);
    }

    @Test
    void accumulator_externalConstantOne_long() {
        Processor p1  = ctx.binL(Long::sum);
        Processor p2  = ctx.wire(Signal.Type.LONG);
        Processor rec = new FastRecProcessor(p1, p2);

        Processor combined = ctx.seq(DefaultProcessorArray.of(ctx.valL(1L), rec));
        SignalArray out = combined.apply();

        assertEquals(1L, out.at(0).longAt(0));
        assertEquals(2L, out.at(0).longAt(1));
        assertEquals(3L, out.at(0).longAt(2));
        assertEquals(10L, out.at(0).longAt(9));
        assertEquals(100L, out.at(0).longAt(99));
    }

    @Test
    void feedbackInitialisedToZero() {
        Processor rec      = new FastRecProcessor(ctx.binD(Double::sum), ctx.wire(Signal.Type.DOUBLE));
        Processor combined = ctx.seq(DefaultProcessorArray.of(ctx.valD(1.0), rec));

        assertEquals(1.0, combined.apply().at(0).doubleAt(0), 1e-9);
    }

    // ─── Performance Test: Long recursive counter 100 million iterations ─────────

    @Test
    void longCounter_100million_fastRecProcessor() {
        // Build a Long counter: out(t) = out(t-1) + 1
        Processor p1  = ctx.binL(Long::sum);
        Processor p2  = ctx.wire(Signal.Type.LONG);
        Processor fastRec = new FastRecProcessor(p1, p2);
        Processor combined = ctx.seq(DefaultProcessorArray.of(ctx.valL(1L), fastRec));
        SignalArray out = combined.apply();

        long N = 100_000_000L;

        long startTime = System.nanoTime();
        long result = out.at(0).longAt(N - 1);
        long elapsed = System.nanoTime() - startTime;

        assertEquals(N, result);
        System.out.println("FastRecProcessor: 100M iterations in " + (elapsed / 1_000_000) + " ms");
    }

    @Test
    void longCounter_100million_recProcessor() {
        // Build a Long counter: out(t) = out(t-1) + 1
        // RecProcessor uses deep recursion – this would stack overflow for large N,
        // so we test with a small value to demonstrate the limitation, and
        // extrapolate the time per call.
        Processor p1  = ctx.binL(Long::sum);
        Processor p2  = ctx.wire(Signal.Type.LONG);
        Processor rec = ctx.rec(p1, p2);
        Processor combined = ctx.seq(DefaultProcessorArray.of(ctx.valL(1L), rec));
        SignalArray out = combined.apply();

        // RecProcessor can handle small values; verify correctness
        assertEquals(1L, out.at(0).longAt(0));
        assertEquals(10L, out.at(0).longAt(9));
        assertEquals(100L, out.at(0).longAt(99));

        // Measure with 1K iterations (safe stack depth)
        int smallN = 1_000;
        long startTime = System.nanoTime();
        long result = out.at(0).longAt(smallN - 1);
        long elapsed = System.nanoTime() - startTime;

        assertEquals((long) smallN, result);
        double msPerIteration = (double) elapsed / smallN;
        double projected100M = msPerIteration * 100_000_000 / 1_000_000;

        System.out.println("RecProcessor: " + smallN + " iterations in " + (elapsed / 1_000_000) + " ms");
        System.out.println("RecProcessor: projected 100M iterations would take ~" + (long) projected100M + " ms");
        System.out.println("RecProcessor: NOTE - would StackOverflow for 100M iterations due to O(n) recursion depth");
    }

    @Test
    void performanceComparison() {
        // Direct comparison: FastRecProcessor handles 100M; RecProcessor can only handle ~10K
        Processor p1  = ctx.binL(Long::sum);
        Processor p2  = ctx.wire(Signal.Type.LONG);

        // FastRecProcessor: 100M iterations
        Processor fastRec = new FastRecProcessor(p1, p2);
        Processor fastCombined = ctx.seq(DefaultProcessorArray.of(ctx.valL(1L), fastRec));
        SignalArray fastOut = fastCombined.apply();

        long N = 100_000_000L;
        long fastStart = System.nanoTime();
        long fastResult = fastOut.at(0).longAt(N - 1);
        long fastElapsed = System.nanoTime() - fastStart;
        assertEquals(N, fastResult);

        // RecProcessor: 1K iterations (maximum safe depth without StackOverflow)
        Processor recProc = ctx.rec(p1, p2);
        Processor recCombined = ctx.seq(DefaultProcessorArray.of(ctx.valL(1L), recProc));
        SignalArray recOut = recCombined.apply();

        int smallN = 1_000;
        long recStart = System.nanoTime();
        long recResult = recOut.at(0).longAt(smallN - 1);
        long recElapsed = System.nanoTime() - recStart;
        assertEquals((long) smallN, recResult);

        // Compute speedup ratio (normalized to same iteration count)
        double fastPerIter = (double) fastElapsed / N;
        double recPerIter = (double) recElapsed / smallN;
        double speedup = recPerIter / fastPerIter;

        System.out.println("=== Performance Comparison ===");
        System.out.println("FastRecProcessor: " + N + " iterations in " + (fastElapsed / 1_000_000) + " ms (" + String.format("%.2f", fastPerIter) + " ns/iter)");
        System.out.println("RecProcessor:     " + smallN + " iterations in " + (recElapsed / 1_000_000) + " ms (" + String.format("%.2f", recPerIter) + " ns/iter)");
        System.out.println("Speedup:          " + String.format("%.1f", speedup) + "x faster per iteration");
        System.out.println("FastRecProcessor handles 100M iterations without StackOverflow");
        System.out.println("RecProcessor would StackOverflow beyond ~1K-10K iterations (stack-depth dependent)");

        // FastRecProcessor should be significantly faster per iteration
        assertTrue(speedup > 1.0, "FastRecProcessor should be faster per iteration than RecProcessor");
    }
}
