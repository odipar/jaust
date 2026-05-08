package org.jaust.processor.bin;

import org.jaust.Context;
import org.jaust.Signal;
import org.jaust.processor.DefaultProcessor;
import org.jaust.signal.IntSignal;
import java.util.function.IntBinaryOperator;

public record IntBinProcessor(Context context, IntBinaryOperator o) implements DefaultProcessor {
    public Signal.Type[] inType() {
        return new Signal.Type[] {Signal.Type.DOUBLE, Signal.Type.DOUBLE};
    }
    public Signal.Type[] outType() {
        return new Signal.Type[] {Signal.Type.DOUBLE};
    }
    public Signal[] apply(Signal... signal) {
        var _1 = signal[0];
        var _2 = signal[1];
        var sg = new IntSignal() {
            public Context context() {  return context; }
            public int intAt(long time) { return o.applyAsInt(_1.intAt(time), _2.intAt(time)); }
        };
        return new Signal[] { sg };
    }
}

