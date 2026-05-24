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
 * Alternative RecProcessor using single-value mutable state for feedback.
 * <p>
 * Each feedback channel holds only one value: the most recently computed output (t-1).
 * Computation is iterative – stepping forward from t=0 to the requested time,
 * updating the single stored value at each step. This avoids O(t) stack depth
 * and uses minimal memory (one value per channel regardless of time).
 * <p>
 * Key properties:
 * <ul>
 *   <li>No recursion – iterative forward computation avoids stack overflow</li>
 *   <li>Minimal memory – only 1 value per feedback channel (the t-1 state)</li>
 *   <li>Inline mutation – the stored value is updated in place at each step</li>
 *   <li>Sequential access is O(1) per step; random access recomputes from 0</li>
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

        // Each feedback channel holds only the single previous value (t-1)
        FeedbackCell[] cells = new FeedbackCell[q];
        Signal[] feedbackSignals = new Signal[q];
        for (int i = 0; i < q; i++) {
            cells[i] = new FeedbackCell(outTypes[i]);
            feedbackSignals[i] = cells[i].asSignal(p1.context());
        }

        // Wire p2: takes all q p1-outputs as input, produces r feedback signals
        SignalArray p2OutSig = p2.apply(DefaultArray.a(feedbackSignals));
        // Wire p1: takes r feedback signals + external signals as input
        SignalArray p1OutSig = p1.apply(p2OutSig.slice(0, r).append(externalSignals));

        // Shared state for iterative stepping
        IterativeState state = new IterativeState(q, cells, p1OutSig);

        // Create output signals that step forward to the requested time
        Signal[] outputSignals = new Signal[q];
        for (int i = 0; i < q; i++) {
            final int idx = i;
            outputSignals[i] = switch (outTypes[i]) {
                case BOOL -> new BooleanSignal() {
                    public Context context() { return p1.context(); }
                    public boolean boolAt(long time) { return state.boolAt(idx, time); }
                };
                case INT -> new IntSignal() {
                    public Context context() { return p1.context(); }
                    public int intAt(long time) { return state.intAt(idx, time); }
                };
                case LONG -> new LongSignal() {
                    public Context context() { return p1.context(); }
                    public long longAt(long time) { return state.longAt(idx, time); }
                };
                case DOUBLE -> new DoubleSignal() {
                    public Context context() { return p1.context(); }
                    public double doubleAt(long time) { return state.doubleAt(idx, time); }
                };
            };
        }

        return DefaultArray.a(outputSignals);
    }

    /**
     * Shared iterative computation state. Steps forward one sample at a time,
     * updating each cell's single stored value. Tracks the last computed time
     * so sequential forward access is O(1) per step.
     */
    private static class IterativeState {
        private long computedUpTo = -1;
        private final int q;
        private final FeedbackCell[] cells;
        private final SignalArray p1OutSig;
        // Snapshot of output values at computedUpTo (for output reads)
        private double[] lastDouble;
        private int[] lastInt;
        private long[] lastLong;
        private boolean[] lastBool;

        IterativeState(int q, FeedbackCell[] cells, SignalArray p1OutSig) {
            this.q = q;
            this.cells = cells;
            this.p1OutSig = p1OutSig;
            this.lastDouble = new double[q];
            this.lastInt = new int[q];
            this.lastLong = new long[q];
            this.lastBool = new boolean[q];
        }

        private void stepTo(long time) {
            if (time < 0) return;
            // If requested time is behind current position, reset and recompute
            if (time < computedUpTo) {
                computedUpTo = -1;
                for (int i = 0; i < q; i++) {
                    cells[i].reset();
                }
            }
            if (time <= computedUpTo) return;
            long start = computedUpTo + 1;
            for (long t = start; t <= time; t++) {
                for (int i = 0; i < q; i++) {
                    cells[i].advance(t, p1OutSig.at(i));
                    // Capture the current output value
                    switch (cells[i].type) {
                        case DOUBLE -> lastDouble[i] = cells[i].doubleVal;
                        case INT    -> lastInt[i] = cells[i].intVal;
                        case LONG   -> lastLong[i] = cells[i].longVal;
                        case BOOL   -> lastBool[i] = cells[i].boolVal;
                    }
                }
            }
            computedUpTo = time;
        }

        boolean boolAt(int idx, long time) {
            if (time < 0) return false;
            stepTo(time);
            return lastBool[idx];
        }

        int intAt(int idx, long time) {
            if (time < 0) return 0;
            stepTo(time);
            return lastInt[idx];
        }

        long longAt(int idx, long time) {
            if (time < 0) return 0L;
            stepTo(time);
            return lastLong[idx];
        }

        double doubleAt(int idx, long time) {
            if (time < 0) return 0.0;
            stepTo(time);
            return lastDouble[idx];
        }
    }

    /**
     * Holds a single feedback value (the t-1 state) for one channel.
     * The feedback signal reads this value, providing the one-sample delay.
     * After each step, the value is updated to the latest p1 output.
     */
    private static class FeedbackCell {
        final Signal.Type type;
        double doubleVal;
        int intVal;
        long longVal;
        boolean boolVal;

        FeedbackCell(Signal.Type type) {
            this.type = type;
        }

        void reset() {
            doubleVal = 0.0;
            intVal = 0;
            longVal = 0L;
            boolVal = false;
        }

        /** Advance: read the current p1 output at time t, store it as the new feedback value. */
        void advance(long time, Signal source) {
            switch (type) {
                case DOUBLE -> doubleVal = source.doubleAt(time);
                case INT    -> intVal = source.intAt(time);
                case LONG   -> longVal = source.longAt(time);
                case BOOL   -> boolVal = source.boolAt(time);
            }
        }

        /** Create a feedback Signal that reads the single stored value (one-sample delay). */
        Signal asSignal(Context ctx) {
            return switch (type) {
                case BOOL -> new BooleanSignal() {
                    public Context context() { return ctx; }
                    public boolean boolAt(long time) {
                        if (time <= 0) return false;
                        return boolVal;
                    }
                };
                case INT -> new IntSignal() {
                    public Context context() { return ctx; }
                    public int intAt(long time) {
                        if (time <= 0) return 0;
                        return intVal;
                    }
                };
                case LONG -> new LongSignal() {
                    public Context context() { return ctx; }
                    public long longAt(long time) {
                        if (time <= 0) return 0L;
                        return longVal;
                    }
                };
                case DOUBLE -> new DoubleSignal() {
                    public Context context() { return ctx; }
                    public double doubleAt(long time) {
                        if (time <= 0) return 0.0;
                        return doubleVal;
                    }
                };
            };
        }
    }
}
