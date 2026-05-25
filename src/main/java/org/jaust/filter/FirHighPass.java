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
        return new FirHighPassSignal(input, cutoff, order, context);
    }

    class FirHighPassSignal extends FilterSignal {
        final int order;
        double[] coeff;
        double coeffSum;
        int m;

        FirHighPassSignal(DoubleSignal input, DoubleSignal cutoff, int order, Context context) {
            super(input, cutoff, context);
            this.order = order;
        }

        void calcCutoff() {
            double fs = context.frequency();
            double normalized = fc / fs; // normalized cutoff (0..0.5)
            m = order / 2; // center of the filter
            coeff = new double[order];
            coeffSum = 0.0;
            for (int i = 0; i < order; i++) {
                int k = i - m;
                double h;
                if (k == 0) {
                    h = 2.0 * normalized;
                } else {
                    h = Math.sin(2.0 * Math.PI * normalized * k) / (Math.PI * k);
                }
                // Hamming window
                h *= 0.54 - 0.46 * Math.cos(2.0 * Math.PI * i / (order - 1));
                coeff[i] = h;
                coeffSum += h;
            }
        }

        double doubleAtC(long time) {
            double sum = 0.0;
            for (int i = 0; i < order; i++) {
                long t = time - i;
                double x = (t >= 0) ? input.doubleAt(t) : 0.0;
                sum += coeff[i] * x;
            }
            // Spectral inversion: high-pass = delta - low-pass
            double lpSample = (coeffSum != 0.0) ? sum / coeffSum : 0.0;
            return input.doubleAt(Math.max(0, time - m)) - lpSample;
        }
    }
}
