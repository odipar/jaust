package org.jaust.processor;

import org.jaust.Context;
import org.jaust.Processor;
import org.jaust.Signal;

/**
 * Implements the Faust split composition operator ({@code <:}).
 * <p>
 * {@code p1 <: p2} distributes p1's outputs to p2's inputs. If p1 has {@code n} outputs and
 * p2 has {@code m} inputs, {@code m} must be a multiple of {@code n}. Input {@code i} of p2
 * receives output {@code i % n} of p1.
 */
public record DivProcessor(Processor p1, Processor p2) implements DefaultProcessor {

    public Context context() { return p1.context(); }

    public Signal.Type[] inType() { return p1.inType(); }

    public Signal.Type[] outType() { return p2.outType(); }

    public Signal[] apply(Signal... signal) {
        Signal[] s1 = p1.apply(signal);
        int n = s1.length;
        int m = p2.inType().length;
        Signal[] expanded = new Signal[m];
        for (int i = 0; i < m; i++) {
            expanded[i] = s1[i % n];
        }
        return p2.apply(expanded);
    }
}
