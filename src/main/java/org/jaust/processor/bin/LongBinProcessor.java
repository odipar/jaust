package org.jaust.processor.bin;

import org.jaust.Context;
import org.jaust.Signal;
import org.jaust.processor.DefaultProcessor;
import org.jaust.signal.LongSignal;

import java.util.function.LongBinaryOperator;

public record LongBinProcessor(Context context,LongBinaryOperator o) implements DefaultProcessor {
    public Signal.Type[] inType() {
        return new Signal.Type[] {Signal.Type.DOUBLE, Signal.Type.DOUBLE};
    }
    public Signal.Type[] outType() {
        return new Signal.Type[] {Signal.Type.DOUBLE};
    }
    public Signal[] apply(Signal... signal) {
        var _1 = signal[0];
        var _2 = signal[1];
        var sg = new LongSignal() {
            public Context context() {  return context; }
            public long longAt(long time) { return o.applyAsLong(_1.longAt(time), _2.longAt(time)); }
        };
        return new Signal[] { sg };
    }
}
