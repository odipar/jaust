package org.jaust;

import org.jaust.processor.ProcessorArray;
import static org.jaust.processor.array.DefaultProcessorArray.of;

import java.util.function.*;

public interface Context {
    long frequency();
    
    Processor valB(boolean b);
    Processor valD(double d);
    Processor valI(int i);
    Processor valL(long l);
    
    Processor genD(LongToDoubleFunction sup);
    Processor genI(LongToIntFunction sup);
    Processor genL(LongUnaryOperator sup);
    Processor genB(LongPredicate sup);
    
    Processor binD(DoubleBinaryOperator op);
    Processor binI(IntBinaryOperator op);
    Processor binL(LongBinaryOperator op);
    Processor binB(BooleanBinaryOperator op);
    
    Processor wire(Signal.Type type);
    Processor cache(Processor p);
    
    // Faust-style composition operators (, : <: >: ~)
    // par (,)
    Processor par(ProcessorArray processors);
    // seq (:)
    Processor seq(ProcessorArray processors);
    // div (<: split)
    Processor div(Processor p1, Processor p2);
    // avg (>: merge)
    Processor avg(Processor p1, Processor p2);
    // rec (~)
    Processor rec(Processor p1, Processor p2);
    
    default Processor par(Processor... processors) { return par(of(processors)); }
    default Processor seq(Processor... processors) { return seq(of(processors)); }
    
    default Processor addD() { return binD((a, b) -> a + b); }
    default Processor mulD() { return binD((a, b) -> a * b); }
    default Processor subD() { return binD((a, b) -> a - b); }
    default Processor divD() { return binD((a, b) -> a / b); }
}
