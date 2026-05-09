package org.jaust.signal;

import org.jaust.Signal;

// A Signal producing double values; other types are scaled proportionally to their full range.
public interface DoubleSignal extends Signal {
    default Type type() { return  Type.DOUBLE; }
    
    default int intAt(long time) { return (int) (doubleAt(time) * Integer.MAX_VALUE); }
    default long longAt(long time) { return (long) (doubleAt(time) * Long.MAX_VALUE); }
    default boolean boolAt(long time) { return doubleAt(time) != 0.0; }
}
