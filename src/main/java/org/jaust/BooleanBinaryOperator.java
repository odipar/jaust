package org.jaust;

@FunctionalInterface
public interface BooleanBinaryOperator {
    boolean apply(boolean a, boolean b);
}
