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

    // FilterSignal is a helper class that handles cutoff changes and delegates to
    // doubleAtC() for actual sample computation.
    abstract static class FilterSignal implements DoubleSignal {
        protected DoubleSignal input;
        protected DoubleSignal cutoff;
        protected Context context;
        protected double fc = Double.NaN;
        
        public Context context() {
            return context;
        }
        
        // calcCutoff() is called whenever the cutoff frequency changes,
        // allowing subclasses to precompute coefficients or state as needed
        abstract void calcCutoff();
        
        // doubleAtC() is called to compute the output sample at a given time,
        // assuming the cutoff frequency is up-to-date.
        abstract double doubleAtC(long time);
        
        public FilterSignal(DoubleSignal input, DoubleSignal cutoff, Context context) {
            this.cutoff = cutoff;
            this.input = input;
            this.context = context;
        }
        
        public double doubleAt(long time) {
            double c = cutoff.doubleAt(time);
            if (c <= 0) return input.doubleAt(time); // No filtering if cutoff is non-positive.
            if (c != fc) {
                fc = c;
                calcCutoff();
            }
            return doubleAtC(time);
        }
        
    }
    protected abstract DoubleSignal computeOutput(DoubleSignal input, DoubleSignal cutoff);
}
