package org.jaust.filter;

import org.jaust.Context;
import org.jaust.signal.DoubleSignal;

// First-order IIR high-pass filter: y[t] = alpha * (y[t-1] + x[t] - x[t-1]).
public class IirHighPass extends Filter {

    public IirHighPass(Context context) {
        super(context);
    }

    protected DoubleSignal computeOutput(DoubleSignal input, DoubleSignal cutoff) {
        IirHighPassSignal signal = new IirHighPassSignal(input, cutoff, context);
        DoubleSignal output = new SequentialDoubleCache(signal);
        signal.holder = output;
        return output;
    }

    class IirHighPassSignal extends FilterSignal {
        protected DoubleSignal holder;
        double alpha;

        IirHighPassSignal(DoubleSignal input, DoubleSignal cutoff, Context context) {
            super(input, cutoff, context);
        }

        void calcCutoff() {
            double fs = context.frequency();
            double dt = 1.0 / fs;
            double rc = 1.0 / (2.0 * Math.PI * fc);
            alpha = rc / (rc + dt);
        }

        double doubleAtC(long time) {
            return alpha * (holder.doubleAt(time - 1) + input.doubleAt(time) - input.doubleAt(time - 1));
        }
    }
}
