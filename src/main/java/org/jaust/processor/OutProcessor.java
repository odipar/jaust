package org.jaust.processor;

import org.jaust.Context;
import org.jaust.Signal;
import org.jaust.signal.SignalArray;
import org.jaust.signal.array.DefaultArray;

// Wraps a single Signal as a zero-input, one-output Processor.
public record OutProcessor(Signal output) implements DefaultProcessor {
    
    public Context context() {
        return output.context();
    }
    public Signal.Type[] inType() {
        return new Signal.Type[]{};
    }
    public Signal.Type[] outType() {
        return new Signal.Type[]{output.type()};
    }
    public SignalArray apply(SignalArray signal) {
        return DefaultArray.a(output);
    }
}
