package org.jaust.filter;

import org.jaust.Context;
import org.jaust.signal.DoubleSignal;

// 4th-order critically-damped high-pass filter implemented as a cascade of two second-order sections (biquads).
// Uses Q=0.5 (critical damping) per section to prevent signal amplification beyond input range.
public class ButterworthHighPass extends Filter {

    public ButterworthHighPass(Context context) {
        super(context);
    }

    protected DoubleSignal computeOutput(DoubleSignal input, DoubleSignal cutoff) {
        DoubleSignal stage1 = biquadSection(input, cutoff, 0.0);
        return biquadSection(stage1, cutoff, 0.0);
    }

    class HighPassBiquad extends Filter.FilterSignal {
        protected DoubleSignal holder;
        
        double poleAngle;
        double a0;
        double a1;
        double a2;
        double b0;
        double b1;
        double b2;
        
        public HighPassBiquad(DoubleSignal input, DoubleSignal cutoff, double poleAngle, Context context) {
            super(input, cutoff, context);
            this.poleAngle = poleAngle;
        }
        void calcCutoff() {
            double q = 1.0 / (2.0 * Math.cos(poleAngle));
            double fs = context.frequency();
            double w0 = 2.0 * Math.PI * fc / fs;
            double alpha = Math.sin(w0) / (2.0 * q);
            
            double cosW0 = Math.cos(w0);
            
            a0 = 1.0 + alpha;
            a1 = -2.0 * cosW0 / a0;
            a2 = (1.0 - alpha) / a0;
            b0 = ((1.0 + cosW0) / 2.0) / a0;
            b1 = -(1.0 + cosW0) / a0;
            b2 = b0;
        }
        double doubleAtC(long time) {
            double x0 = input.doubleAt(time);
            double x1 = input.doubleAt(time - 1);
            double x2 = input.doubleAt(time - 2);
            double y1 = holder.doubleAt(time - 1);
            double y2 = holder.doubleAt(time - 2);
            
            return b0 * x0 + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
        }
    }
    private DoubleSignal biquadSection(DoubleSignal input, DoubleSignal cutoff, double poleAngle) {
        HighPassBiquad biquad = new HighPassBiquad(input, cutoff, poleAngle, context);
        DoubleSignal output = new SequentialDoubleCache(biquad);
        biquad.holder = output;
        return output;
        
    }
}
