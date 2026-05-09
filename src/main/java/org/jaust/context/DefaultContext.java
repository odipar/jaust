package org.jaust.context;

import org.jaust.operator.BooleanBinaryOperator;
import org.jaust.Context;
import org.jaust.Processor;
import org.jaust.Signal;
import org.jaust.processor.*;
import org.jaust.processor.bin.BoolBinProcessor;
import org.jaust.processor.bin.DoubleBinProcessor;
import org.jaust.processor.bin.IntBinProcessor;
import org.jaust.processor.bin.LongBinProcessor;
import org.jaust.processor.resample.DownProcessor;
import org.jaust.processor.resample.UpProcessor;
import org.jaust.signal.gen.BoolGen;
import org.jaust.signal.gen.DoubleGen;
import org.jaust.signal.gen.IntGen;
import org.jaust.signal.gen.LongGen;
import org.jaust.signal.val.BooleanVal;
import org.jaust.signal.val.DoubleVal;
import org.jaust.signal.val.IntVal;
import org.jaust.signal.val.LongVal;

import java.util.function.*;

// Standard implementation of Context. Creates typed signal sources, binary operators, and Faust composition primitives.
public record DefaultContext(long frequency) implements Context {
    
    public Processor valB(boolean d) {
        return new OutProcessor(new BooleanVal(this, d));
    }
    public Processor valI(int d) {
        return new OutProcessor(new IntVal(this, d));
    }
    public Processor valL(long d) {
        return new OutProcessor(new LongVal(this, d));
    }
    public Processor valD(double d) {
        return new OutProcessor(new DoubleVal(this, d));
    }
    public Processor genI(LongToIntFunction sup) {
        return new OutProcessor(new IntGen(this, sup));
    }
    public Processor genL(LongUnaryOperator sup) {
        return new OutProcessor(new LongGen(this, sup));
    }
    public Processor genB(LongPredicate sup) {
        return new OutProcessor(new BoolGen(this, sup));
    }
    public Processor genD(LongToDoubleFunction sup) {
        return new OutProcessor(new DoubleGen(this, sup));
    }
    public Processor binI(IntBinaryOperator op) {
        return new IntBinProcessor(this, op);
    }
    public Processor binL(LongBinaryOperator op) {
        return new LongBinProcessor(this, op);
    }
    public Processor binB(BooleanBinaryOperator op) {
        return new BoolBinProcessor(this, op);
    }
    public Processor wire(Signal.Type type) {
        return new WireProcessor(this, type);
    }
    public Processor cache(Processor p) {
        return new CacheProcessor(p);
    }
    public Processor binD(DoubleBinaryOperator op) {
        return new DoubleBinProcessor(this, op);
    }
    public Processor par(ProcessorArray processors) {
        return processors.reduce(ParProcessor::new);
    }
    public Processor seq(ProcessorArray processors) {
        return processors.reduce(SeqProcessor::new);
    }
    public Processor div(Processor p1, Processor p2) {
        return new DivProcessor(p1, p2);
    }
    public Processor avg(Processor p1, Processor p2) {
        return new AvgProcessor(p1, p2);
    }
    public Processor rec(Processor p1, Processor p2) {
        return new RecProcessor(p1, p2);
    }
    public Processor up(Processor source) {
        return new UpProcessor(this, source);
    }
    public Processor down(Processor source) {
        return new DownProcessor(this, source);
    }
}
