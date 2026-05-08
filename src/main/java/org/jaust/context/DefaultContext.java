package org.jaust.context;

import org.jaust.Context;
import org.jaust.Processor;
import org.jaust.processor.*;
import org.jaust.signal.gen.DoubleGen;
import org.jaust.signal.gen.IntGen;
import org.jaust.signal.gen.LongGen;
import org.jaust.signal.val.BooleanVal;
import org.jaust.signal.val.DoubleVal;
import org.jaust.signal.val.IntVal;
import org.jaust.signal.val.LongVal;

import java.util.function.*;

public interface DefaultContext extends Context {
    
    default Processor val(boolean d) {
        return new OutProcessor(this, new BooleanVal(this, d));
    }
    default Processor val(int d) {
        return new OutProcessor(this, new IntVal(this, d));
    }
    default Processor val(long d) {
        return new OutProcessor(this, new LongVal(this, d));
    }
    default Processor val(double d) {
        return new OutProcessor(this, new DoubleVal(this, d));
    }
    default Processor genI(LongToIntFunction sup) {
        return new OutProcessor(this, new IntGen(this, sup));
    }
    default Processor genL(LongUnaryOperator sup) {
        return new OutProcessor(this, new LongGen(this, sup));
    }
    default Processor genD(LongToDoubleFunction sup) {
        return new OutProcessor(this, new DoubleGen(this, sup));
    }
    default Processor bin(DoubleBinaryOperator op) {
        return null;
    }
    default Processor par(Processor... processor) {
        return null;
    }
    default Processor seq(Processor... processors) {
        return null;
    }
    default Processor div(Processor processor) {
        return null;
    }
    default Processor rec(Processor processor) {
        return null;
    }
}
