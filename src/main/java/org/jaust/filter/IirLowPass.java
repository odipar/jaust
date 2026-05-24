package org.jaust.filter;

import org.jaust.Context;
import org.jaust.signal.DoubleSignal;

// First-order IIR low-pass filter: y[t] = alpha * x[t] + (1 - alpha) * y[t-1].
public class IirLowPass extends Filter {

    public IirLowPass(Context context) {
        super(context);
    }

    protected DoubleSignal computeOutput(DoubleSignal input, DoubleSignal cutoff) {
        long fs = context.frequency();
        DoubleSignal[] holder = new DoubleSignal[1];
        DoubleSignal output = new SequentialDoubleCache(new DoubleSignal() {
            public Context context() { return IirLowPass.this.context; }

            public double doubleAt(long time) {
                if (time < 0) return 0.0;
                double fc = cutoff.doubleAt(time);
                double dt = 1.0 / fs;
                double rc = 1.0 / (2.0 * Math.PI * fc);
                double alpha = dt / (rc + dt);
                return alpha * input.doubleAt(time) + (1.0 - alpha) * holder[0].doubleAt(time - 1);
            }
        });
        holder[0] = output;
        return output;
    }
}
