package org.jaust;

// A time-varying value that can be queried at any sample time. Supports bool, int, long, and double types.
public interface Signal {
    enum Type { BOOL, INT, LONG, DOUBLE }
    
    Type type();
    Context context();
    
    int intAt(long time);
    long longAt(long time);
    double doubleAt(long time);
    boolean boolAt(long time);
}