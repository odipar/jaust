package org.jaust.signal.cache;

import org.jaust.Context;
import org.jaust.signal.IntSignal;

// Caches the last queried int value to avoid recomputing the same sample time twice.
public class IntCache implements IntSignal {
    private final IntSignal s;

    private long cacheTime = Long.MIN_VALUE;
    private int cache;

    public IntCache(IntSignal s) {
        this.s = s;
    }

    public Context context() {
        return s.context();
    }

    public int intAt(long time) {
        if (cacheTime == time) {
            return cache;
        }
        cache = s.intAt(time);
        cacheTime = time;
        return cache;
    }
}
