package org.jaust.signal.gen;

import org.jaust.Context;
import org.jaust.signal.LongSignal;

import java.util.function.LongUnaryOperator;

public record LongGen(Context context, LongUnaryOperator f) implements LongSignal {
    public long longAt(long time) {
        if (time < 0) return 0;
        return f.applyAsLong(time);
    }
}
