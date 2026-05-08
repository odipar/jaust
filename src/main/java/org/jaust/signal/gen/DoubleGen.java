package org.jaust.signal.gen;

import org.jaust.Context;
import org.jaust.signal.DoubleSignal;

import java.util.function.LongToDoubleFunction;

public record DoubleGen(Context context, LongToDoubleFunction f) implements DoubleSignal {
    public double doubleAt(long time) {
        if (time < 0) return 0;
        return f.applyAsDouble(time);
    }
}
