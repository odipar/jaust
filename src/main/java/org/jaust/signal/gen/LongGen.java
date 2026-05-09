package org.jaust.signal.gen;

import org.jaust.Context;
import org.jaust.signal.LongSignal;
import java.util.function.LongUnaryOperator;

// Generates a long signal by applying a time-to-long function; returns 0 for negative time.
public record LongGen(Context context, LongUnaryOperator f) implements LongSignal {
    public long longAt(long time) {
        if (time < 0) return 0;
        return f.applyAsLong(time);
    }
}
