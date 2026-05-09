package org.jaust.processor;

import org.jaust.Processor;
import static org.jaust.processor.array.DefaultProcessorArray.of;

// Mixin interface that provides default combinator implementations (par, seq, div, avg, rec) delegating to the context.
public interface DefaultProcessor extends Processor {
    
    // default implementations for combinators
    default Processor par(Processor... processors) {
        return context().par(of(processors).prepend(this));
    }
    default Processor seq(Processor... processors) {
        return context().seq(of(processors).prepend(this));
    }
    default Processor div(Processor processor) {
        return context().div(this, processor);
    }
    default Processor avg(Processor processor) {
        return context().avg(this, processor);
    }
    default Processor rec(Processor processor) {
        return context().rec(this, processor);
    }
}
