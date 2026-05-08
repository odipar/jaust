package org.jaust.processor;

import org.jaust.Context;
import org.jaust.Processor;
import org.jaust.Signal;
import org.jaust.signal.DoubleSignal;

import java.util.Arrays;

/**
 * Implements the Faust recursive composition operator ({@code ~}).
 * <p>
 * {@code p1 ~ p2}: p1 has {@code p} inputs and {@code q} outputs; p2 has {@code q} inputs and
 * {@code r} outputs ({@code r <= p}). The combined block has {@code p - r} inputs and {@code q}
 * outputs. p2's outputs are fed back to p1's first {@code r} inputs with a one-sample delay.
 * <p>
 * Time is assumed to be queried sequentially (t = 0, 1, 2, …). Out-of-order queries will
 * return the last cached value.
 */
public record RecProcessor(Processor p1, Processor p2) implements DefaultProcessor {

    public Context context() { return p1.context(); }

    public Signal.Type[] inType() {
        int r = p2.outType().length;
        return Arrays.copyOfRange(p1.inType(), r, p1.inType().length);
    }

    public Signal.Type[] outType() { return p1.outType(); }

    public Signal[] apply(Signal... externalSignals) {
        int q = p1.outType().length;  // number of p1 outputs (= p2 inputs)
        int r = p2.outType().length;  // number of feedback signals (= p2 outputs)

        // State: previous p1 outputs (used as p2's input), initialised to 0
        double[] prevP1Out = new double[q];
        double[] curOut = new double[q];
        long[] lastTime = {-1};

        // Feedback signals: p2's inputs are the previous p1 outputs
        Signal[] p2InSig = new Signal[q];
        for (int i = 0; i < q; i++) {
            final int fi = i;
            p2InSig[i] = new DoubleSignal() {
                public Context context() { return RecProcessor.this.context(); }
                // Always returns the stored previous output (time parameter is not used here;
                // correctness relies on advanceTo keeping prevP1Out up to date)
                public double doubleAt(long time) { return prevP1Out[fi]; }
            };
        }

        // Build the lazy signal chain once; state mutations drive the computation forward
        Signal[] p2OutSig = p2.apply(p2InSig);

        Signal[] p1InSig = new Signal[r + externalSignals.length];
        System.arraycopy(p2OutSig, 0, p1InSig, 0, r);
        System.arraycopy(externalSignals, 0, p1InSig, r, externalSignals.length);

        Signal[] p1OutSig = p1.apply(p1InSig);

        // Wrap results in stateful signals that advance the computation step by step
        Signal[] result = new Signal[q];
        for (int i = 0; i < q; i++) {
            final int fi = i;
            result[fi] = new DoubleSignal() {
                public Context context() { return RecProcessor.this.context(); }
                public double doubleAt(long time) {
                    advanceTo(time, p1OutSig, prevP1Out, curOut, lastTime);
                    return curOut[fi];
                }
            };
        }
        return result;
    }

    /**
     * Advances the recursive computation to {@code target} time, stepping forward from the last
     * computed time. {@code prevP1Out} must hold the p1 output at {@code lastTime[0]} before each
     * call, and is updated to the output at {@code target} afterwards.
     */
    private static void advanceTo(long target, Signal[] p1OutSig,
                                   double[] prevP1Out, double[] curOut, long[] lastTime) {
        if (target <= lastTime[0]) return;

        for (long t = lastTime[0] + 1; t <= target; t++) {
            // prevP1Out holds p1 output at t-1, so p2InSig correctly represents the
            // one-sample-delayed feedback when p1OutSig[i].doubleAt(t) is evaluated here
            for (int i = 0; i < p1OutSig.length; i++) {
                curOut[i] = p1OutSig[i].doubleAt(t);
            }
            System.arraycopy(curOut, 0, prevP1Out, 0, prevP1Out.length);
            lastTime[0] = t;
        }
    }
}
