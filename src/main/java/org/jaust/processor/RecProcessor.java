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

// Faust recursive composition (~): feeds p1's outputs back to its first inputs via p2 with a one-sample delay.
public record RecProcessor(Processor p1, Processor p2) implements DefaultProcessor {

    public Context context() { return p1.context(); }

    public Signal.Type[] inType() {
        Signal.Type[] t = p1.inType();
        return Arrays.copyOfRange(t, p2.outType().length, t.length);
    }

    public Signal.Type[] outType() { return p1.outType(); }

    private static class RecBooleanSignal implements BooleanSignal {
        private BooleanSignal rec;

        public Context context() { return rec.context(); }

        public boolean boolAt(long time) {
            if (time <= 0) return false;
            return rec.boolAt(time - 1);
        }
    }

    private static class RecIntSignal implements IntSignal {
        private IntSignal rec;

        public Context context() { return rec.context(); }

        public int intAt(long time) {
            if (time <= 0) return 0;
            return rec.intAt(time - 1);
        }
    }

    private static class RecLongSignal implements LongSignal {
        private LongSignal rec;

        public Context context() { return rec.context(); }

        public long longAt(long time) {
            if (time <= 0) return 0L;
            return rec.longAt(time - 1);
        }
    }

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
        
        Signal[] recSignals = new Signal[q];
        for (int i = 0; i < q; i++) {
            recSignals[i] = switch (p1.outType()[i]) {
                case BOOL   -> new RecBooleanSignal();
                case INT    -> new RecIntSignal();
                case LONG   -> new RecLongSignal();
                case DOUBLE -> new RecDoubleSignal();
            };
        }

        SignalArray p2OutSig = p2.apply(DefaultArray.a(recSignals));
        SignalArray p1OutSig = p1.apply(p2OutSig.slice(0, r).append(externalSignals));
        
        // Close the loop: each recSignal points back to the corresponding p1OutSig signal.
        for (int i = 0; i < q; i++) {
            switch (recSignals[i]) {
                case RecBooleanSignal rbs -> rbs.rec = (BooleanSignal) p1OutSig.at(i);
                case RecIntSignal    ris  -> ris.rec = (IntSignal)     p1OutSig.at(i);
                case RecLongSignal   rls  -> rls.rec = (LongSignal)    p1OutSig.at(i);
                case RecDoubleSignal rds  -> rds.rec = (DoubleSignal)  p1OutSig.at(i);
                default -> throw new UnsupportedOperationException("Unsupported rec signal type: " + recSignals[i].getClass().getName());
            }
        }
        return p1OutSig;
    }
}
