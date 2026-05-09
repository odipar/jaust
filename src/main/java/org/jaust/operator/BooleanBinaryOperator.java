package org.jaust.operator;

@FunctionalInterface
public interface BooleanBinaryOperator {
    boolean apply(boolean a, boolean b);
}
