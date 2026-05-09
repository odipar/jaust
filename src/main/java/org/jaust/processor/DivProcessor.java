package org.jaust.processor;

import org.jaust.Context;
import org.jaust.Processor;
import org.jaust.Signal;
import org.jaust.signal.SignalArray;
import org.jaust.signal.array.DefaultArray;

// Faust split composition (<:): distributes p1's outputs across p2's inputs by cycling (input i gets output i % n).
public record DivProcessor(Processor p1, Processor p2) implements DefaultProcessor {

    public Context context() { return p1.context(); }

    public Signal.Type[] inType() { return p1.inType(); }

    public Signal.Type[] outType() { return p2.outType(); }

    public SignalArray apply(SignalArray signal) {
        SignalArray s1 = p1.apply(signal);
        int n = s1.length();
        int m = p2.inType().length;
        return p2.apply(DefaultArray.generate(m, i -> s1.at(i % n)));
    }
}
