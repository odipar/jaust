package org.jaust;

public interface Signal {
    enum Type { BOOL, INT, LONG, DOUBLE }
    
    Type type();
    Context context();
    
    int intAt(long time);
    long longAt(long time);
    double doubleAt(long time);
    boolean boolAt(long time);
}