package org.jaust;

public interface Processor {
    Context context();
    
    Signal.Type[] inType();
    Signal.Type[] outType();
    
    Signal[] apply(Signal... signal);
    
    // fluent api (should be implemented with the context combinators)
    Processor par(Processor... processors);
    Processor seq(Processor... processors);
    Processor div(Processor processor);
    Processor rec(Processor processor);
}