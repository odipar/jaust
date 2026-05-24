package org.jaust.filter;

import org.jaust.Context;
import org.jaust.Signal;
import org.jaust.processor.DefaultProcessor;
import org.jaust.signal.DoubleSignal;
import org.jaust.signal.SignalArray;
import org.jaust.signal.array.DefaultArray;
// Base class for double-precision filters with two inputs (signal, cutoff frequency) and one output.
public abstract class Filter implements DefaultProcessor {
    protected final Context context;

    protected Filter(Context context) {
        this.context = context;
    }

    public Context context() { return context; }

    public Signal.Type[] inType() {
        return new Signal.Type[]{ Signal.Type.DOUBLE, Signal.Type.DOUBLE };
    }

    public Signal.Type[] outType() {
        return new Signal.Type[]{ Signal.Type.DOUBLE };
    }

    public SignalArray apply(SignalArray signal) {
        DoubleSignal input = (DoubleSignal) signal.at(0);
        DoubleSignal cutoff = (DoubleSignal) signal.at(1);
        DoubleSignal output = computeOutput(input, cutoff);
        return DefaultArray.a(output);
    }

    protected abstract DoubleSignal computeOutput(DoubleSignal input, DoubleSignal cutoff);
}
