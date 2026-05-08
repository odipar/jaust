package org.jaust.main;

import org.jaust.Context;

import java.util.function.BiFunction;
import java.util.function.DoubleBinaryOperator;
import static java.lang.Math.*;

public class Start {
    static void main(String[] args) {
        example(null);
    }

    interface MyContext extends Context{
        default long frequency() {
            return 44100;
        }
    }
    
    static void example(MyContext c) {
        var s440 = c.genD(t -> sin(2 * PI * 440 * t / c.frequency()));
        var s880 = c.genD(t -> sin(2 * PI * 880 * t / c.frequency()));
        var add = c.binD(Double::sum);
        
        s440.
            par(s880).
            seq(add);
        
    }
}
