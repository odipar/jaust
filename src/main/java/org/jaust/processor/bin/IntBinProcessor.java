package org.jaust.processor.bin;

import org.jaust.Context;
import org.jaust.Signal;
import org.jaust.processor.DefaultProcessor;
import org.jaust.signal.IntSignal;
import org.jaust.signal.SignalArray;
import org.jaust.signal.array.DefaultArray;
import java.util.function.IntBinaryOperator;

public record IntBinProcessor(Context context, IntBinaryOperator o) implements DefaultProcessor {
    public Signal.Type[] inType() {
        return new Signal.Type[] {Signal.Type.INT, Signal.Type.INT};
    }
    public Signal.Type[] outType() {
        return new Signal.Type[] {Signal.Type.INT};
    }
    public SignalArray apply(SignalArray signal) {
        var _1 = signal.at(0);
        var _2 = signal.at(1);
        var sg = new IntSignal() {
            public Context context() {  return context; }
            public int intAt(long time) { return o.applyAsInt(_1.intAt(time), _2.intAt(time)); }
        };
        return DefaultArray.a(sg);
    }
}

