package org.jaust.processor;

import org.jaust.Context;
import org.jaust.Processor;
import org.jaust.Signal;
import org.jaust.context.DefaultContext;
import org.jaust.signal.SignalArray;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the downsampling combinator {@code Context#down(Processor)}.
 *
 * <p>Each target sample averages all source samples in the exact rational window
 * [ceil(t*srcFreq/tgtFreq), ceil((t+1)*srcFreq/tgtFreq)) using ceiling division,
 * which correctly handles coprime source and target frequencies without double-counting
 * or missing source samples. Boolean signals use majority voting.</p>
 */
class DownProcessorTest {

    /** 2x downsample of a constant double: result should equal the constant. */
    @Test
    void downsampleDouble_2x_constant() {
        Context src = new DefaultContext(2);   // 2 Hz source
        Context tgt = new DefaultContext(1);   // 1 Hz target  (ratio = 2)

        Processor srcProc = src.valD(4.0);
        Processor down = tgt.down(srcProc);

        SignalArray out = down.apply();
        // average of 4.0, 4.0 = 4.0
        assertEquals(4.0, out.at(0).doubleAt(0), 1e-9);
        assertEquals(4.0, out.at(0).doubleAt(1), 1e-9);
    }

    /** 2x downsample averages pairs of source samples. */
    @Test
    void downsampleDouble_2x_averaging() {
        Context src = new DefaultContext(2);
        Context tgt = new DefaultContext(1);

        // source: 0, 1, 2, 3, 4, 5, …
        Processor srcProc = src.genD(t -> (double) t);
        Processor down = tgt.down(srcProc);

        SignalArray out = down.apply();
        // t=0: avg(0,1) = 0.5
        assertEquals(0.5, out.at(0).doubleAt(0), 1e-9);
        // t=1: avg(2,3) = 2.5
        assertEquals(2.5, out.at(0).doubleAt(1), 1e-9);
        // t=2: avg(4,5) = 4.5
        assertEquals(4.5, out.at(0).doubleAt(2), 1e-9);
    }

    /** 4x downsample averages groups of 4. */
    @Test
    void downsampleDouble_4x_averaging() {
        Context src = new DefaultContext(4);
        Context tgt = new DefaultContext(1);

        Processor srcProc = src.genD(t -> (double) t); // 0,1,2,3,4,5,6,7,…
        Processor down = tgt.down(srcProc);

        SignalArray out = down.apply();
        // t=0: avg(0,1,2,3) = 1.5
        assertEquals(1.5, out.at(0).doubleAt(0), 1e-9);
        // t=1: avg(4,5,6,7) = 5.5
        assertEquals(5.5, out.at(0).doubleAt(1), 1e-9);
    }

    /** 2x downsample of an int ramp: floors the average. */
    @Test
    void downsampleInt_2x_averaging() {
        Context src = new DefaultContext(2);
        Context tgt = new DefaultContext(1);

        Processor srcProc = src.genI(t -> (int) t); // 0,1,2,3,…
        Processor down = tgt.down(srcProc);

        SignalArray out = down.apply();
        // t=0: avg(0,1)=0 (integer truncation)
        assertEquals(0, out.at(0).intAt(0));
        // t=1: avg(2,3)=2
        assertEquals(2, out.at(0).intAt(1));
    }

    /** 2x downsample of a long ramp. */
    @Test
    void downsampleLong_2x_averaging() {
        Context src = new DefaultContext(2);
        Context tgt = new DefaultContext(1);

        Processor srcProc = src.genL(t -> t * 10L); // 0,10,20,30,…
        Processor down = tgt.down(srcProc);

        SignalArray out = down.apply();
        // t=0: avg(0,10) = 5
        assertEquals(5L, out.at(0).longAt(0));
        // t=1: avg(20,30) = 25
        assertEquals(25L, out.at(0).longAt(1));
    }

    /** 2x downsample of a boolean signal using majority vote. */
    @Test
    void downsampleBool_2x_majorityVote() {
        Context src = new DefaultContext(4);  // 4 Hz
        Context tgt = new DefaultContext(1);  // 1 Hz

        // source pattern per group of 4: [true,true,false,false] → majority true (2 of 4 → tie → false)
        // Use 3 trues: [true,true,true,false] → majority true
        Processor srcProc = src.genB(t -> t % 4 < 3); // true,true,true,false,true,true,true,false,…
        Processor down = tgt.down(srcProc);

        SignalArray out = down.apply();
        assertTrue(out.at(0).boolAt(0));   // 3 true, 1 false → majority true
    }

    /** Downsample bool where majority is false. */
    @Test
    void downsampleBool_2x_majorityFalse() {
        Context src = new DefaultContext(4);
        Context tgt = new DefaultContext(1);

        // 1 true, 3 false per group → majority false
        Processor srcProc = src.genB(t -> t % 4 == 0);
        Processor down = tgt.down(srcProc);

        SignalArray out = down.apply();
        assertFalse(out.at(0).boolAt(0));
    }

    /** Downsample with same frequency (1:1) should return identical values. */
    @Test
    void downsampleDouble_sameFrequency_identity() {
        Context ctx = new DefaultContext(44100);

        Processor srcProc = ctx.genD(t -> (double) t);
        Processor down = ctx.down(srcProc);

        SignalArray out = down.apply();
        for (long t = 0; t < 10; t++) {
            assertEquals((double) t, out.at(0).doubleAt(t), 1e-9);
        }
    }

    /** Output type matches source output type. */
    @Test
    void outTypeMatchesSource() {
        Context src = new DefaultContext(4);
        Context tgt = new DefaultContext(1);

        assertArrayEquals(new Signal.Type[]{Signal.Type.DOUBLE}, tgt.down(src.valD(0.0)).outType());
        assertArrayEquals(new Signal.Type[]{Signal.Type.INT},    tgt.down(src.valI(0)).outType());
        assertArrayEquals(new Signal.Type[]{Signal.Type.LONG},   tgt.down(src.valL(0L)).outType());
        assertArrayEquals(new Signal.Type[]{Signal.Type.BOOL},   tgt.down(src.valB(false)).outType());
    }

    /** Input type is empty (no external inputs; source is applied internally). */
    @Test
    void inTypeIsEmpty() {
        Context src = new DefaultContext(4);
        Context tgt = new DefaultContext(1);
        assertArrayEquals(new Signal.Type[]{}, tgt.down(src.valD(1.0)).inType());
    }

    /**
     * Downsample with coprime prime frequencies (srcFreq=5, tgtFreq=3).
     * Windows (using ceiling division): [0,2), [2,4), [4,5).
     * Expected: avg(0,1)=0.5, avg(2,3)=2.5, avg(4)=4.0.
     */
    @Test
    void downsampleDouble_primeRatio_5to3() {
        Context src = new DefaultContext(5);  // 5 Hz source (prime)
        Context tgt = new DefaultContext(3);  // 3 Hz target (prime)

        Processor srcProc = src.genD(t -> t); // 0.0, 1.0, 2.0, 3.0, 4.0,...
        Processor down = tgt.down(srcProc);

        SignalArray out = down.apply();
        assertEquals(0.5, out.at(0).doubleAt(0), 1e-9); // avg(0, 1)
        assertEquals(2.5, out.at(0).doubleAt(1), 1e-9); // avg(2, 3)
        assertEquals(4.0, out.at(0).doubleAt(2), 1e-9); // avg(4)
    }

    /**
     * Downsample int with coprime prime frequencies (srcFreq=5, tgtFreq=3).
     * Windows: [0,2)→avg(0,1)=0, [2,4)→avg(2,3)=2, [4,5)→avg(4)=4.
     */
    @Test
    void downsampleInt_primeRatio_5to3() {
        Context src = new DefaultContext(5);
        Context tgt = new DefaultContext(3);

        Processor srcProc = src.genI(t -> (int) t);
        Processor down = tgt.down(srcProc);

        SignalArray out = down.apply();
        assertEquals(0, out.at(0).intAt(0)); // avg(0,1) = 0 (truncated)
        assertEquals(2, out.at(0).intAt(1)); // avg(2,3) = 2 (truncated)
        assertEquals(4, out.at(0).intAt(2)); // avg(4)   = 4
    }

    /**
     * Downsample long with coprime prime frequencies (srcFreq=5, tgtFreq=3).
     */
    @Test
    void downsampleLong_primeRatio_5to3() {
        Context src = new DefaultContext(5);
        Context tgt = new DefaultContext(3);

        Processor srcProc = src.genL(t -> t * 10L); // 0,10,20,30,40,...
        Processor down = tgt.down(srcProc);

        SignalArray out = down.apply();
        assertEquals(5L,  out.at(0).longAt(0)); // avg(0,10) = 5
        assertEquals(25L, out.at(0).longAt(1)); // avg(20,30) = 25
        assertEquals(40L, out.at(0).longAt(2)); // avg(40) = 40
    }

    /**
     * Downsample bool with coprime prime frequencies (srcFreq=5, tgtFreq=3).
     * Windows: [0,2): {true,true}→true; [2,4): {true,false}→false (tie); [4,5): {false}→false.
     */
    @Test
    void downsampleBool_primeRatio_5to3() {
        Context src = new DefaultContext(5);
        Context tgt = new DefaultContext(3);

        // src: [true, true, true, false, false, ...]  (first 3 true, next 2 false)
        Processor srcProc = src.genB(t -> t % 5 < 3);
        Processor down = tgt.down(srcProc);

        SignalArray out = down.apply();
        assertTrue(out.at(0).boolAt(0));   // window [0,2): true,true → majority true
        assertFalse(out.at(0).boolAt(1));  // window [2,4): true,false → tie → false
        assertFalse(out.at(0).boolAt(2));  // window [4,5): false → false
    }

    /**
     * Non-overlapping partition property: with coprime srcFreq=7, tgtFreq=3,
     * the windows returned by ceiling division are contiguous and non-overlapping.
     * A constant source produces the same constant at every target sample.
     */
    @Test
    void downsampleDouble_primeRatio_7to3_constant() {
        Context src = new DefaultContext(7);  // 7 Hz source (prime)
        Context tgt = new DefaultContext(3);  // 3 Hz target (prime)

        Processor srcProc = src.valD(1.0);
        Processor down = tgt.down(srcProc);

        SignalArray out = down.apply();
        for (long t = 0; t < 6; t++) {
            assertEquals(1.0, out.at(0).doubleAt(t), 1e-9,
                "constant signal should downsample to same constant at t=" + t);
        }
    }
}
