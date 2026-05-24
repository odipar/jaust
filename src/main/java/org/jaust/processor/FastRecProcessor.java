package org.jaust.processor;

import org.jaust.Context;
import org.jaust.Processor;
import org.jaust.Signal;
import org.jaust.signal.BooleanSignal;
import org.jaust.signal.DoubleSignal;
import org.jaust.signal.IntSignal;
import org.jaust.signal.LongSignal;
import org.jaust.signal.SignalArray;
import org.jaust.signal.array.DefaultArray;

import java.util.Arrays;

// Optimized version of RecProcessor: uses iterative forward computation instead of deep recursion.
// This avoids stack overflow and is dramatically faster for large time values.
public record FastRecProcessor(Processor p1, Processor p2) implements DefaultProcessor {

    public Context context() { return p1.context(); }

    public Signal.Type[] inType() {
        Signal.Type[] t = p1.inType();
        return Arrays.copyOfRange(t, p2.outType().length, t.length);
    }

    public Signal.Type[] outType() { return p1.outType(); }

    // Iterative signal that computes values forward from time 0 to t, caching the last computed value.
    private static class FastLongSignal implements LongSignal {
        private LongSignal source;
        private long cachedTime = -1;
        private long cachedValue = 0L;

        public Context context() { return source.context(); }

        public long longAt(long time) {
            if (time <= 0) return 0L;
            if (time == cachedTime) return cachedValue;
            // If requesting the next sequential time, just compute one step
            if (time == cachedTime + 1) {
                cachedValue = source.longAt(time - 1);
                cachedTime = time;
                return cachedValue;
            }
            // Otherwise iterate forward from last cached position
            long startTime = cachedTime + 1;
            if (startTime <= 0) startTime = 1;
            for (long t = startTime; t <= time; t++) {
                cachedValue = source.longAt(t - 1);
                cachedTime = t;
            }
            return cachedValue;
        }
    }

    private static class FastIntSignal implements IntSignal {
        private IntSignal source;
        private long cachedTime = -1;
        private int cachedValue = 0;

        public Context context() { return source.context(); }

        public int intAt(long time) {
            if (time <= 0) return 0;
            if (time == cachedTime) return cachedValue;
            if (time == cachedTime + 1) {
                cachedValue = source.intAt(time - 1);
                cachedTime = time;
                return cachedValue;
            }
            long startTime = cachedTime + 1;
            if (startTime <= 0) startTime = 1;
            for (long t = startTime; t <= time; t++) {
                cachedValue = source.intAt(t - 1);
                cachedTime = t;
            }
            return cachedValue;
        }
    }

    private static class FastBooleanSignal implements BooleanSignal {
        private BooleanSignal source;
        private long cachedTime = -1;
        private boolean cachedValue = false;

        public Context context() { return source.context(); }

        public boolean boolAt(long time) {
            if (time <= 0) return false;
            if (time == cachedTime) return cachedValue;
            if (time == cachedTime + 1) {
                cachedValue = source.boolAt(time - 1);
                cachedTime = time;
                return cachedValue;
            }
            long startTime = cachedTime + 1;
            if (startTime <= 0) startTime = 1;
            for (long t = startTime; t <= time; t++) {
                cachedValue = source.boolAt(t - 1);
                cachedTime = t;
            }
            return cachedValue;
        }
    }

    private static class FastDoubleSignal implements DoubleSignal {
        private DoubleSignal source;
        private long cachedTime = -1;
        private double cachedValue = 0.0;

        public Context context() { return source.context(); }

        public double doubleAt(long time) {
            if (time <= 0) return 0.0;
            if (time == cachedTime) return cachedValue;
            if (time == cachedTime + 1) {
                cachedValue = source.doubleAt(time - 1);
                cachedTime = time;
                return cachedValue;
            }
            long startTime = cachedTime + 1;
            if (startTime <= 0) startTime = 1;
            for (long t = startTime; t <= time; t++) {
                cachedValue = source.doubleAt(t - 1);
                cachedTime = t;
            }
            return cachedValue;
        }
    }

    public SignalArray apply(SignalArray externalSignals) {
        int q = p1.outType().length;
        int r = p2.outType().length;

        Signal[] recSignals = new Signal[q];
        for (int i = 0; i < q; i++) {
            recSignals[i] = switch (p1.outType()[i]) {
                case BOOL   -> new FastBooleanSignal();
                case INT    -> new FastIntSignal();
                case LONG   -> new FastLongSignal();
                case DOUBLE -> new FastDoubleSignal();
            };
        }

        SignalArray p2OutSig = p2.apply(DefaultArray.a(recSignals));
        SignalArray p1OutSig = p1.apply(p2OutSig.slice(0, r).append(externalSignals));

        // Close the loop: each fast signal points back to the corresponding p1 output signal.
        for (int i = 0; i < q; i++) {
            switch (recSignals[i]) {
                case FastBooleanSignal fbs -> fbs.source = (BooleanSignal) p1OutSig.at(i);
                case FastIntSignal    fis  -> fis.source = (IntSignal)     p1OutSig.at(i);
                case FastLongSignal   fls  -> fls.source = (LongSignal)    p1OutSig.at(i);
                case FastDoubleSignal fds  -> fds.source = (DoubleSignal)  p1OutSig.at(i);
                default -> throw new UnsupportedOperationException("Unsupported signal type: " + recSignals[i].getClass().getName());
            }
        }
        return p1OutSig;
    }
}
