package org.jaust;

import org.jaust.signal.SignalArray;
import org.jaust.signal.array.DefaultArray;

public interface Processor {
    Context context();
    
    Signal.Type[] inType();
    Signal.Type[] outType();
    
    SignalArray apply(SignalArray signal);
    
    default SignalArray apply() { return apply(DefaultArray.a()); }
    
    // fluent api (should be implemented with the context combinators)
    Processor par(Processor... processors);
    Processor seq(Processor... processors);
    Processor div(Processor processor);
    Processor rec(Processor processor);
}