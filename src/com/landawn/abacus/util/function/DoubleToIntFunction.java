package com.landawn.abacus.util.function;

/**
 * Refer to JDK API documentation at: <a href="https://docs.oracle.com/javase/8/docs/api/java/util/function/package-summary.html">https://docs.oracle.com/javase/8/docs/api/java/util/function/package-summary.html</a>
 */
public interface DoubleToIntFunction extends java.util.function.DoubleToIntFunction {

    public static final DoubleToIntFunction DEFAULT = new DoubleToIntFunction() {
        @Override
        public int applyAsInt(double value) {
            return (int) value;
        }
    };

    @Override
    int applyAsInt(double value);
}