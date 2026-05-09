package org.jaust.signal.cache;

import org.jaust.Context;
import org.jaust.signal.BooleanSignal;

// Caches the last queried boolean value to avoid recomputing the same sample time twice.
public class BooleanCache implements BooleanSignal {
    private final BooleanSignal s;

    private long cacheTime = Long.MIN_VALUE;
    private boolean cache;

    public BooleanCache(BooleanSignal s) {
        this.s = s;
    }

    public Context context() {
        return s.context();
    }

    public boolean boolAt(long time) {
        if (cacheTime == time) {
            return cache;
        }
        cache = s.boolAt(time);
        cacheTime = time;
        return cache;
    }
}
