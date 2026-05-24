package org.jaust.filter;

import org.jaust.Context;
import org.jaust.signal.DoubleSignal;

// Ring-buffer cache of 256 values for DoubleSignal, anticipating sequential (back in time t-n) reads.
class SequentialDoubleCache implements DoubleSignal {
    private static final int SIZE = 256;
    private final DoubleSignal source;
    private final double[] ring = new double[SIZE];
    private long computed; // values at indices 0..computed-1 have been computed sequentially

    SequentialDoubleCache(DoubleSignal source) {
        this.source = source;
        this.computed = 0;
    }

    public Context context() { return source.context(); }

    public double doubleAt(long time) {
        if (time < 0) return source.doubleAt(time);
        // Ensure all values up to 'time' are computed sequentially
        if (time >= computed) {
            for (long i = computed; i <= time; i++) {
                ring[(int) (i & (SIZE - 1))] = source.doubleAt(i);
            }
            computed = time + 1;
        }
        // Return from ring buffer if within window
        if (time >= computed - SIZE) {
            return ring[(int) (time & (SIZE - 1))];
        }
        // Fell out of ring buffer; recompute directly
        return source.doubleAt(time);
    }
}
