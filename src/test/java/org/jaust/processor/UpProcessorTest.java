package org.jaust.processor;

import org.jaust.Context;
import org.jaust.Processor;
import org.jaust.Signal;
import org.jaust.context.DefaultContext;
import org.jaust.signal.SignalArray;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the upsampling combinator {@code Context#up(Processor)}.
 *
 * <p>Upsampling by a factor of N injects (N-1) zero samples between every two
 * consecutive source samples (zero-stuffing). Only target times that align exactly
 * with a source sample boundary return the source value; all others return 0.</p>
 */
class UpProcessorTest {

    /** 2x upsample of a constant double: source sample appears at even target times, zeros at odd times. */
    @Test
    void upsampleDouble_2x_zeroStuffing() {
        Context src = new DefaultContext(1);   // 1 Hz source
        Context tgt = new DefaultContext(2);   // 2 Hz target  (ratio = 2)

        Processor srcProc = src.valD(3.0);
        Processor up = tgt.up(srcProc);

        SignalArray out = up.apply();
        // t=0 → srcTime=0 → 3.0
        assertEquals(3.0, out.at(0).doubleAt(0), 1e-9);
        // t=1 → not aligned → 0.0
        assertEquals(0.0, out.at(0).doubleAt(1), 1e-9);
        // t=2 → srcTime=1 → 3.0
        assertEquals(3.0, out.at(0).doubleAt(2), 1e-9);
        // t=3 → not aligned → 0.0
        assertEquals(0.0, out.at(0).doubleAt(3), 1e-9);
    }

    /** 2x upsample of a ramp int signal. */
    @Test
    void upsampleInt_2x_zeroStuffing() {
        Context src = new DefaultContext(1);
        Context tgt = new DefaultContext(2);

        Processor srcProc = src.genI(t -> (int) t); // 0, 1, 2, 3, …
        Processor up = tgt.up(srcProc);

        SignalArray out = up.apply();
        assertEquals(0, out.at(0).intAt(0)); // srcTime 0 → 0
        assertEquals(0, out.at(0).intAt(1)); // not aligned → 0
        assertEquals(1, out.at(0).intAt(2)); // srcTime 1 → 1
        assertEquals(0, out.at(0).intAt(3)); // not aligned → 0
        assertEquals(2, out.at(0).intAt(4)); // srcTime 2 → 2
    }

    /** 2x upsample of a ramp long signal. */
    @Test
    void upsampleLong_2x_zeroStuffing() {
        Context src = new DefaultContext(1);
        Context tgt = new DefaultContext(2);

        Processor srcProc = src.genL(t -> t * 10L);
        Processor up = tgt.up(srcProc);

        SignalArray out = up.apply();
        assertEquals(0L,  out.at(0).longAt(0));
        assertEquals(0L,  out.at(0).longAt(1));
        assertEquals(10L, out.at(0).longAt(2));
        assertEquals(0L,  out.at(0).longAt(3));
        assertEquals(20L, out.at(0).longAt(4));
    }

    /** 2x upsample of a boolean signal: false injected at non-aligned times. */
    @Test
    void upsampleBool_2x_falseInjected() {
        Context src = new DefaultContext(1);
        Context tgt = new DefaultContext(2);

        Processor srcProc = src.valB(true);
        Processor up = tgt.up(srcProc);

        SignalArray out = up.apply();
        assertTrue(out.at(0).boolAt(0));   // aligned → source value (true)
        assertFalse(out.at(0).boolAt(1));  // non-aligned → false (zero)
        assertTrue(out.at(0).boolAt(2));   // aligned → source value (true)
        assertFalse(out.at(0).boolAt(3));  // non-aligned → false (zero)
    }

    /** Upsample with same frequency (1:1) should return identical values. */
    @Test
    void upsampleDouble_sameFrequency_identity() {
        Context ctx = new DefaultContext(44100);

        Processor srcProc = ctx.genD(t -> (double) t);
        Processor up = ctx.up(srcProc);

        SignalArray out = up.apply();
        for (long t = 0; t < 10; t++) {
            assertEquals((double) t, out.at(0).doubleAt(t), 1e-9);
        }
    }

    /** Output type matches source output type. */
    @Test
    void outTypeMatchesSource() {
        Context src = new DefaultContext(1);
        Context tgt = new DefaultContext(4);

        assertArrayEquals(new Signal.Type[]{Signal.Type.DOUBLE}, tgt.up(src.valD(0.0)).outType());
        assertArrayEquals(new Signal.Type[]{Signal.Type.INT},    tgt.up(src.valI(0)).outType());
        assertArrayEquals(new Signal.Type[]{Signal.Type.LONG},   tgt.up(src.valL(0L)).outType());
        assertArrayEquals(new Signal.Type[]{Signal.Type.BOOL},   tgt.up(src.valB(false)).outType());
    }

    /** Input type is empty (no external inputs; source is applied internally). */
    @Test
    void inTypeIsEmpty() {
        Context src = new DefaultContext(1);
        Context tgt = new DefaultContext(2);
        assertArrayEquals(new Signal.Type[]{}, tgt.up(src.valD(1.0)).inType());
    }
}
