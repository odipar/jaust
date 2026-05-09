package org.jaust.processor;

import org.jaust.Processor;

public interface DefaultProcessor extends Processor {
    
    // default implementations for combinators
    default Processor par(ProcessorArray processors) {
        return context().par(processors.prepend(this));
    }
    default Processor seq(ProcessorArray processors) {
        return context().seq(processors.prepend(this));
    }
    default Processor div(Processor processor) {
        return context().div(this, processor);
    }
    default Processor rec(Processor processor) {
        return context().rec(this, processor);
    }
}
