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
 * Implemented using actual recursion with no hidden state. Signals may be queried at any time
 * in any order; each query is computed purely from the input signals and prior recursive values.
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

        // resultRef is populated after p1OutSig is computed; p2InSig captures it by reference
        // so that the circular dependency is resolved lazily at query time.
        Signal[] resultRef = new Signal[q];

        // p2InSig[i](t) = resultRef[i](t-1), i.e. one-sample-delayed output of p1.
        // At t=0 the feedback is 0 (no previous output exists).
        Signal[] p2InSig = new Signal[q];
        for (int i = 0; i < q; i++) {
            final int fi = i;
            p2InSig[fi] = new DoubleSignal() {
                public Context context() { return ctx; }
                public double doubleAt(long time) {
                    return time <= 0 ? 0.0 : resultRef[fi].doubleAt(time - 1);
                }
            };
        }

        Signal[] p2OutSig = p2.apply(p2InSig);
        Signal[] p1InSig  = new Signal[r + externalSignals.length];
        System.arraycopy(p2OutSig,        0, p1InSig, 0, r);
        System.arraycopy(externalSignals, 0, p1InSig, r, externalSignals.length);
        Signal[] p1OutSig = p1.apply(p1InSig);

        // Close the loop: resultRef[i] points to p1OutSig[i] so that p2InSig can recurse.
        System.arraycopy(p1OutSig, 0, resultRef, 0, q);

        return p1OutSig;
    }
}
