package org.jaust.signal;

import org.jaust.Signal;

// A Signal producing long values; other types are scaled proportionally to their full range.
public interface LongSignal extends Signal {
    default Signal.Type type() { return  Signal.Type.LONG; }
    
    default int intAt(long time) { return (int) (longAt(time) >> 32); }
    default boolean boolAt(long time) { return longAt(time) != 0; }
    default double doubleAt(long time) { return (double) longAt(time) / Long.MAX_VALUE; }
}