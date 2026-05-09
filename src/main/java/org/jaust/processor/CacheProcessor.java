package org.jaust.processor;

import org.jaust.Processor;
import org.jaust.signal.BooleanSignal;
import org.jaust.signal.DoubleSignal;
import org.jaust.signal.IntSignal;
import org.jaust.signal.LongSignal;
import org.jaust.signal.SignalArray;
import org.jaust.signal.cache.BooleanCache;
import org.jaust.signal.cache.DoubleCache;
import org.jaust.signal.cache.IntCache;
import org.jaust.signal.cache.LongCache;

// Wraps a processor to memoize each signal value per sample time, avoiding redundant recomputation.
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
        return processor.apply(signal).map(s -> switch (s) {
            case BooleanSignal bs -> new BooleanCache(bs);
            case IntSignal is     -> new IntCache(is);
            case LongSignal ls    -> new LongCache(ls);
            case DoubleSignal ds  -> new DoubleCache(ds);
            default -> throw new UnsupportedOperationException("Unsupported signal type: " + s.type());
        });
    }
}
