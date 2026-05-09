package org.jaust.processor;

import org.jaust.Context;
import org.jaust.Processor;
import org.jaust.Signal;
import org.jaust.signal.DoubleSignal;
import org.jaust.signal.SignalArray;
import org.jaust.signal.array.DefaultArray;

/**
 * Implements the Faust merge composition operator ({@code >:}).
 * <p>
 * {@code p1 >: p2}: p1 has {@code m} outputs and p2 has {@code n} inputs, where {@code m} must
 * be a multiple of {@code n}. Input {@code j} of p2 receives the average of p1's outputs at
 * indices {@code j, j+n, j+2n, ...}.
 */
public record AvgProcessor(Processor p1, Processor p2) implements DefaultProcessor {

    public Context context() { return p1.context(); }

    public Signal.Type[] inType() { return p1.inType(); }

    public Signal.Type[] outType() { return p2.outType(); }

    public SignalArray apply(SignalArray signal) {
        SignalArray s1 = p1.apply(signal);
        int m = s1.length();      // number of p1 outputs
        int n = p2.inType().length; // number of p2 inputs
        if (m % n != 0) {
            throw new IllegalArgumentException(
                "p1 output count (" + m + ") must be a multiple of p2 input count (" + n + ")");
        }
        int k = m / n;            // number of signals merged per p2 input

        SignalArray merged = DefaultArray.generate(n, j -> {
            Signal[] contributing = new Signal[k];
            for (int x = 0; x < k; x++) {
                contributing[x] = s1.at(j + x * n);
            }
            return new DoubleSignal() {
                public Context context() { return p1.context(); }
                public double doubleAt(long time) {
                    double sum = 0;
                    for (Signal s : contributing) {
                        sum += s.doubleAt(time);
                    }
                    return sum / k;
                }
            };
        });

        return p2.apply(merged);
    }
}
