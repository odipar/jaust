package org.jaust.processor.bin;

import org.jaust.Context;
import org.jaust.Signal;
import org.jaust.processor.DefaultProcessor;
import org.jaust.signal.DoubleSignal;
import org.jaust.signal.SignalArray;
import org.jaust.signal.array.DefaultArray;
import java.util.function.DoubleBinaryOperator;

public record DoubleBinProcessor(Context context, DoubleBinaryOperator o) implements DefaultProcessor {
    public Signal.Type[] inType() {
        return new Signal.Type[] {Signal.Type.DOUBLE, Signal.Type.DOUBLE};
    }
    public Signal.Type[] outType() {
        return new Signal.Type[] {Signal.Type.DOUBLE};
    }
    public SignalArray apply(SignalArray signal) {
        var _1 = signal.at(0);
        var _2 = signal.at(1);
        
        var sg = new DoubleSignal() {
            public Context context() {  return context; }
            public double doubleAt(long time) { return o.applyAsDouble(_1.doubleAt(time), _2.doubleAt(time)); }
        };
        return DefaultArray.a(sg);
    }
}
