package org.jaust.processor;

import org.jaust.Processor;

public interface DefaultProcessor extends Processor {
    
    // default implementations for combinators
    default Processor par(Processor... processors) {
        return context().par(prepend(this, processors));
    }
    default Processor seq(Processor... processors) {
        return context().seq(prepend(this, processors));
    }
    default Processor div(Processor processor) {
        return context().div(this, processor);
    }
    default Processor rec(Processor processor) {
        return context().rec(this, processor);
    }
    
    static Processor[] prepend(Processor t, Processor... processors) {
        // prepend t to processors
        Processor[] result = new Processor[processors.length + 1];
        result[0] = t;
        System.arraycopy(processors, 0, result, 1, processors.length);
        return result;
    }
}
