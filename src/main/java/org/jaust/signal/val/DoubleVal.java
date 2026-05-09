package org.jaust.signal.val;

import org.jaust.Context;
import org.jaust.signal.DoubleSignal;

// Constant double signal; always returns the same value regardless of time.
public record DoubleVal(Context context, double value) implements DoubleSignal {
    public double doubleAt(long time) {
        if (time < 0) return 0;
        return value;
    }
}