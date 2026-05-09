package org.jaust.signal.val;

import org.jaust.Context;
import org.jaust.signal.LongSignal;

// Constant long signal; always returns the same value regardless of time.
public record LongVal(Context context, long value) implements LongSignal {
    public long longAt(long time) {
        if (time < 0) return 0;
        return value;
    }
}
