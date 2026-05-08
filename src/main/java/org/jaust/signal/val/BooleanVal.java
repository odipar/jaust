package org.jaust.signal.val;

import org.jaust.Context;
import org.jaust.signal.BooleanSignal;

public record BooleanVal(Context context, boolean value) implements BooleanSignal {
    public boolean boolAt(long time) {
        if (time < 0) return false;
        return value;
    }
}
