package org.jaust.filter;

import org.jaust.Context;
import org.jaust.signal.DoubleSignal;

// 4th-order Butterworth low-pass filter implemented as a cascade of two second-order sections (biquads).
public class ButterworthLowPass extends Filter {

    public ButterworthLowPass(Context context) {
        super(context);
    }

    protected DoubleSignal computeOutput(DoubleSignal input, DoubleSignal cutoff) {
        DoubleSignal stage1 = biquadSection(input, cutoff, Math.PI / 4.0);
        return biquadSection(stage1, cutoff, 3.0 * Math.PI / 8.0);
    }

    private DoubleSignal biquadSection(DoubleSignal input, DoubleSignal cutoff, double poleAngle) {
        long fs = context.frequency();
        double q = 1.0 / (2.0 * Math.cos(poleAngle));

        DoubleSignal[] holder = new DoubleSignal[1];
        DoubleSignal output = new SequentialDoubleCache(new DoubleSignal() {
            public Context context() { return ButterworthLowPass.this.context; }

            public double doubleAt(long time) {
                if (time <= 0) return input.doubleAt(Math.max(0, time));

                double fc = cutoff.doubleAt(time);
                double w0 = 2.0 * Math.PI * fc / fs;
                double alpha = Math.sin(w0) / (2.0 * q);

                double cosW0 = Math.cos(w0);
                double a0 = 1.0 + alpha;
                double a1 = -2.0 * cosW0 / a0;
                double a2 = (1.0 - alpha) / a0;
                double b0 = ((1.0 - cosW0) / 2.0) / a0;
                double b1 = (1.0 - cosW0) / a0;
                double b2 = b0;

                double x0 = input.doubleAt(time);
                double x1 = (time >= 1) ? input.doubleAt(time - 1) : 0.0;
                double x2 = (time >= 2) ? input.doubleAt(time - 2) : 0.0;
                double y1 = holder[0].doubleAt(time - 1);
                double y2 = (time >= 2) ? holder[0].doubleAt(time - 2) : 0.0;

                return b0 * x0 + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
            }
        });
        holder[0] = output;
        return output;
    }
}
