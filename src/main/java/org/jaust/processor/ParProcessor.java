package org.jaust.processor;

import org.jaust.Context;
import org.jaust.Processor;
import org.jaust.Signal;
import org.jaust.signal.SignalArray;

// Faust parallel composition (,): runs p1 and p2 side by side, concatenating their inputs and outputs.
public record ParProcessor(Processor p1, Processor p2) implements DefaultProcessor {

    public Context context() {
        return p1.context();
    }
    public Signal.Type[] inType() {
        return concat(p1.inType(), p2.inType());
    }
    public Signal.Type[] outType() {
        return concat(p1.outType(), p2.outType());
    }
    public SignalArray apply(SignalArray signal) {
        int n = p1.inType().length;
        var s1 = p1.apply(signal.slice(0, n));
        var s2 = p2.apply(signal.slice(n, signal.length()));
        return s1.append(s2);
    }

    private static Signal.Type[] concat(Signal.Type[] a, Signal.Type[] b) {
        var c = new Signal.Type[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }
}
