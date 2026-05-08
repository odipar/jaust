package org.jaust.signal.gen;

import org.jaust.Context;
import org.jaust.signal.BooleanSignal;

import java.util.function.LongPredicate;

public record BoolGen(Context context, LongPredicate f) implements BooleanSignal {
    public boolean boolAt(long time) {
        if (time < 0) return false;
        return f.test(time);
    }
}
