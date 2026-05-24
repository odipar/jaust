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
                if (time < 0) return 0.0;
                double fc = cutoff.doubleAt(time);
                double normalized = fc / fs; // normalized cutoff (0..0.5)
                int m = order / 2; // center of the filter
                double sum = 0.0;
                double coeffSum = 0.0;
                for (int i = 0; i < order; i++) {
                    long t = time - i;
                    double x = (t >= 0) ? input.doubleAt(t) : 0.0;
                    // windowed-sinc low-pass coefficient
                    int k = i - m;
                    double h;
                    if (k == 0) {
                        h = 2.0 * normalized;
                    } else {
                        h = Math.sin(2.0 * Math.PI * normalized * k) / (Math.PI * k);
                    }
                    // Hamming window
                    h *= 0.54 - 0.46 * Math.cos(2.0 * Math.PI * i / (order - 1));
                    sum += h * x;
                    coeffSum += h;
                }
                // Spectral inversion: high-pass = delta - low-pass
                double lpSample = (coeffSum != 0.0) ? sum / coeffSum : 0.0;
                return input.doubleAt(Math.max(0, time - m)) - lpSample;
            }
        };
    }
}
