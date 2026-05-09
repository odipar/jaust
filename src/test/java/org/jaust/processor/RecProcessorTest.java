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
     * Returns a 1-input / 1-output identity (wire) processor that passes its signal through
     * unchanged. Used as p2 in the basic accumulator test.
     */
    private Processor wire() {
        return new DefaultProcessor() {
            public Context context() { return ctx; }
            public Signal.Type[] inType()  { return new Signal.Type[]{Signal.Type.DOUBLE}; }
            public Signal.Type[] outType() { return new Signal.Type[]{Signal.Type.DOUBLE}; }
            public SignalArray apply(SignalArray signal) { return DefaultArray.a(signal.at(0)); }
        };
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
        Processor p2  = wire();
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
        Processor rec      = ctx.rec(ctx.binD(Double::sum), wire());
        Processor combined = ctx.seq(DefaultProcessorArray.of(ctx.valD(1.0), rec));

        // At t=0 the feedback is 0 (initial state), so result = 0 + 1 = 1
        assertEquals(1.0, combined.apply().at(0).doubleAt(0), 1e-9);
    }

    /**
     * Leaky accumulator: p2 scales the feedback by 0.5.
     * <pre>
     *   p2(x) = x * 0.5  (implemented as a 1-in/1-out DefaultProcessor)
     *   out(t) = out(t-1)*0.5 + 1.0
     * </pre>
     * out(0) = 0*0.5 + 1 = 1.0
     * out(1) = 1.0*0.5 + 1 = 1.5
     * out(2) = 1.5*0.5 + 1 = 1.75
     */
    @Test
    void leakyAccumulator_feedbackScaledByHalf() {
        Processor scaleHalf = new DefaultProcessor() {
            public Context context() { return ctx; }
            public Signal.Type[] inType()  { return new Signal.Type[]{Signal.Type.DOUBLE}; }
            public Signal.Type[] outType() { return new Signal.Type[]{Signal.Type.DOUBLE}; }
            public SignalArray apply(SignalArray signal) {
                Signal in = signal.at(0);
                return DefaultArray.a(new org.jaust.signal.DoubleSignal() {
                    public Context context() { return ctx; }
                    public double doubleAt(long time) { return in.doubleAt(time) * 0.5; }
                });
            }
        };

        Processor rec      = ctx.rec(ctx.binD(Double::sum), scaleHalf);
        Processor combined = ctx.seq(DefaultProcessorArray.of(ctx.valD(1.0), rec));

        SignalArray out = combined.apply();
        assertEquals(1.0,  out.at(0).doubleAt(0), 1e-9);
        assertEquals(1.5,  out.at(0).doubleAt(1), 1e-9);
        assertEquals(1.75, out.at(0).doubleAt(2), 1e-9);
    }

    /**
     * inType/outType wiring: p1 has 2 inputs, 1 output; p2 has 1 input, 1 output → r=1.
     * Combined inType should have 1 DOUBLE (p - r = 2 - 1 = 1).
     */
    @Test
    void inTypeAndOutTypeAreCorrect() {
        Processor rec = ctx.rec(ctx.binD(Double::sum), wire());

        assertEquals(1, rec.inType().length);
        assertEquals(Signal.Type.DOUBLE, rec.inType()[0]);
        assertEquals(1, rec.outType().length);
        assertEquals(Signal.Type.DOUBLE, rec.outType()[0]);
    }

    /**
     * Fluent API: p1.rec(p2) produces the same result as ctx.rec(p1, p2).
     */
    @Test
    void fluentApi_recMethod() {
        Processor rec = ctx.binD(Double::sum).rec(wire());
        assertNotNull(rec);
        assertEquals(1, rec.outType().length);
        assertEquals(Signal.Type.DOUBLE, rec.outType()[0]);
    }

    /**
     * Signals must honour the Signal contract: querying an earlier time after a later time
     * must still return the correct value (no reliance on monotonically-increasing queries).
     * <pre>
     *   accumulator with external = 1.0: out(t) = t + 1
     * </pre>
     * Query t=9 first, then t=0 – both must be correct.
     */
    @Test
    void outOfOrderQuery_earlierTimeAfterLaterTime() {
        Processor rec      = ctx.rec(ctx.binD(Double::sum), wire());
        Processor combined = ctx.seq(DefaultProcessorArray.of(ctx.valD(1.0), rec));

        SignalArray out = combined.apply();
        assertEquals(10.0, out.at(0).doubleAt(9), 1e-9);  // query t=9 first
        assertEquals(1.0,  out.at(0).doubleAt(0), 1e-9);  // then query t=0 (must not use cached t=9)
        assertEquals(5.0,  out.at(0).doubleAt(4), 1e-9);  // then an intermediate time
    }
}
