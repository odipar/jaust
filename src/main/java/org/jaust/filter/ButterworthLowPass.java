package org.jaust.filter;

import org.jaust.Context;
import org.jaust.signal.DoubleSignal;

// 4th-order critically-damped low-pass filter implemented as a cascade of two second-order sections (biquads).
// Uses Q=0.5 (critical damping) per section to prevent step response overshoot.
public class ButterworthLowPass extends Filter {

    public ButterworthLowPass(Context context) {
        super(context);
    }

    protected DoubleSignal computeOutput(DoubleSignal input, DoubleSignal cutoff) {
        DoubleSignal stage1 = biquadSection(input, cutoff, 0.0);
        return biquadSection(stage1, cutoff, 0.0);
    }
    
    class LowPassBiquad extends Filter.Biquad {
        public LowPassBiquad(DoubleSignal input, DoubleSignal cutoff, double poleAngle, Context context) {
            super(input, cutoff, poleAngle, context);
        }

        void calcB(double cosW0, double a0) {
            b0 = ((1.0 - cosW0) / 2.0) / a0;
            b1 = (1.0 - cosW0) / a0;
            b2 = b0;
        }
    }
    
    private DoubleSignal biquadSection(DoubleSignal input, DoubleSignal cutoff, double poleAngle) {
        LowPassBiquad biquad = new LowPassBiquad(input, cutoff, poleAngle, context);
        DoubleSignal output = new SequentialDoubleCache(biquad);
        biquad.holder = output;
        return output;
    }
}
