package org.jaust.filter;

import org.jaust.Context;
import org.jaust.signal.DoubleSignal;

// FIR high-pass filter: subtracts the FIR low-pass from the original signal.
public class FirHighPass extends Filter {
    private final int order;

    public FirHighPass(Context context, int order) {
        super(context);
        this.order = order;
    }

    public FirHighPass(Context context) {
        this(context, 16);
    }

    protected DoubleSignal computeOutput(DoubleSignal input, DoubleSignal cutoff) {
        long fs = context.frequency();
        return new DoubleSignal() {
            public Context context() { return FirHighPass.this.context; }

            public double doubleAt(long time) {
                double fc = cutoff.doubleAt(time);
                int n = Math.max(1, Math.min(order, (int) (fs / (2.0 * fc))));
                double sum = 0.0;
                for (int i = 0; i < n; i++) {
                    long t = time - i;
                    sum += (t >= 0) ? input.doubleAt(t) : 0.0;
                }
                return input.doubleAt(time) - sum / n;
            }
        };
    }
}
