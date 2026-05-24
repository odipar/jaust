package org.jaust.filter;

import org.jaust.Context;
import org.jaust.signal.DoubleSignal;

// First-order IIR high-pass filter: y[t] = alpha * (y[t-1] + x[t] - x[t-1]).
public class IirHighPass extends Filter {

    public IirHighPass(Context context) {
        super(context);
    }

    protected DoubleSignal computeOutput(DoubleSignal input, DoubleSignal cutoff) {
        long fs = context.frequency();
        DoubleSignal[] holder = new DoubleSignal[1];
        DoubleSignal output = new SequentialDoubleCache(new DoubleSignal() {
            public Context context() { return IirHighPass.this.context; }

            public double doubleAt(long time) {
                if (time <= 0) return input.doubleAt(0);
                double fc = cutoff.doubleAt(time);
                double dt = 1.0 / fs;
                double rc = 1.0 / (2.0 * Math.PI * fc);
                double alpha = rc / (rc + dt);
                return alpha * (holder[0].doubleAt(time - 1) + input.doubleAt(time) - input.doubleAt(time - 1));
            }
        });
        holder[0] = output;
        return output;
    }
}
