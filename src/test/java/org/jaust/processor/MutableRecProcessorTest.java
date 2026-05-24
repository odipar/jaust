package org.jaust.processor;

import org.jaust.Context;
import org.jaust.Processor;
import org.jaust.Signal;
import org.jaust.context.DefaultContext;
import org.jaust.processor.array.DefaultProcessorArray;
import org.jaust.signal.SignalArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link MutableRecProcessor} – the mutable-array-based recursive composition.
 */
class MutableRecProcessorTest {

    private Context ctx;

    @BeforeEach
    void setUp() {
        ctx = new DefaultContext(44100);
    }

    /**
     * Accumulator: out(t) = out(t-1) + 1.0
     * Verifies correct iterative computation over many samples.
     */
    @Test
    void accumulator_manySteps() {
        Processor rec = ctx.rec(ctx.binD(Double::sum), ctx.wire(Signal.Type.DOUBLE));
        Processor combined = ctx.seq(DefaultProcessorArray.of(ctx.valD(1.0), rec));
        SignalArray out = combined.apply();

        for (int t = 0; t < 100; t++) {
            assertEquals(t + 1.0, out.at(0).doubleAt(t), 1e-9, "at t=" + t);
        }
    }

    /**
     * Verifies that querying a later time then an earlier time works
     * (tests that cached values are reused correctly).
     */
    @Test
    void outOfOrderAccess() {
        Processor rec = ctx.rec(ctx.binD(Double::sum), ctx.wire(Signal.Type.DOUBLE));
        Processor combined = ctx.seq(DefaultProcessorArray.of(ctx.valD(1.0), rec));
        SignalArray out = combined.apply();

        // Access t=50 first, then t=10
        assertEquals(51.0, out.at(0).doubleAt(50), 1e-9);
        assertEquals(11.0, out.at(0).doubleAt(10), 1e-9);
        // Access t=100 (beyond previously computed)
        assertEquals(101.0, out.at(0).doubleAt(100), 1e-9);
    }

    /**
     * Verifies that negative time returns zero.
     */
    @Test
    void negativeTimeReturnsZero() {
        Processor rec = ctx.rec(ctx.binD(Double::sum), ctx.wire(Signal.Type.DOUBLE));
        Processor combined = ctx.seq(DefaultProcessorArray.of(ctx.valD(1.0), rec));
        SignalArray out = combined.apply();

        assertEquals(0.0, out.at(0).doubleAt(-1), 1e-9);
        assertEquals(0.0, out.at(0).doubleAt(-100), 1e-9);
    }

    /**
     * Int accumulator: verifies int signal type works with feedback.
     */
    @Test
    void intAccumulator() {
        Processor rec = ctx.rec(ctx.binI(Integer::sum), ctx.wire(Signal.Type.INT));
        Processor combined = ctx.seq(DefaultProcessorArray.of(ctx.valI(1), rec));
        SignalArray out = combined.apply();

        assertEquals(1, out.at(0).intAt(0));
        assertEquals(2, out.at(0).intAt(1));
        assertEquals(10, out.at(0).intAt(9));
    }

    /**
     * Large time step: verifies the array grows correctly.
     */
    @Test
    void largeTimeStep() {
        Processor rec = ctx.rec(ctx.binD(Double::sum), ctx.wire(Signal.Type.DOUBLE));
        Processor combined = ctx.seq(DefaultProcessorArray.of(ctx.valD(1.0), rec));
        SignalArray out = combined.apply();

        // Jump directly to a large time value
        assertEquals(1001.0, out.at(0).doubleAt(1000), 1e-9);
    }

    /**
     * Verifies that MutableRecProcessor is actually used (instance check).
     */
    @Test
    void usedByDefault() {
        Processor rec = ctx.rec(ctx.binD(Double::sum), ctx.wire(Signal.Type.DOUBLE));
        assertInstanceOf(MutableRecProcessor.class, rec);
    }
}
