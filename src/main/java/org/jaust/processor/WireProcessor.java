package org.jaust.processor;

import org.jaust.Context;
import org.jaust.Signal;

public record WireProcessor(Context context, Signal.Type type) implements DefaultProcessor {
    public Signal.Type[] inType() {
        return new Signal.Type[]{type};
    }
    public Signal.Type[] outType() {
        return inType();
    }
    public Signal[] apply(Signal... signal) {
        assert(signal.length == 1);
        assert(signal[0].type() == type);
        
        return signal;
    }
}
