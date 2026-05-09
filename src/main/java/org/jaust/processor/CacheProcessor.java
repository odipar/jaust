package org.jaust.processor;

import org.jaust.Processor;
import org.jaust.Signal;
import org.jaust.signal.DoubleSignal;
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
    
    public org.jaust.Signal[] apply(org.jaust.Signal... signal) {
        Signal[] result = processor.apply(signal);
        Signal[] wrapped = new Signal[result.length];
        
        for (int i = 0; i < result.length; i++) {
            var s =  result[i];
            var wr = switch (s) {
                // TODO: add non-double signal types
                case DoubleSignal ds -> new DoubleCache(ds);
                default -> throw new UnsupportedOperationException("Unsupported signal type: " + s.type());
            };
            wrapped[i] = wr;
        }
        return wrapped;
    }
}
