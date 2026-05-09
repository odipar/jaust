package org.jaust.signal.cache;

import org.jaust.Context;
import org.jaust.signal.DoubleSignal;

// Purely functional cache for a double signal.
// Caches the last value and time, and returns the cached value if the time is the same as the last time.
// Otherwise, it updates the cache with the new value and time.
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
