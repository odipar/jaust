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

/**
 * Alternative RecProcessor using mutable arrays for iterative computation.
 * <p>
 * Instead of lazy recursive signal delegation (which leads to O(t) stack depth
 * for each sample query), this implementation eagerly computes and caches all
 * feedback values in growable arrays. Queries are O(1) amortized after the
 * initial forward pass.
 * <p>
 * Key efficiency improvements over {@link RecProcessor}:
 * <ul>
 *   <li>No deep recursion – iterative forward computation avoids stack overflow</li>
 *   <li>O(1) amortized per sample via cached mutable arrays</li>
 *   <li>Better cache locality – sequential array access vs pointer chasing</li>
 *   <li>Inline mutation – arrays are mutated in place as new samples are computed</li>
 * </ul>
 */
public record MutableRecProcessor(Processor p1, Processor p2) implements DefaultProcessor {

    public Context context() { return p1.context(); }

    public Signal.Type[] inType() {
        Signal.Type[] t = p1.inType();
        return Arrays.copyOfRange(t, p2.outType().length, t.length);
    }

    public Signal.Type[] outType() { return p1.outType(); }

    public SignalArray apply(SignalArray externalSignals) {
        int q = p1.outType().length;  // number of p1 outputs (fed to p2)
        int r = p2.outType().length;  // number of feedback signals (p2 outputs fed back to p1)

        Signal.Type[] outTypes = p1.outType();

        // Create mutable cached signals for p1 outputs (the feedback path)
        MutableSignal[] mutables = new MutableSignal[q];
        Signal[] mutSignals = new Signal[q];
        for (int i = 0; i < q; i++) {
            mutables[i] = new MutableSignal(outTypes[i]);
            mutSignals[i] = mutables[i].asSignal(p1.context());
        }

        // Wire p2: takes all q p1-outputs as input, produces r feedback signals
        SignalArray p2OutSig = p2.apply(DefaultArray.a(mutSignals));
        // Wire p1: takes r feedback signals + external signals as input
        SignalArray p1OutSig = p1.apply(p2OutSig.slice(0, r).append(externalSignals));

        // Create output signals that iteratively compute and cache values
        Signal[] outputSignals = new Signal[q];
        // Shared computation state across all output channels
        IterativeState state = new IterativeState(q, mutables, p1OutSig);

        for (int i = 0; i < q; i++) {
            final int idx = i;
            outputSignals[i] = switch (outTypes[i]) {
                case BOOL -> new BooleanSignal() {
                    public Context context() { return p1.context(); }
                    public boolean boolAt(long time) {
                        state.computeUpTo(time);
                        return mutables[idx].directBoolAt(time);
                    }
                };
                case INT -> new IntSignal() {
                    public Context context() { return p1.context(); }
                    public int intAt(long time) {
                        state.computeUpTo(time);
                        return mutables[idx].directIntAt(time);
                    }
                };
                case LONG -> new LongSignal() {
                    public Context context() { return p1.context(); }
                    public long longAt(long time) {
                        state.computeUpTo(time);
                        return mutables[idx].directLongAt(time);
                    }
                };
                case DOUBLE -> new DoubleSignal() {
                    public Context context() { return p1.context(); }
                    public double doubleAt(long time) {
                        state.computeUpTo(time);
                        return mutables[idx].directDoubleAt(time);
                    }
                };
            };
        }

        return DefaultArray.a(outputSignals);
    }

    /**
     * Shared iterative computation state. Coordinates forward computation
     * across all output channels so that each time step is computed exactly once.
     */
    private static class IterativeState {
        private long computedUpTo = -1;
        private final int q;
        private final MutableSignal[] mutables;
        private final SignalArray p1OutSig;

        IterativeState(int q, MutableSignal[] mutables, SignalArray p1OutSig) {
            this.q = q;
            this.mutables = mutables;
            this.p1OutSig = p1OutSig;
        }

        void computeUpTo(long time) {
            if (time <= computedUpTo) return;
            // Iteratively compute from computedUpTo+1 to time
            long start = computedUpTo + 1;
            for (long t = start; t <= time; t++) {
                for (int i = 0; i < q; i++) {
                    mutables[i].store(t, p1OutSig.at(i));
                }
            }
            computedUpTo = time;
        }
    }

    /**
     * Mutable signal backed by a growable array. Stores computed sample values
     * inline and provides O(1) access to previously computed samples.
     * For the feedback path, values at time <= 0 return the type's zero value,
     * and the signal delegates to the stored value shifted by one sample (time - 1).
     */
    private static class MutableSignal {
        private final Signal.Type type;
        private double[] doubleData;
        private int[] intData;
        private long[] longData;
        private boolean[] boolData;
        private int capacity;
        private long stored = -1; // highest time index stored

        private static final int INITIAL_CAPACITY = 64;

        MutableSignal(Signal.Type type) {
            this.type = type;
            this.capacity = INITIAL_CAPACITY;
            switch (type) {
                case DOUBLE -> doubleData = new double[capacity];
                case INT    -> intData = new int[capacity];
                case LONG   -> longData = new long[capacity];
                case BOOL   -> boolData = new boolean[capacity];
            }
        }

        /** Store the value at the given time from the source signal. */
        void store(long time, Signal source) {
            int idx = (int) time;
            ensureCapacity(idx + 1);
            switch (type) {
                case DOUBLE -> doubleData[idx] = source.doubleAt(time);
                case INT    -> intData[idx] = source.intAt(time);
                case LONG   -> longData[idx] = source.longAt(time);
                case BOOL   -> boolData[idx] = source.boolAt(time);
            }
            if (time > stored) stored = time;
        }

        /** Read cached double at time (for feedback: shifted by -1, zero at t<=0). */
        boolean boolAt(long time) {
            // Feedback semantics: one-sample delay
            long feedbackTime = time - 1;
            if (feedbackTime < 0) return false;
            return boolData[(int) feedbackTime];
        }

        int intAt(long time) {
            long feedbackTime = time - 1;
            if (feedbackTime < 0) return 0;
            return intData[(int) feedbackTime];
        }

        long longAt(long time) {
            long feedbackTime = time - 1;
            if (feedbackTime < 0) return 0L;
            return longData[(int) feedbackTime];
        }

        double doubleAt(long time) {
            long feedbackTime = time - 1;
            if (feedbackTime < 0) return 0.0;
            return doubleData[(int) feedbackTime];
        }

        /** Direct access (no time shift) for output signals. */
        boolean directBoolAt(long time) {
            if (time < 0) return false;
            return boolData[(int) time];
        }

        int directIntAt(long time) {
            if (time < 0) return 0;
            return intData[(int) time];
        }

        long directLongAt(long time) {
            if (time < 0) return 0L;
            return longData[(int) time];
        }

        double directDoubleAt(long time) {
            if (time < 0) return 0.0;
            return doubleData[(int) time];
        }

        /** Create a Signal view for use in the signal graph (feedback path). */
        Signal asSignal(Context ctx) {
            return switch (type) {
                case BOOL -> new BooleanSignal() {
                    public Context context() { return ctx; }
                    public boolean boolAt(long time) { return MutableSignal.this.boolAt(time); }
                };
                case INT -> new IntSignal() {
                    public Context context() { return ctx; }
                    public int intAt(long time) { return MutableSignal.this.intAt(time); }
                };
                case LONG -> new LongSignal() {
                    public Context context() { return ctx; }
                    public long longAt(long time) { return MutableSignal.this.longAt(time); }
                };
                case DOUBLE -> new DoubleSignal() {
                    public Context context() { return ctx; }
                    public double doubleAt(long time) { return MutableSignal.this.doubleAt(time); }
                };
            };
        }

        private void ensureCapacity(int minCapacity) {
            if (minCapacity <= capacity) return;
            int newCapacity = Math.max(capacity * 2, minCapacity);
            switch (type) {
                case DOUBLE -> doubleData = Arrays.copyOf(doubleData, newCapacity);
                case INT    -> intData = Arrays.copyOf(intData, newCapacity);
                case LONG   -> longData = Arrays.copyOf(longData, newCapacity);
                case BOOL   -> boolData = Arrays.copyOf(boolData, newCapacity);
            }
            capacity = newCapacity;
        }
    }
}
