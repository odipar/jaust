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

    class HighPassBiquad extends Filter.Biquad {
        public HighPassBiquad(DoubleSignal input, DoubleSignal cutoff, double poleAngle, Context context) {
            super(input, cutoff, poleAngle, context);
        }

        void calcB(double cosW0, double a0) {
            b0 = ((1.0 + cosW0) / 2.0) / a0;
            b1 = -(1.0 + cosW0) / a0;
            b2 = b0;
        }
    }
    private DoubleSignal biquadSection(DoubleSignal input, DoubleSignal cutoff, double poleAngle) {
        HighPassBiquad biquad = new HighPassBiquad(input, cutoff, poleAngle, context);
        DoubleSignal output = new SequentialDoubleCache(biquad);
        biquad.holder = output;
        return output;
        
    }
}
