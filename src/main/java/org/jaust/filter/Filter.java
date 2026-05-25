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

    // Biquad is a second-order IIR section (Direct Form I) used by Butterworth filters.
    // Subclasses implement calcB() to set the feedforward (b) coefficients for their filter type.
    abstract static class Biquad extends FilterSignal {
        protected DoubleSignal holder;

        double poleAngle;
        double a1;
        double a2;
        double b0;
        double b1;
        double b2;

        public Biquad(DoubleSignal input, DoubleSignal cutoff, double poleAngle, Context context) {
            super(input, cutoff, context);
            this.poleAngle = poleAngle;
        }

        void calcCutoff() {
            double q = 1.0 / (2.0 * Math.cos(poleAngle));
            double fs = context.frequency();
            double w0 = 2.0 * Math.PI * fc / fs;
            double sinW0 = Math.sin(w0);
            double cosW0 = Math.cos(w0);
            double alpha = sinW0 / (2.0 * q);
            double a0 = 1.0 + alpha;
            a1 = -2.0 * cosW0 / a0;
            a2 = (1.0 - alpha) / a0;
            calcB(cosW0, a0);
        }

        // calcB() sets the feedforward (b) coefficients specific to each filter type.
        abstract void calcB(double cosW0, double a0);

        double doubleAtC(long time) {
            double x0 = input.doubleAt(time);
            double x1 = input.doubleAt(time - 1);
            double x2 = input.doubleAt(time - 2);
            double y1 = holder.doubleAt(time - 1);
            double y2 = holder.doubleAt(time - 2);
            return b0 * x0 + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
        }
    }
    protected abstract DoubleSignal computeOutput(DoubleSignal input, DoubleSignal cutoff);
}
