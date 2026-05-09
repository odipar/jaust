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
 * Tests for {@link DivProcessor} (the Faust split {@code <:} operator).
 */
class DivProcessorTest {

    private Context ctx;

    @BeforeEach
    void setUp() {
        ctx = new DefaultContext(44100);
    }

    /**
     * 1-output split to 2-input: both inputs of p2 receive the same signal.
     * p1 = constant 3.0, p2 = add (sums its two inputs)
     * Expected: 3.0 + 3.0 = 6.0
     */
    @Test
    void splitOne_toTwo_broadcastsSameSignal() {
        Processor p1 = ctx.valD(3.0);                    // 0 inputs, 1 output (3.0)
        Processor p2 = ctx.binD(Double::sum);             // 2 inputs, 1 output (a+b)
        Processor div = ctx.div(p1, p2);

        // div has same inputs as p1 (none) and same outputs as p2
        assertArrayEquals(p1.inType(), div.inType());
        assertArrayEquals(p2.outType(), div.outType());

        SignalArray out = div.apply();
        assertEquals(6.0, out.at(0).doubleAt(0), 1e-9);
        assertEquals(6.0, out.at(0).doubleAt(1), 1e-9);
    }

    /**
     * 2-output split to 4-input: round-robin distribution.
     * p1 produces (1.0, 2.0); p2 adds pairs of inputs.
     * p2 receives (1.0, 2.0, 1.0, 2.0) and the par'd adder produces (1.0+2.0, 1.0+2.0) = (3.0, 3.0).
     */
    @Test
    void splitTwo_toFour_roundRobinDistribution() {
        Processor c1 = ctx.valD(1.0);
        Processor c2 = ctx.valD(2.0);
        Processor p1 = ctx.par(DefaultProcessorArray.of(c1, c2));                  // 2 outputs: [1.0, 2.0]

        Processor adder = ctx.binD(Double::sum);
        Processor p2 = ctx.par(DefaultProcessorArray.of(adder, adder));             // 4 inputs, 2 outputs

        Processor div = ctx.div(p1, p2);

        SignalArray out = div.apply();
        assertEquals(2, out.length());
        assertEquals(3.0, out.at(0).doubleAt(0), 1e-9);     // 1.0 + 2.0
        assertEquals(3.0, out.at(1).doubleAt(0), 1e-9);     // 1.0 + 2.0
    }

    /**
     * Fluent API: p1.div(p2) should produce the same result as ctx.div(p1, p2).
     */
    @Test
    void fluentApi_divMethod() {
        Processor p1 = ctx.valD(5.0);
        Processor p2 = ctx.binD(Double::sum);
        Processor div = p1.div(p2);

        SignalArray out = div.apply();
        assertEquals(10.0, out.at(0).doubleAt(0), 1e-9);
    }

    /**
     * inType/outType wiring check.
     */
    @Test
    void inTypeAndOutTypeAreCorrect() {
        Processor p1 = ctx.valD(1.0);
        Processor p2 = ctx.binD(Double::sum);
        Processor div = ctx.div(p1, p2);

        assertArrayEquals(new Signal.Type[]{}, div.inType());
        assertArrayEquals(new Signal.Type[]{Signal.Type.DOUBLE}, div.outType());
    }
}
