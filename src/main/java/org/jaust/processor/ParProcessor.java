package org.jaust.processor;

import org.jaust.Context;
import org.jaust.Processor;
import org.jaust.Signal;
import java.util.Arrays;

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
    public Signal[] apply(Signal... signal) {
        var s1 = p1.apply(Arrays.copyOfRange(signal, 0, p1.inType().length));
        var s2 = p2.apply(Arrays.copyOfRange(signal, p1.inType().length, signal.length));
        var result = new Signal[s1.length + s2.length];
        System.arraycopy(s1, 0, result, 0, s1.length);
        System.arraycopy(s2, 0, result, s1.length, s2.length);
        return result;
    }

    private static Signal.Type[] concat(Signal.Type[] a, Signal.Type[] b) {
        var c = new Signal.Type[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }
}
