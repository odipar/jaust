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
        Signal.Type[] t = p1.inType();
        return Arrays.copyOfRange(t, p2.outType().length, t.length);
    }

    public Signal.Type[] outType() { return p1.outType(); }

    public Signal[] apply(Signal... externalSignals) {
        int q = p1.outType().length;  // number of p1 outputs (= p2 inputs)
        int r = p2.outType().length;  // number of feedback signals (= p2 outputs)
        Context ctx = context();

        double[] feedback = new double[q];  // one-sample-delayed p1 outputs fed into p2
        double[] current  = new double[q];  // p1 outputs at the last computed time
        long[]   lastTime = {-1};

        Signal[] p2InSig = new Signal[q];
        for (int i = 0; i < q; i++) {
            final int fi = i;
            p2InSig[fi] = new DoubleSignal() {
                public Context context() { return ctx; }
                public double doubleAt(long time) { return feedback[fi]; }
            };
        }

        Signal[] p2OutSig = p2.apply(p2InSig);
        Signal[] p1InSig  = new Signal[r + externalSignals.length];
        System.arraycopy(p2OutSig,        0, p1InSig, 0, r);
        System.arraycopy(externalSignals, 0, p1InSig, r, externalSignals.length);
        Signal[] p1OutSig = p1.apply(p1InSig);

        Signal[] result = new Signal[q];
        for (int i = 0; i < q; i++) {
            final int fi = i;
            result[fi] = new DoubleSignal() {
                public Context context() { return ctx; }
                public double doubleAt(long time) {
                    if (time > lastTime[0]) {
                        for (long t = lastTime[0] + 1; t <= time; t++) {
                            for (int j = 0; j < q; j++) current[j] = p1OutSig[j].doubleAt(t);
                            System.arraycopy(current, 0, feedback, 0, q);
                            lastTime[0] = t;
                        }
                    }
                    return current[fi];
                }
            };
        }
        return result;
    }
}
