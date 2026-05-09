package org.jaust.operator;

// Functional interface for a primitive boolean binary operator (boolean, boolean) -> boolean.
@FunctionalInterface
public interface BooleanBinaryOperator {
    boolean apply(boolean a, boolean b);
}
