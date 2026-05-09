package org.jaust.main;

import org.jaust.Context;
import org.jaust.context.DefaultContext;
import org.jaust.Signal.Type;
import org.jaust.processor.array.DefaultProcessorArray;

// Entry point with example programs demonstrating Faust-style signal composition.
public class Start {
    public static void main(String[] args) {
        //example1(new DefaultContext(44100));
        example2(new DefaultContext(44100));
    }
    
    static void example1(Context c) {
        var t1 = c.genD(t -> t+1);
        var t2 = c.genD(t -> t*2);
        var add = c.binD(Double::sum);
        
        var p =
            t1.
            par(t2).
            seq(add);
        
        var s = p.apply().at(0);
        
        double sum = 0.0;
        
        for (long i = 0; i < 10_000_000_000L; i++) {
            sum += s.longAt(i);
        }
        System.out.println(sum);
    }
    
    static void example2(Context c) {
        // Feedback loop: out(t) = out(t-1) + 1.0
        // with cache to avoid redundant computation of the feedback signal
        
        var p1 = c.addD();
        var p2 = c.cache(c.wire(Type.DOUBLE));
        var rec = c.rec(p1, p2);
        var combined = c.seq(c.valD(1.0), rec);
        var s = combined.apply().at(0);
        double sum = 0.0;
        
        for (long i = 0; i < 1_000_000_000; i++) {
            sum += s.doubleAt(i);
        }
        System.out.println(sum);
    }
}
