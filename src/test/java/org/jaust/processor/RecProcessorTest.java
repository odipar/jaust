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
 * Tests for {@link RecProcessor} (the Faust recursive {@code ~} operator).
 */
class RecProcessorTest {

    private Context ctx;

    @BeforeEach
    void setUp() {
        ctx = new DefaultContext(44100);
    }

    /**
     * Simple accumulator: external constant input 1.0, p2 = identity (wire).
     * <pre>
     *   p1 = binD(+)  (2 inputs, 1 output)
     *   p2 = wire     (1 input,  1 output)  →  r = 1
     *   combined: 1 external input, 1 output
     *   out(t) = out(t-1) + external(t)
     * </pre>
     * With external = 1.0: out(0)=1, out(1)=2, out(2)=3, out(9)=10
     */
    @Test
    void accumulator_externalConstantOne() {
        Processor p1  = ctx.binD(Double::sum);
        Processor p2  = ctx.wire(Signal.Type.DOUBLE);
        Processor rec = ctx.rec(p1, p2);

        // Provide external input = constant 1.0 via seq
        Processor combined = ctx.seq(DefaultProcessorArray.of(ctx.valD(1.0), rec));

        SignalArray out = combined.apply();
        assertEquals(1.0,  out.at(0).doubleAt(0), 1e-9);
        assertEquals(2.0,  out.at(0).doubleAt(1), 1e-9);
        assertEquals(3.0,  out.at(0).doubleAt(2), 1e-9);
        assertEquals(10.0, out.at(0).doubleAt(9), 1e-9);
    }

    /**
     * Feedback initial state is zero: out(0) = out(-1) + external(0) = 0 + 1.0 = 1.0.
     */
    @Test
    void feedbackInitialisedToZero() {
        Processor rec      = ctx.rec(ctx.binD(Double::sum), ctx.wire(Signal.Type.DOUBLE));
        Processor combined = ctx.seq(DefaultProcessorArray.of(ctx.valD(1.0), rec));

        // At t=0 the feedback is 0 (initial state), so result = 0 + 1 = 1
        assertEquals(1.0, combined.apply().at(0).doubleAt(0), 1e-9);
    }
 
}
