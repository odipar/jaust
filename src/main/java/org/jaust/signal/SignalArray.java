package org.jaust.signal;

import org.jaust.Signal;
import java.util.function.Function;

public interface SignalArray {
    int length();
    Signal at(int index);
    Signal[] toArray();
    
    SignalArray prepend(Signal signal);
    SignalArray prepend(SignalArray signal);
    SignalArray append(Signal signal);
    SignalArray append(SignalArray signal);
    SignalArray map(Function<Signal, Signal> func);
}