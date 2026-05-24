package org.jaust.filter;

import org.jaust.Context;
import org.jaust.signal.DoubleSignal;

// A cache for DoubleSignal that computes values sequentially from time 0, avoiding deep recursion.
class SequentialDoubleCache implements DoubleSignal {
    private final DoubleSignal source;
    private double[] cache;
    private int computed; // number of values computed (indices 0..computed-1 are valid)

    SequentialDoubleCache(DoubleSignal source) {
        this.source = source;
        this.cache = new double[256];
        this.computed = 0;
    }

    public Context context() { return source.context(); }

    public double doubleAt(long time) {
        if (time < 0) return source.doubleAt(time);
        int t = (int) time;
        ensureComputed(t);
        return cache[t];
    }

    private void ensureComputed(int t) {
        if (t < computed) return;
        if (t >= cache.length) {
            int newLen = Math.max(cache.length * 2, t + 1);
            double[] newCache = new double[newLen];
            System.arraycopy(cache, 0, newCache, 0, computed);
            cache = newCache;
        }
        for (int i = computed; i <= t; i++) {
            cache[i] = source.doubleAt(i);
        }
        computed = t + 1;
    }
}
