package org.jaust.signal.cache;

import org.jaust.Context;
import org.jaust.signal.LongSignal;

public class LongCache implements LongSignal {
    private final LongSignal s;

    private long cacheTime = Long.MIN_VALUE;
    private long cache;

    public LongCache(LongSignal s) {
        this.s = s;
    }

    public Context context() {
        return s.context();
    }

    public long longAt(long time) {
        if (cacheTime == time) {
            return cache;
        }
        cache = s.longAt(time);
        cacheTime = time;
        return cache;
    }
}
