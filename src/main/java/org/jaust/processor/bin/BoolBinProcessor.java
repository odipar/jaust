package org.jaust.processor.bin;

import org.jaust.BooleanBinaryOperator;
import org.jaust.Context;
import org.jaust.Signal;
import org.jaust.processor.DefaultProcessor;
import org.jaust.signal.BooleanSignal;
import org.jaust.signal.SignalArray;
import org.jaust.signal.array.DefaultArray;

public record BoolBinProcessor(Context context, BooleanBinaryOperator o) implements DefaultProcessor {
    public Signal.Type[] inType() {
        return new Signal.Type[] {Signal.Type.BOOL, Signal.Type.BOOL};
    }
    public Signal.Type[] outType() {
        return new Signal.Type[] {Signal.Type.BOOL};
    }
    public SignalArray apply(SignalArray signal) {
        var _1 = signal.at(0);
        var _2 = signal.at(1);
        var sg = new BooleanSignal() {
            public Context context() { return context; }
            public boolean boolAt(long time) { return o.apply(_1.boolAt(time), _2.boolAt(time)); }
        };
        return DefaultArray.a(sg);
    }
}
