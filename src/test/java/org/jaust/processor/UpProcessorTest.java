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
 * <p>For an integer ratio (tgtFreq = k * srcFreq), source samples appear at every k-th
 * target position and zeros fill the rest. For a rational ratio p/q (in lowest terms),
 * the upsampler distributes source samples optimally: each source sample i is placed at
 * target position round(i * tgtFreq / srcFreq), computed with integer arithmetic to
 * avoid floating-point imprecision. A target position t that doesn't own any source
 * sample returns zero (zero-stuffing). This ensures exactly srcFreq non-zero values
 * occur every tgtFreq target samples, even when srcFreq and tgtFreq are coprime primes.</p>
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

    /**
     * Upsample with coprime prime frequencies (srcFreq=3, tgtFreq=5).
     * In every 5 target samples exactly 3 must be non-zero (matching srcFreq=3 Hz).
     * Expected positions for genD(t -> t+1): t=0→1.0, t=1→0.0, t=2→2.0, t=3→3.0, t=4→0.0.
     */
    @Test
    void upsampleDouble_primeRatio_3to5() {
        Context src = new DefaultContext(3);   // 3 Hz source (prime)
        Context tgt = new DefaultContext(5);   // 5 Hz target (prime)

        Processor srcProc = src.genD(t -> t + 1.0); // 1.0, 2.0, 3.0, ...
        Processor up = tgt.up(srcProc);

        SignalArray out = up.apply();
        assertEquals(1.0, out.at(0).doubleAt(0), 1e-9); // src[0]
        assertEquals(0.0, out.at(0).doubleAt(1), 1e-9); // zero
        assertEquals(2.0, out.at(0).doubleAt(2), 1e-9); // src[1]
        assertEquals(3.0, out.at(0).doubleAt(3), 1e-9); // src[2]
        assertEquals(0.0, out.at(0).doubleAt(4), 1e-9); // zero
        assertEquals(4.0, out.at(0).doubleAt(5), 1e-9); // src[3]
    }

    /**
     * Upsample int with coprime prime frequencies (srcFreq=3, tgtFreq=5).
     */
    @Test
    void upsampleInt_primeRatio_3to5() {
        Context src = new DefaultContext(3);
        Context tgt = new DefaultContext(5);

        Processor srcProc = src.genI(t -> (int)(t + 1)); // 1, 2, 3, ...
        Processor up = tgt.up(srcProc);

        SignalArray out = up.apply();
        assertEquals(1, out.at(0).intAt(0)); // src[0]
        assertEquals(0, out.at(0).intAt(1)); // zero
        assertEquals(2, out.at(0).intAt(2)); // src[1]
        assertEquals(3, out.at(0).intAt(3)); // src[2]
        assertEquals(0, out.at(0).intAt(4)); // zero
    }

    /**
     * Upsample long with coprime prime frequencies (srcFreq=3, tgtFreq=5).
     */
    @Test
    void upsampleLong_primeRatio_3to5() {
        Context src = new DefaultContext(3);
        Context tgt = new DefaultContext(5);

        Processor srcProc = src.genL(t -> (t + 1) * 10L); // 10, 20, 30, ...
        Processor up = tgt.up(srcProc);

        SignalArray out = up.apply();
        assertEquals(10L, out.at(0).longAt(0)); // src[0]
        assertEquals(0L,  out.at(0).longAt(1)); // zero
        assertEquals(20L, out.at(0).longAt(2)); // src[1]
        assertEquals(30L, out.at(0).longAt(3)); // src[2]
        assertEquals(0L,  out.at(0).longAt(4)); // zero
    }

    /**
     * Upsample bool with coprime prime frequencies (srcFreq=3, tgtFreq=5).
     * source: [true, false, true, false, ...]
     */
    @Test
    void upsampleBool_primeRatio_3to5() {
        Context src = new DefaultContext(3);
        Context tgt = new DefaultContext(5);

        Processor srcProc = src.genB(t -> t % 2 == 0); // true, false, true, false, ...
        Processor up = tgt.up(srcProc);

        SignalArray out = up.apply();
        assertTrue(out.at(0).boolAt(0));   // src[0] = true
        assertFalse(out.at(0).boolAt(1)); // zero → false
        assertFalse(out.at(0).boolAt(2)); // src[1] = false
        assertTrue(out.at(0).boolAt(3));  // src[2] = true
        assertFalse(out.at(0).boolAt(4)); // zero → false
    }

    /**
     * Non-overlapping partition: for coprime srcFreq=3, tgtFreq=5, the number of
     * non-zero target samples in any complete second equals srcFreq=3.
     */
    @Test
    void upsampleDouble_primeRatio_nonZeroCountPerSecond() {
        Context src = new DefaultContext(3);
        Context tgt = new DefaultContext(5);

        Processor srcProc = src.valD(1.0);
        Processor up = tgt.up(srcProc);

        SignalArray out = up.apply();
        long nonZeroCount = 0;
        for (long t = 0; t < 5; t++) {
            if (out.at(0).doubleAt(t) != 0.0) nonZeroCount++;
        }
        assertEquals(3, nonZeroCount); // srcFreq=3 non-zeros per tgtFreq=5 target samples
    }
}
