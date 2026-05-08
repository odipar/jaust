package org.jaust.main;

import org.jaust.Context;
import org.jaust.context.DefaultContext;

import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.DoubleBinaryOperator;
import static java.lang.Math.*;

public class Start {
    public static void main(String[] args) {
        example(new DefaultContext(44100));
    }
    
    static void example(Context c) {
        var s440 = c.genL(t -> t+1);
        var s880 = c.genL(t -> t*2);
        var add = c.binL(Long::sum);
        
        var p =
            s440.
            par(s880).
            seq(add);
        
        var s1 = p.apply()[0];
        
        System.out.print(s1);
        for (long i = 0; i < 5; i++) {
            System.out.println(s1.longAt(i));
        }
    }
}
