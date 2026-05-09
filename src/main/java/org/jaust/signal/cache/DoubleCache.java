package org.jaust.signal.cache;

import org.jaust.Context;
import org.jaust.signal.DoubleSignal;

// Caches the last queried double value to avoid recomputing the same sample time twice.
public class DoubleCache implements DoubleSignal {
    private final DoubleSignal s;
    
    private long cacheTime = Long.MIN_VALUE;
    private double cache;
    
    public DoubleCache(DoubleSignal s) {
        this.s = s;
    }
    public Context context() {
        return s.context();
    }
    public double doubleAt(long time) {
        if (cacheTime == time) {
            return cache;
        }
        cache = s.doubleAt(time);
        cacheTime = time;
        return cache;
    }
}
