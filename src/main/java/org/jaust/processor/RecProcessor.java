package org.jaust.processor;

import org.jaust.Context;
import org.jaust.Processor;
import org.jaust.Signal;
import org.jaust.signal.DoubleSignal;
import org.jaust.signal.SignalArray;
import org.jaust.signal.array.DefaultArray;

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

    private static class RecDoubleSignal implements DoubleSignal {
        // to be set after p1OutSig is computed, so that doubleAt can recurse back to p1OutSig
        private DoubleSignal rec;
        
        public Context context() { return rec.context(); }
        
        public double doubleAt(long time) {
            if (time <= 0) return 0.0;
            return rec.doubleAt(time - 1);
        }
    }
    
    public SignalArray apply(SignalArray externalSignals) {
        int q = p1.outType().length;  // number of p1 outputs (= p2 inputs)
        int r = p2.outType().length;  // number of feedback signals (= p2 outputs)
        
        RecDoubleSignal[] p2InSig = new RecDoubleSignal[q];
        for (int i = 0; i < q; i++) {
            // TODO: handle non-DOUBLE signals (currently only supports DOUBLE)
            p2InSig[i] = new RecDoubleSignal();
        }

        SignalArray p2OutSig = p2.apply(DefaultArray.a(p2InSig));
        Signal[] p2Out = p2OutSig.toArray();
        Signal[] ext = externalSignals.toArray();
        Signal[] p1InArr = new Signal[r + ext.length];
        System.arraycopy(p2Out, 0, p1InArr, 0, r);
        System.arraycopy(ext,   0, p1InArr, r, ext.length);
        SignalArray p1OutSig = p1.apply(DefaultArray.a(p1InArr));
        
        // Close the loop: p2InSig[i].rec points to p1OutSig[i] so that p2InSig can recurse.
        for (int i = 0; i < q; i++) {
            // TODO: handle non-DOUBLE signals (currently only supports DOUBLE)
            (p2InSig[i]).rec = (DoubleSignal) p1OutSig.at(i);
        }
        return p1OutSig;
    }
}
