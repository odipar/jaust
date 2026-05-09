package org.jaust.processor.bin;

import org.jaust.Context;
import org.jaust.Signal;
import org.jaust.processor.DefaultProcessor;
import org.jaust.signal.LongSignal;
import org.jaust.signal.SignalArray;
import org.jaust.signal.array.DefaultArray;

import java.util.function.LongBinaryOperator;

public record LongBinProcessor(Context context,LongBinaryOperator o) implements DefaultProcessor {
    public Signal.Type[] inType() {
        return new Signal.Type[] {Signal.Type.DOUBLE, Signal.Type.DOUBLE};
    }
    public Signal.Type[] outType() {
        return new Signal.Type[] {Signal.Type.DOUBLE};
    }
    public SignalArray apply(SignalArray signal) {
        var _1 = signal.at(0);
        var _2 = signal.at(1);
        var sg = new LongSignal() {
            public Context context() {  return context; }
            public long longAt(long time) { return o.applyAsLong(_1.longAt(time), _2.longAt(time)); }
        };
        return DefaultArray.a(sg);
    }
}
