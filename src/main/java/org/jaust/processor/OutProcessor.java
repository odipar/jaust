package org.jaust.processor;

import org.jaust.Context;
import org.jaust.Signal;

public record OutProcessor(Context context, Signal output) implements DefaultProcessor {
    public Signal.Type[] inType() {
        return new Signal.Type[]{};
    }
    public Signal.Type[] outType() {
        return new Signal.Type[]{output.type()};
    }
    public Signal[] apply(Signal... signal) {
        return new Signal[0];
    }
}
