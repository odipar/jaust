package org.jaust.processor;

import org.jaust.Context;
import org.jaust.Signal;
import org.jaust.signal.SignalArray;

// Identity processor that passes a single typed signal through unchanged.
public record WireProcessor(Context context, Signal.Type type) implements DefaultProcessor {
    public Signal.Type[] inType() {
        return new Signal.Type[]{type};
    }
    public Signal.Type[] outType() {
        return inType();
    }
    public SignalArray apply(SignalArray signal) {
        assert(signal.length() == 1);
        assert(signal.at(0).type() == type);
        
        return signal;
    }
}
