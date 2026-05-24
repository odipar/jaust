package org.jaust.filter;

import org.jaust.Context;
import org.jaust.Signal;
import org.jaust.context.DefaultContext;
import org.jaust.signal.DoubleSignal;
import org.jaust.signal.SignalArray;
import org.jaust.signal.array.DefaultArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FilterTest {

    private Context ctx;
    private static final long SAMPLE_RATE = 44100;

    @BeforeEach
    void setUp() {
        ctx = new DefaultContext(SAMPLE_RATE);
    }

    private DoubleSignal constantSignal(double value) {
        return new DoubleSignal() {
            public Context context() { return ctx; }
            public double doubleAt(long time) { return value; }
        };
    }

    private DoubleSignal sineSignal(double freqHz) {
        return new DoubleSignal() {
            public Context context() { return ctx; }
            public double doubleAt(long time) {
                return Math.sin(2.0 * Math.PI * freqHz * time / SAMPLE_RATE);
            }
        };
    }

    // --- Filter interface tests ---

    @Test
    void filter_hasCorrectInputOutputTypes() {
        Filter f = new FirLowPass(ctx);
        assertArrayEquals(new Signal.Type[]{ Signal.Type.DOUBLE, Signal.Type.DOUBLE }, f.inType());
        assertArrayEquals(new Signal.Type[]{ Signal.Type.DOUBLE }, f.outType());
    }

    // --- FIR Low-Pass tests ---

    @Test
    void firLowPass_constantSignalPassesThrough() {
        FirLowPass f = new FirLowPass(ctx);
        DoubleSignal input = constantSignal(5.0);
        DoubleSignal cutoff = constantSignal(1000.0);
        SignalArray out = f.apply(DefaultArray.a(input, cutoff));
        DoubleSignal result = (DoubleSignal) out.at(0);

        // After enough samples, a constant should pass through unchanged
        assertEquals(5.0, result.doubleAt(100), 1e-9);
    }

    @Test
    void firLowPass_attenuatesHighFrequency() {
        FirLowPass f = new FirLowPass(ctx, 32);
        DoubleSignal input = sineSignal(10000.0); // 10kHz signal
        DoubleSignal cutoff = constantSignal(1000.0); // 1kHz cutoff
        SignalArray out = f.apply(DefaultArray.a(input, cutoff));
        DoubleSignal result = (DoubleSignal) out.at(0);

        // Measure energy of output vs input over a window
        double inputEnergy = 0, outputEnergy = 0;
        for (long t = 100; t < 200; t++) {
            inputEnergy += input.doubleAt(t) * input.doubleAt(t);
            outputEnergy += result.doubleAt(t) * result.doubleAt(t);
        }
        assertTrue(outputEnergy < inputEnergy * 0.5, "High frequency should be attenuated");
    }

    // --- FIR High-Pass tests ---

    @Test
    void firHighPass_constantSignalIsBlocked() {
        FirHighPass f = new FirHighPass(ctx);
        DoubleSignal input = constantSignal(5.0);
        DoubleSignal cutoff = constantSignal(1000.0);
        SignalArray out = f.apply(DefaultArray.a(input, cutoff));
        DoubleSignal result = (DoubleSignal) out.at(0);

        // DC (constant) should be blocked by high-pass
        assertEquals(0.0, result.doubleAt(100), 1e-9);
    }

    @Test
    void firHighPass_passesHighFrequency() {
        FirHighPass f = new FirHighPass(ctx, 32);
        DoubleSignal input = sineSignal(10000.0); // 10kHz
        DoubleSignal cutoff = constantSignal(1000.0); // 1kHz cutoff
        SignalArray out = f.apply(DefaultArray.a(input, cutoff));
        DoubleSignal result = (DoubleSignal) out.at(0);

        // Measure energy - high frequency should mostly pass
        double inputEnergy = 0, outputEnergy = 0;
        for (long t = 100; t < 200; t++) {
            inputEnergy += input.doubleAt(t) * input.doubleAt(t);
            outputEnergy += result.doubleAt(t) * result.doubleAt(t);
        }
        assertTrue(outputEnergy > inputEnergy * 0.3, "High frequency should pass through");
    }

    // --- IIR Low-Pass tests ---

    @Test
    void iirLowPass_constantSignalPassesThrough() {
        IirLowPass f = new IirLowPass(ctx);
        DoubleSignal input = constantSignal(3.0);
        DoubleSignal cutoff = constantSignal(5000.0);
        SignalArray out = f.apply(DefaultArray.a(input, cutoff));
        DoubleSignal result = (DoubleSignal) out.at(0);

        // After settling, output should converge to the constant
        assertEquals(3.0, result.doubleAt(1000), 1e-3);
    }

    @Test
    void iirLowPass_attenuatesHighFrequency() {
        IirLowPass f = new IirLowPass(ctx);
        DoubleSignal input = sineSignal(10000.0);
        DoubleSignal cutoff = constantSignal(1000.0);
        SignalArray out = f.apply(DefaultArray.a(input, cutoff));
        DoubleSignal result = (DoubleSignal) out.at(0);

        double inputEnergy = 0, outputEnergy = 0;
        for (long t = 200; t < 400; t++) {
            inputEnergy += input.doubleAt(t) * input.doubleAt(t);
            outputEnergy += result.doubleAt(t) * result.doubleAt(t);
        }
        assertTrue(outputEnergy < inputEnergy * 0.3, "IIR low-pass should attenuate high frequency");
    }

    // --- IIR High-Pass tests ---

    @Test
    void iirHighPass_constantSignalIsBlocked() {
        IirHighPass f = new IirHighPass(ctx);
        DoubleSignal input = constantSignal(3.0);
        DoubleSignal cutoff = constantSignal(1000.0);
        SignalArray out = f.apply(DefaultArray.a(input, cutoff));
        DoubleSignal result = (DoubleSignal) out.at(0);

        // DC should decay to zero
        assertEquals(0.0, result.doubleAt(1000), 0.1);
    }

    @Test
    void iirHighPass_passesHighFrequency() {
        IirHighPass f = new IirHighPass(ctx);
        DoubleSignal input = sineSignal(10000.0);
        DoubleSignal cutoff = constantSignal(1000.0);
        SignalArray out = f.apply(DefaultArray.a(input, cutoff));
        DoubleSignal result = (DoubleSignal) out.at(0);

        double inputEnergy = 0, outputEnergy = 0;
        for (long t = 200; t < 400; t++) {
            inputEnergy += input.doubleAt(t) * input.doubleAt(t);
            outputEnergy += result.doubleAt(t) * result.doubleAt(t);
        }
        assertTrue(outputEnergy > inputEnergy * 0.5, "IIR high-pass should pass high frequency");
    }

    // --- Butterworth Low-Pass tests ---

    @Test
    void butterworthLowPass_constantSignalPassesThrough() {
        ButterworthLowPass f = new ButterworthLowPass(ctx);
        DoubleSignal input = constantSignal(2.0);
        DoubleSignal cutoff = constantSignal(5000.0);
        SignalArray out = f.apply(DefaultArray.a(input, cutoff));
        DoubleSignal result = (DoubleSignal) out.at(0);

        assertEquals(2.0, result.doubleAt(2000), 0.01);
    }

    @Test
    void butterworthLowPass_attenuatesHighFrequency() {
        ButterworthLowPass f = new ButterworthLowPass(ctx);
        DoubleSignal input = sineSignal(10000.0);
        DoubleSignal cutoff = constantSignal(1000.0);
        SignalArray out = f.apply(DefaultArray.a(input, cutoff));
        DoubleSignal result = (DoubleSignal) out.at(0);

        double inputEnergy = 0, outputEnergy = 0;
        for (long t = 500; t < 700; t++) {
            inputEnergy += input.doubleAt(t) * input.doubleAt(t);
            outputEnergy += result.doubleAt(t) * result.doubleAt(t);
        }
        assertTrue(outputEnergy < inputEnergy * 0.1,
                "4th-order Butterworth should strongly attenuate frequencies above cutoff");
    }

    // --- Butterworth High-Pass tests ---

    @Test
    void butterworthHighPass_constantSignalIsBlocked() {
        ButterworthHighPass f = new ButterworthHighPass(ctx);
        DoubleSignal input = constantSignal(2.0);
        DoubleSignal cutoff = constantSignal(1000.0);
        SignalArray out = f.apply(DefaultArray.a(input, cutoff));
        DoubleSignal result = (DoubleSignal) out.at(0);

        assertEquals(0.0, result.doubleAt(2000), 0.1);
    }

    @Test
    void butterworthHighPass_passesHighFrequency() {
        ButterworthHighPass f = new ButterworthHighPass(ctx);
        DoubleSignal input = sineSignal(10000.0);
        DoubleSignal cutoff = constantSignal(1000.0);
        SignalArray out = f.apply(DefaultArray.a(input, cutoff));
        DoubleSignal result = (DoubleSignal) out.at(0);

        double inputEnergy = 0, outputEnergy = 0;
        for (long t = 500; t < 700; t++) {
            inputEnergy += input.doubleAt(t) * input.doubleAt(t);
            outputEnergy += result.doubleAt(t) * result.doubleAt(t);
        }
        assertTrue(outputEnergy > inputEnergy * 0.5,
                "4th-order Butterworth high-pass should pass frequencies above cutoff");
    }

    // --- Butterworth amplitude range tests ---

    @Test
    void butterworthLowPass_outputDoesNotExceedInputRange() {
        ButterworthLowPass f = new ButterworthLowPass(ctx);
        // Input signal in range [0.0, 1.0]
        DoubleSignal input = new DoubleSignal() {
            public Context context() { return ctx; }
            public double doubleAt(long time) {
                // A signal that stays within [0.0, 1.0]: offset sine
                return 0.5 + 0.5 * Math.sin(2.0 * Math.PI * 1000.0 * time / SAMPLE_RATE);
            }
        };
        DoubleSignal cutoff = constantSignal(5000.0);
        SignalArray out = f.apply(DefaultArray.a(input, cutoff));
        DoubleSignal result = (DoubleSignal) out.at(0);

        double maxOutput = Double.NEGATIVE_INFINITY;
        double minOutput = Double.POSITIVE_INFINITY;
        for (long t = 0; t < 4000; t++) {
            double v = result.doubleAt(t);
            if (v > maxOutput) maxOutput = v;
            if (v < minOutput) minOutput = v;
        }
        assertTrue(maxOutput <= 1.0,
                "Butterworth low-pass output should not exceed input max (1.0), got: " + maxOutput);
        assertTrue(minOutput >= 0.0,
                "Butterworth low-pass output should not go below input min (0.0), got: " + minOutput);
    }

    @Test
    void butterworthHighPass_outputDoesNotExceedInputRange() {
        ButterworthHighPass f = new ButterworthHighPass(ctx);
        // Input signal in range [-1.0, 1.0]: a sine wave
        DoubleSignal input = sineSignal(5000.0);
        DoubleSignal cutoff = constantSignal(1000.0);
        SignalArray out = f.apply(DefaultArray.a(input, cutoff));
        DoubleSignal result = (DoubleSignal) out.at(0);

        // Skip initial transient (first 500 samples) and check steady-state amplitude
        double maxOutput = Double.NEGATIVE_INFINITY;
        double minOutput = Double.POSITIVE_INFINITY;
        for (long t = 500; t < 4000; t++) {
            double v = result.doubleAt(t);
            if (v > maxOutput) maxOutput = v;
            if (v < minOutput) minOutput = v;
        }
        assertTrue(maxOutput <= 1.0,
                "Butterworth high-pass output should not exceed input max (1.0), got: " + maxOutput);
        assertTrue(minOutput >= -1.0,
                "Butterworth high-pass output should not go below input min (-1.0), got: " + minOutput);
    }

    @Test
    void butterworthLowPass_stepResponseDoesNotOvershoot() {
        ButterworthLowPass f = new ButterworthLowPass(ctx);
        // Step signal: 0 for t<0, 1.0 for t>=0 (input range [0.0, 1.0])
        DoubleSignal input = new DoubleSignal() {
            public Context context() { return ctx; }
            public double doubleAt(long time) {
                return time >= 0 ? 1.0 : 0.0;
            }
        };
        DoubleSignal cutoff = constantSignal(5000.0);
        SignalArray out = f.apply(DefaultArray.a(input, cutoff));
        DoubleSignal result = (DoubleSignal) out.at(0);

        double maxOutput = Double.NEGATIVE_INFINITY;
        double minOutput = Double.POSITIVE_INFINITY;
        for (long t = 0; t < 4000; t++) {
            double v = result.doubleAt(t);
            if (v > maxOutput) maxOutput = v;
            if (v < minOutput) minOutput = v;
        }
        assertTrue(maxOutput <= 1.0,
                "Butterworth low-pass step response should not overshoot above 1.0, got: " + maxOutput);
        assertTrue(minOutput >= 0.0,
                "Butterworth low-pass step response should not undershoot below 0.0, got: " + minOutput);
    }

    @Test
    void butterworthHighPass_steadyStateSineDoesNotAmplify() {
        ButterworthHighPass f = new ButterworthHighPass(ctx);
        // Input signal well above cutoff frequency: amplitude should pass unchanged
        DoubleSignal input = sineSignal(10000.0); // 10kHz, well above 2kHz cutoff
        DoubleSignal cutoff = constantSignal(2000.0);
        SignalArray out = f.apply(DefaultArray.a(input, cutoff));
        DoubleSignal result = (DoubleSignal) out.at(0);

        // After transient settles, measure peak amplitude
        double maxAbsOutput = 0;
        for (long t = 1000; t < 4000; t++) {
            double v = Math.abs(result.doubleAt(t));
            if (v > maxAbsOutput) maxAbsOutput = v;
        }
        assertTrue(maxAbsOutput <= 1.0,
                "Butterworth high-pass should not amplify signals in passband, got peak: " + maxAbsOutput);
    }

    // --- Variable cutoff frequency test ---

    @Test
    void iirLowPass_variableCutoffFrequency() {
        IirLowPass f = new IirLowPass(ctx);
        DoubleSignal input = sineSignal(5000.0);
        // Cutoff sweeps from 100Hz to 10kHz
        DoubleSignal cutoff = new DoubleSignal() {
            public Context context() { return ctx; }
            public double doubleAt(long time) {
                return 100.0 + (10000.0 - 100.0) * time / 1000.0;
            }
        };
        SignalArray out = f.apply(DefaultArray.a(input, cutoff));
        DoubleSignal result = (DoubleSignal) out.at(0);

        // Early samples (low cutoff) should have less energy than later samples (high cutoff)
        double earlyEnergy = 0, lateEnergy = 0;
        for (long t = 50; t < 150; t++) {
            earlyEnergy += result.doubleAt(t) * result.doubleAt(t);
        }
        for (long t = 850; t < 950; t++) {
            lateEnergy += result.doubleAt(t) * result.doubleAt(t);
        }
        assertTrue(lateEnergy > earlyEnergy,
                "Higher cutoff should allow more signal through");
    }
}
