package org.jaust.processor;

import org.jaust.Processor;

public record SeqProcessor(Processor p1, Processor p2) implements DefaultProcessor {
    public org.jaust.Context context() { return p1.context(); }

    public org.jaust.Signal.Type[] inType() { return p1.inType(); }

    public org.jaust.Signal.Type[] outType() { return p2.outType(); }

    public org.jaust.Signal[] apply(org.jaust.Signal... signal) {
        return p2.apply(p1.apply(signal));
    }
}
