package org.jaust.filter;

import org.jaust.Context;
import org.jaust.signal.DoubleSignal;

// First-order IIR low-pass filter: y[t] = alpha * x[t] + (1 - alpha) * y[t-1].
public class IirLowPass extends Filter {

    public IirLowPass(Context context) {
        super(context);
    }

    protected DoubleSignal computeOutput(DoubleSignal input, DoubleSignal cutoff) {
        IirLowPassSignal signal = new IirLowPassSignal(input, cutoff, context);
        DoubleSignal output = new SequentialDoubleCache(signal);
        signal.holder = output;
        return output;
    }

    class IirLowPassSignal extends FilterSignal {
        protected DoubleSignal holder;
        double alpha;

        IirLowPassSignal(DoubleSignal input, DoubleSignal cutoff, Context context) {
            super(input, cutoff, context);
        }

        void calcCutoff() {
            double fs = context.frequency();
            double dt = 1.0 / fs;
            double rc = 1.0 / (2.0 * Math.PI * fc);
            alpha = dt / (rc + dt);
        }

        double doubleAtC(long time) {
            return alpha * input.doubleAt(time) + (1.0 - alpha) * holder.doubleAt(time - 1);
        }
    }
}
