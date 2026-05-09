package org.jaust.signal;

import org.jaust.Signal;
import java.util.function.Function;

// An ordered, immutable array of Signals with slice, append, prepend, and map operations.
public interface SignalArray {
    int length();
    Signal at(int index);
    Signal[] toArray();
    SignalArray slice(int from, int to);

    SignalArray prepend(Signal signal);
    SignalArray prepend(SignalArray signal);
    SignalArray append(Signal signal);
    SignalArray append(SignalArray signal);
    SignalArray map(Function<Signal, Signal> func);
}