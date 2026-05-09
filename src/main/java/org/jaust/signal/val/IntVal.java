package org.jaust.signal.val;

import org.jaust.Context;
import org.jaust.signal.IntSignal;

// Constant int signal; always returns the same value regardless of time.
public record IntVal(Context context, int value) implements IntSignal {
    public int intAt(long time) {
        if (time < 0) return 0;
        return value;
    }
}