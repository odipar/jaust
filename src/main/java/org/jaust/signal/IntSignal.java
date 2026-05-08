package org.jaust.signal;

import org.jaust.Signal;

public interface IntSignal extends Signal {
    default Signal.Type type() { return  Signal.Type.INT; }
    
    default long longAt(long time) { return ((long) intAt(time) << 32); }
    default boolean boolAt(long time) { return intAt(time) != 0; }
    default double doubleAt(long time) { return (double) intAt(time) / Integer.MAX_VALUE; }
}
