package org.jaust.processor;

import org.jaust.Processor;
import org.jaust.Signal;
import org.jaust.signal.DoubleSignal;
import org.jaust.signal.SignalArray;
import org.jaust.signal.array.DefaultArray;
import org.jaust.signal.cache.DoubleCache;

public record CacheProcessor(Processor processor) implements DefaultProcessor {
    
    public org.jaust.Context context() {
        return processor.context();
    }
    
    public org.jaust.Signal.Type[] inType() {
        return processor.inType();
    }
    
    public org.jaust.Signal.Type[] outType() {
        return processor.outType();
    }
    
    public SignalArray apply(SignalArray signal) {
        SignalArray result = processor.apply(signal);
        int len = result.length();
        Signal[] wrapped = new Signal[len];
        
        for (int i = 0; i < len; i++) {
            var s = result.at(i);
            var wr = switch (s) {
                // TODO: add non-double signal types
                case DoubleSignal ds -> new DoubleCache(ds);
                default -> throw new UnsupportedOperationException("Unsupported signal type: " + s.type());
            };
            wrapped[i] = wr;
        }
        return DefaultArray.a(wrapped);
    }
}
