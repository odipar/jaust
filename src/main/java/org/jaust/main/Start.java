package org.jaust.main;

import org.jaust.Context;
import org.jaust.context.DefaultContext;

import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.DoubleBinaryOperator;
import static java.lang.Math.*;

public class Start {
    static void main(String[] args) {
        example(new DefaultContext(44100));
    }
    
    static void example(Context c) {
        var t1 = c.genD(t -> t+1);
        var t2 = c.genD(t -> t*2);
        var add = c.binD(Double::sum);
        
        var p =
            t1.
            par(t2).
            seq(add);
        
        var s = p.apply()[0];
        
        double t = 0.0;
        
        for (long i = 0; i < 10_000_000_000L; i++) {
            t += s.longAt(i);
        }
        System.out.println(t);
    }
}
