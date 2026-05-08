package org.jaust;

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
    
    Processor par(Processor... processor);
    Processor seq(Processor... processors);
    Processor div(Processor p1, Processor p2);
    Processor rec(Processor p1, Processor p2);
}
