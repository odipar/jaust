package org.jaust.filter;

import org.jaust.Context;
import org.jaust.signal.DoubleSignal;

// FIR low-pass filter using a windowed moving average. Window size adapts to cutoff frequency.
public class FirLowPass extends Filter {
    private final int order;

    public FirLowPass(Context context, int order) {
        super(context);
        this.order = order;
    }

    public FirLowPass(Context context) {
        this(context, 16);
    }

    protected DoubleSignal computeOutput(DoubleSignal input, DoubleSignal cutoff) {
        long fs = context.frequency();
        return new DoubleSignal() {
            public Context context() { return FirLowPass.this.context; }

            public double doubleAt(long time) {
                if (time < 0) return 0.0;
                double fc = cutoff.doubleAt(time);
                int n = Math.max(1, Math.min(order, (int) (fs / (2.0 * fc))));
                double sum = 0.0;
                for (int i = 0; i < n; i++) {
                    long t = time - i;
                    sum += (t >= 0) ? input.doubleAt(t) : 0.0;
                }
                return sum / n;
            }
        };
    }
}
