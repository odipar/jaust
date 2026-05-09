package org.jaust.signal;

import org.jaust.Signal;

// A Signal producing boolean values; other types default to 0 (false) or 1 (true) scaled to their range.
public interface BooleanSignal extends Signal {
    default Signal.Type type() { return  Signal.Type.BOOL; }
    
    default int valueAt(long time) { return boolAt(time) ? 1 : 0; }
    default int intAt(long time) { return ( valueAt(time) * Integer.MAX_VALUE); }
    default long longAt(long time) { return ( valueAt(time) * Long.MAX_VALUE); }
    default double doubleAt(long time) { return valueAt(time); }
}

