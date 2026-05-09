package org.jaust.signal.gen;

import org.jaust.Context;
import org.jaust.signal.IntSignal;
import java.util.function.LongToIntFunction;

// Generates an int signal by applying a time-to-int function; returns 0 for negative time.
public record IntGen(Context context, LongToIntFunction f) implements IntSignal {
    public int intAt(long time) {
        if (time < 0) return 0;
        return f.applyAsInt(time);
    }
}
