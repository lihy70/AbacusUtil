/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.landawn.abacus.util.stream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;

import com.landawn.abacus.exception.AbacusException;
import com.landawn.abacus.util.CompletableFuture;
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.DoubleList;
import com.landawn.abacus.util.DoubleSummaryStatistics;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.IndexedDouble;
import com.landawn.abacus.util.Multimap;
import com.landawn.abacus.util.MutableInt;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Nth;
import com.landawn.abacus.util.Optional;
import com.landawn.abacus.util.OptionalDouble;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BinaryOperator;
import com.landawn.abacus.util.function.DoubleBiFunction;
import com.landawn.abacus.util.function.DoubleBinaryOperator;
import com.landawn.abacus.util.function.DoubleConsumer;
import com.landawn.abacus.util.function.DoubleFunction;
import com.landawn.abacus.util.function.DoubleNFunction;
import com.landawn.abacus.util.function.DoublePredicate;
import com.landawn.abacus.util.function.DoubleSupplier;
import com.landawn.abacus.util.function.DoubleToFloatFunction;
import com.landawn.abacus.util.function.DoubleToIntFunction;
import com.landawn.abacus.util.function.DoubleToLongFunction;
import com.landawn.abacus.util.function.DoubleTriFunction;
import com.landawn.abacus.util.function.DoubleUnaryOperator;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.ObjDoubleConsumer;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.function.ToDoubleFunction;

/**
 * Note: It's copied from OpenJDK at: http://hg.openjdk.java.net/jdk8u/hs-dev/jdk
 * <br />
 * 
 * A sequence of primitive double-valued elements supporting sequential and parallel
 * aggregate operations.  This is the {@code double} primitive specialization of
 * {@link Stream}.
 *
 * <p>The following example illustrates an aggregate operation using
 * {@link Stream} and {@link DoubleStream}, computing the sum of the weights of the
 * red widgets:
 *
 * <pre>{@code
 *     double sum = widgets.stream()
 *                         .filter(w -> w.getColor() == RED)
 *                         .mapToDouble(w -> w.getWeight())
 *                         .sum();
 * }</pre>
 *
 * See the class documentation for {@link Stream} and the package documentation
 * for <a href="package-summary.html">java.util.stream</a> for additional
 * specification of streams, stream operations, stream pipelines, and
 * parallelism.
 *
 * @since 1.8
 * @see Stream
 * @see <a href="package-summary.html">java.util.stream</a>
 */
public abstract class DoubleStream
        extends StreamBase<Double, double[], DoublePredicate, DoubleConsumer, DoubleList, OptionalDouble, IndexedDouble, DoubleStream> {

    private static final DoubleStream EMPTY = new ArrayDoubleStream(N.EMPTY_DOUBLE_ARRAY, null, true);

    DoubleStream(final Collection<Runnable> closeHandlers, final boolean sorted) {
        super(closeHandlers, sorted, null);
    }

    /**
     * Returns a stream consisting of the results of applying the given
     * function to the elements of this stream.
     *
     * <p>This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * @param mapper a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *               <a href="package-summary.html#Statelessness">stateless</a>
     *               function to apply to each element
     * @return the new stream
     */
    public abstract DoubleStream map(DoubleUnaryOperator mapper);

    /**
     * Returns an {@code IntStream} consisting of the results of applying the
     * given function to the elements of this stream.
     *
     * <p>This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * @param mapper a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *               <a href="package-summary.html#Statelessness">stateless</a>
     *               function to apply to each element
     * @return the new stream
     */
    public abstract IntStream mapToInt(DoubleToIntFunction mapper);

    /**
     * Returns a {@code LongStream} consisting of the results of applying the
     * given function to the elements of this stream.
     *
     * <p>This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * @param mapper a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *               <a href="package-summary.html#Statelessness">stateless</a>
     *               function to apply to each element
     * @return the new stream
     */
    public abstract LongStream mapToLong(DoubleToLongFunction mapper);

    /**
     * Returns a {@code FloatStream} consisting of the results of applying the
     * given function to the elements of this stream.
     *
     * <p>This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * @param mapper a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *               <a href="package-summary.html#Statelessness">stateless</a>
     *               function to apply to each element
     * @return the new stream
     */
    public abstract FloatStream mapToFloat(DoubleToFloatFunction mapper);

    /**
     * Returns an object-valued {@code Stream} consisting of the results of
     * applying the given function to the elements of this stream.
     *
     * <p>This is an <a href="package-summary.html#StreamOps">
     *     intermediate operation</a>.
     *
     * @param <U> the element type of the new stream
     * @param mapper a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *               <a href="package-summary.html#Statelessness">stateless</a>
     *               function to apply to each element
     * @return the new stream
     */
    public abstract <U> Stream<U> mapToObj(DoubleFunction<? extends U> mapper);

    /**
     * Returns a stream consisting of the results of replacing each element of
     * this stream with the contents of a mapped stream produced by applying
     * the provided mapping function to each element.  Each mapped stream is
     * {@link java.util.stream.Baseclose() closed} after its contents
     * have been placed into this stream.  (If a mapped stream is {@code null}
     * an empty stream is used, instead.)
     *
     * <p>This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * @param mapper a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *               <a href="package-summary.html#Statelessness">stateless</a>
     *               function to apply to each element which produces a
     *               {@code DoubleStream} of new values
     * @return the new stream
     * @see Stream#flatMap(Function)
     */
    public abstract DoubleStream flatMap(DoubleFunction<? extends DoubleStream> mapper);

    public abstract IntStream flatMapToInt(DoubleFunction<? extends IntStream> mapper);

    public abstract LongStream flatMapToLong(DoubleFunction<? extends LongStream> mapper);

    public abstract FloatStream flatMapToFloat(DoubleFunction<? extends FloatStream> mapper);

    public abstract <T> Stream<T> flatMapToObj(DoubleFunction<? extends Stream<T>> mapper);

    public abstract DoubleStream top(int n);

    public abstract DoubleStream top(final int n, Comparator<? super Double> comparator);

    public abstract DoubleList toDoubleList();

    /**
     * 
     * @param classifier
     * @return
     * @see Collectors#groupingBy(Function)
     */
    public abstract <K> Map<K, List<Double>> toMap(DoubleFunction<? extends K> classifier);

    /**
     * 
     * @param classifier
     * @param mapFactory
     * @return
     * @see Collectors#groupingBy(Function, Supplier)
     */
    public abstract <K, M extends Map<K, List<Double>>> M toMap(final DoubleFunction<? extends K> classifier, final Supplier<M> mapFactory);

    /**
     * 
     * @param classifier
     * @param downstream
     * @return
     * @see Collectors#groupingBy(Function, Collector)
     */
    public abstract <K, A, D> Map<K, D> toMap(final DoubleFunction<? extends K> classifier, final Collector<Double, A, D> downstream);

    /**
     * 
     * @param classifier
     * @param downstream
     * @param mapFactory
     * @return
     * @see Collectors#groupingBy(Function, Collector, Supplier)
     */
    public abstract <K, D, A, M extends Map<K, D>> M toMap(final DoubleFunction<? extends K> classifier, final Collector<Double, A, D> downstream,
            final Supplier<M> mapFactory);

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @return
     * @see Collectors#toMap(Function, Function)
     */
    public abstract <K, U> Map<K, U> toMap(DoubleFunction<? extends K> keyMapper, DoubleFunction<? extends U> valueMapper);

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @param mapSupplier
     * @return
     * @see Collectors#toMap(Function, Function, Supplier)
     */
    public abstract <K, U, M extends Map<K, U>> M toMap(DoubleFunction<? extends K> keyMapper, DoubleFunction<? extends U> valueMapper,
            Supplier<M> mapSupplier);

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @param mergeFunction
     * @return
     * @see Collectors#toMap(Function, Function, BinaryOperator)
     */
    public abstract <K, U> Map<K, U> toMap(DoubleFunction<? extends K> keyMapper, DoubleFunction<? extends U> valueMapper, BinaryOperator<U> mergeFunction);

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @param mergeFunction
     * @param mapSupplier
     * @return
     * @see Collectors#toMap(Function, Function, BinaryOperator, Supplier)
     */
    public abstract <K, U, M extends Map<K, U>> M toMap(DoubleFunction<? extends K> keyMapper, DoubleFunction<? extends U> valueMapper,
            BinaryOperator<U> mergeFunction, Supplier<M> mapSupplier);

    /**
     * 
     * @param keyMapper
     * @return
     * @see Collectors#toMultimap(Function)
     */
    public abstract <K> Multimap<K, Double, List<Double>> toMultimap(DoubleFunction<? extends K> keyMapper);

    /**
     * 
     * @param keyMapper
     * @param mapSupplier
     * @return
     * @see Collectors#toMultimap(Function, Supplier)
     */
    public abstract <K, V extends Collection<Double>> Multimap<K, Double, V> toMultimap(DoubleFunction<? extends K> keyMapper,
            Supplier<Multimap<K, Double, V>> mapSupplier);

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @return
     * @see Collectors#toMultimap(Function, Function)
     */
    public abstract <K, U> Multimap<K, U, List<U>> toMultimap(DoubleFunction<? extends K> keyMapper, DoubleFunction<? extends U> valueMapper);

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @param mapSupplier
     * @return
     * @see Collectors#toMap(Function, Function, BinaryOperator, Supplier)
     */
    public abstract <K, U, V extends Collection<U>> Multimap<K, U, V> toMultimap(DoubleFunction<? extends K> keyMapper, DoubleFunction<? extends U> valueMapper,
            Supplier<Multimap<K, U, V>> mapSupplier);

    /**
     * Performs a <a href="package-summary.html#Reduction">reduction</a> on the
     * elements of this stream, using the provided identity value and an
     * <a href="package-summary.html#Associativity">associative</a>
     * accumulation function, and returns the reduced value.  This is equivalent
     * to:
     * <pre>{@code
     *     double result = identity;
     *     for (double element : this stream)
     *         result = accumulator.applyAsDouble(result, element)
     *     return result;
     * }</pre>
     *
     * but is not constrained to execute sequentially.
     *
     * <p>The {@code identity} value must be an identity for the accumulator
     * function. This means that for all {@code x},
     * {@code accumulator.apply(identity, x)} is equal to {@code x}.
     * The {@code accumulator} function must be an
     * <a href="package-summary.html#Associativity">associative</a> function.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">terminal
     * operation</a>.
     *
     * @apiNote Sum, min, max, and average are all special cases of reduction.
     * Summing a stream of numbers can be expressed as:
    
     * <pre>{@code
     *     double sum = numbers.reduce(0, (a, b) -> a+b);
     * }</pre>
     *
     * or more compactly:
     *
     * <pre>{@code
     *     double sum = numbers.reduce(0, Double::sum);
     * }</pre>
     *
     * <p>While this may seem a more roundabout way to perform an aggregation
     * compared to simply mutating a running total in a loop, reduction
     * operations parallelize more gracefully, without needing additional
     * synchronization and with greatly reduced risk of data races.
     *
     * @param identity the identity value for the accumulating function
     * @param op an <a href="package-summary.html#Associativity">associative</a>,
     *           <a href="package-summary.html#NonInterference">non-interfering</a>,
     *           <a href="package-summary.html#Statelessness">stateless</a>
     *           function for combining two values
     * @return the result of the reduction
     * @see #sum()
     * @see #min()
     * @see #max()
     * @see #average()
     */
    public abstract double reduce(double identity, DoubleBinaryOperator op);

    /**
     * Performs a <a href="package-summary.html#Reduction">reduction</a> on the
     * elements of this stream, using an
     * <a href="package-summary.html#Associativity">associative</a> accumulation
     * function, and returns an {@code OptionalDouble} describing the reduced
     * value, if any. This is equivalent to:
     * <pre>{@code
     *     boolean foundAny = false;
     *     double result = null;
     *     for (double element : this stream) {
     *         if (!foundAny) {
     *             foundAny = true;
     *             result = element;
     *         }
     *         else
     *             result = accumulator.applyAsDouble(result, element);
     *     }
     *     return foundAny ? OptionalDouble.of(result) : OptionalDouble.empty();
     * }</pre>
     *
     * but is not constrained to execute sequentially.
     *
     * <p>The {@code accumulator} function must be an
     * <a href="package-summary.html#Associativity">associative</a> function.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">terminal
     * operation</a>.
     *
     * @param op an <a href="package-summary.html#Associativity">associative</a>,
     *           <a href="package-summary.html#NonInterference">non-interfering</a>,
     *           <a href="package-summary.html#Statelessness">stateless</a>
     *           function for combining two values
     * @return the result of the reduction
     * @see #reduce(double, DoubleBinaryOperator)
     */
    public abstract OptionalDouble reduce(DoubleBinaryOperator op);

    /**
     * Performs a <a href="package-summary.html#MutableReduction">mutable
     * reduction</a> operation on the elements of this stream.  A mutable
     * reduction is one in which the reduced value is a mutable result container,
     * such as an {@code ArrayList}, and elements are incorporated by updating
     * the state of the result rather than by replacing the result.  This
     * produces a result equivalent to:
     * <pre>{@code
     *     R result = supplier.get();
     *     for (double element : this stream)
     *         accumulator.accept(result, element);
     *     return result;
     * }</pre>
     *
     * <p>Like {@link #reduce(double, DoubleBinaryOperator)}, {@code collect}
     * operations can be parallelized without requiring additional
     * synchronization.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">terminal
     * operation</a>.
     *
     * @param <R> type of the result
     * @param supplier a function that creates a new result container. For a
     *                 parallel execution, this function may be called
     *                 multiple times and must return a fresh value each time.
     * @param accumulator an <a href="package-summary.html#Associativity">associative</a>,
     *                    <a href="package-summary.html#NonInterference">non-interfering</a>,
     *                    <a href="package-summary.html#Statelessness">stateless</a>
     *                    function for incorporating an additional element into a result
     * @param combiner an <a href="package-summary.html#Associativity">associative</a>,
     *                    <a href="package-summary.html#NonInterference">non-interfering</a>,
     *                    <a href="package-summary.html#Statelessness">stateless</a>
     *                    function for combining two values, which must be
     *                    compatible with the accumulator function
     * @return the result of the reduction
     * @see Stream#collect(Supplier, BiConsumer, BiConsumer)
     */
    public abstract <R> R collect(Supplier<R> supplier, ObjDoubleConsumer<R> accumulator, BiConsumer<R, R> combiner);

    /**
     * 
     * @param supplier
     * @param accumulator
     * @return
     */
    public abstract <R> R collect(Supplier<R> supplier, ObjDoubleConsumer<R> accumulator);

    /**
     * Head and tail should be used by pair. If only one is called, should use first() or skip(1) instead.
     * Don't call any other methods with this stream after head() and tail() are called. 
     * 
     * @return
     * @throws NoSuchElementException if this stream is empty.
     */
    public abstract double head();

    /**
     * Head and tail should be used by pair. If only one is called, should use first() or skip(1) instead.
     * Don't call any other methods with this stream after head() and tail() are called. 
     * 
     * @return
     * @throws IllegalStateException if this stream is empty.
     */
    public abstract DoubleStream tail();

    /**
     * Returns an {@code OptionalDouble} describing the minimum element of this
     * stream, or an empty OptionalDouble if this stream is empty.  The minimum
     * element will be {@code Double.NaN} if any stream element was NaN. Unlike
     * the numerical comparison operators, this method considers negative zero
     * to be strictly smaller than positive zero. This is a special case of a
     * <a href="package-summary.html#Reduction">reduction</a> and is
     * equivalent to:
     * <pre>{@code
     *     return reduce(Double::min);
     * }</pre>
     *
     * <p>This is a <a href="package-summary.html#StreamOps">terminal
     * operation</a>.
     *
     * @return an {@code OptionalDouble} containing the minimum element of this
     * stream, or an empty optional if the stream is empty
     */
    public abstract OptionalDouble min();

    /**
     * Returns an {@code OptionalDouble} describing the maximum element of this
     * stream, or an empty OptionalDouble if this stream is empty.  The maximum
     * element will be {@code Double.NaN} if any stream element was NaN. Unlike
     * the numerical comparison operators, this method considers negative zero
     * to be strictly smaller than positive zero. This is a
     * special case of a
     * <a href="package-summary.html#Reduction">reduction</a> and is
     * equivalent to:
     * <pre>{@code
     *     return reduce(Double::max);
     * }</pre>
     *
     * <p>This is a <a href="package-summary.html#StreamOps">terminal
     * operation</a>.
     *
     * @return an {@code OptionalDouble} containing the maximum element of this
     * stream, or an empty optional if the stream is empty
     */
    public abstract OptionalDouble max();

    /**
     * 
     * @param k
     * @return OptionalByte.empty() if there is no element or count less than k, otherwise the kth largest element.
     */
    public abstract OptionalDouble kthLargest(int k);

    /**
     * Returns the sum of elements in this stream.
     *
     * Summation is a special case of a <a
     * href="package-summary.html#Reduction">reduction</a>. If
     * floating-point summation were exact, this method would be
     * equivalent to:
     *
     * <pre>{@code
     *     return reduce(0, Double::sum);
     * }</pre>
     *
     * However, since floating-point summation is not exact, the above
     * code is not necessarily equivalent to the summation computation
     * done by this method.
     *
     * <p>If any stream element is a NaN or the sum is at any point a NaN
     * then the sum will be NaN.
     *
     * The value of a floating-point sum is a function both
     * of the input values as well as the order of addition
     * operations. The order of addition operations of this method is
     * intentionally not defined to allow for implementation
     * flexibility to improve the speed and accuracy of the computed
     * result.
     *
     * In particular, this method may be implemented using compensated
     * summation or other technique to reduce the error bound in the
     * numerical sum compared to a simple summation of {@code double}
     * values.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">terminal
     * operation</a>.
     *
     * @apiNote Elements sorted by increasing absolute magnitude tend
     * to yield more accurate results.
     *
     * @return the sum of elements in this stream
     */
    public abstract Double sum();

    /**
     * Returns an {@code OptionalDouble} describing the arithmetic
     * mean of elements of this stream, or an empty optional if this
     * stream is empty.
     *
     * If any recorded value is a NaN or the sum is at any point a NaN
     * then the average will be NaN.
     *
     * <p>The average returned can vary depending upon the order in
     * which values are recorded.
     *
     * This method may be implemented using compensated summation or
     * other technique to reduce the error bound in the {@link #sum
     * numerical sum} used to compute the average.
     *
     *  <p>The average is a special case of a <a
     *  href="package-summary.html#Reduction">reduction</a>.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">terminal
     * operation</a>.
     *
     * @apiNote Elements sorted by increasing absolute magnitude tend
     * to yield more accurate results.
     *
     * @return an {@code OptionalDouble} containing the average element of this
     * stream, or an empty optional if the stream is empty
     */
    public abstract OptionalDouble average();

    public abstract DoubleSummaryStatistics summarize();

    public abstract Pair<DoubleSummaryStatistics, Optional<Map<Percentage, Double>>> summarize2();

    /**
     * 
     * @param b
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public abstract DoubleStream merge(final DoubleStream b, final DoubleBiFunction<Nth> nextSelector);

    public abstract DoubleStream zipWith(DoubleStream b, DoubleBiFunction<Double> zipFunction);

    public abstract DoubleStream zipWith(DoubleStream b, DoubleStream c, DoubleTriFunction<Double> zipFunction);

    public abstract DoubleStream zipWith(DoubleStream b, double valueForNoneA, double valueForNoneB, DoubleBiFunction<Double> zipFunction);

    public abstract DoubleStream zipWith(DoubleStream b, DoubleStream c, double valueForNoneA, double valueForNoneB, double valueForNoneC,
            DoubleTriFunction<Double> zipFunction);

    /**
     * Returns a {@code Stream} consisting of the elements of this stream,
     * boxed to {@code Double}.
     *
     * <p>This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * @return a {@code Stream} consistent of the elements of this stream,
     * each boxed to a {@code Double}
     */
    public abstract Stream<Double> boxed();

    @Override
    public abstract ImmutableIterator<Double> iterator();

    public abstract ImmutableDoubleIterator doubleIterator();

    public static DoubleStream empty() {
        return EMPTY;
    }

    public static DoubleStream of(final double... a) {
        return N.isNullOrEmpty(a) ? empty() : new ArrayDoubleStream(a);
    }

    public static DoubleStream of(final double[] a, final int startIndex, final int endIndex) {
        return N.isNullOrEmpty(a) && (startIndex == 0 && endIndex == 0) ? empty() : new ArrayDoubleStream(a, startIndex, endIndex);
    }

    public static DoubleStream of(final DoubleIterator iterator) {
        return iterator == null ? empty()
                : new IteratorDoubleStream(iterator instanceof ImmutableDoubleIterator ? (ImmutableDoubleIterator) iterator : new ImmutableDoubleIterator() {
                    @Override
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    @Override
                    public double next() {
                        return iterator.next();
                    }
                });
    }

    public static DoubleStream repeat(final double element, final long n) {
        if (n < 0) {
            throw new IllegalArgumentException("'n' can't be negative: " + n);
        } else if (n == 0) {
            return empty();
        }

        return new IteratorDoubleStream(new ImmutableDoubleIterator() {
            private long cnt = 0;

            @Override
            public boolean hasNext() {
                return cnt < n;
            }

            @Override
            public double next() {
                if (cnt >= n) {
                    throw new NoSuchElementException();
                }

                cnt++;
                return element;
            }
        });
    }

    public static DoubleStream random() {
        return iterate(new DoubleSupplier() {
            @Override
            public double getAsDouble() {
                return RAND.nextDouble();
            }
        });
    }

    public static DoubleStream iterate(final Supplier<Boolean> hasNext, final DoubleSupplier next) {
        N.requireNonNull(hasNext);
        N.requireNonNull(next);

        return new IteratorDoubleStream(new ImmutableDoubleIterator() {
            private boolean hasNextVal = false;

            @Override
            public boolean hasNext() {
                if (hasNextVal == false) {
                    hasNextVal = hasNext.get().booleanValue();
                }

                return hasNextVal;
            }

            @Override
            public double next() {
                if (hasNextVal == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                hasNextVal = false;
                return next.getAsDouble();
            }
        });
    }

    public static DoubleStream iterate(final double seed, final Supplier<Boolean> hasNext, final DoubleUnaryOperator f) {
        N.requireNonNull(hasNext);
        N.requireNonNull(f);

        return new IteratorDoubleStream(new ImmutableDoubleIterator() {
            private double t = 0;
            private boolean isFirst = true;
            private boolean hasNextVal = false;

            @Override
            public boolean hasNext() {
                if (hasNextVal == false) {
                    hasNextVal = hasNext.get().booleanValue();
                }

                return hasNextVal;
            }

            @Override
            public double next() {
                if (hasNextVal == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                hasNextVal = false;

                if (isFirst) {
                    isFirst = false;
                    t = seed;
                } else {
                    t = f.applyAsDouble(t);
                }

                return t;
            }
        });
    }

    /**
     * 
     * @param seed
     * @param hasNext test if has next by hasNext.test(seed) for first time and hasNext.test(f.apply(previous)) for remaining.
     * @param f
     * @return
     */
    public static DoubleStream iterate(final double seed, final DoublePredicate hasNext, final DoubleUnaryOperator f) {
        N.requireNonNull(hasNext);
        N.requireNonNull(f);

        return new IteratorDoubleStream(new ImmutableDoubleIterator() {
            private double t = 0;
            private double cur = 0;
            private boolean isFirst = true;
            private boolean noMoreVal = false;
            private boolean hasNextVal = false;

            @Override
            public boolean hasNext() {
                if (hasNextVal == false && noMoreVal == false) {
                    if (isFirst) {
                        isFirst = false;
                        hasNextVal = hasNext.test(cur = seed);
                    } else {
                        hasNextVal = hasNext.test(cur = f.applyAsDouble(t));
                    }

                    if (hasNextVal == false) {
                        noMoreVal = true;
                    }
                }

                return hasNextVal;
            }

            @Override
            public double next() {
                if (hasNextVal == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                t = cur;
                hasNextVal = false;
                return t;
            }
        });
    }

    public static DoubleStream iterate(final double seed, final DoubleUnaryOperator f) {
        N.requireNonNull(f);

        return new IteratorDoubleStream(new ImmutableDoubleIterator() {
            private double t = 0;
            private boolean isFirst = true;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public double next() {
                if (isFirst) {
                    isFirst = false;
                    t = seed;
                } else {
                    t = f.applyAsDouble(t);
                }

                return t;
            }
        });
    }

    public static DoubleStream iterate(final DoubleSupplier s) {
        N.requireNonNull(s);

        return new IteratorDoubleStream(new ImmutableDoubleIterator() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public double next() {
                return s.getAsDouble();
            }
        });
    }

    public static DoubleStream concat(final double[]... a) {
        return N.isNullOrEmpty(a) ? empty() : new IteratorDoubleStream(new ImmutableDoubleIterator() {
            private final Iterator<double[]> iter = N.asList(a).iterator();
            private ImmutableDoubleIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && iter.hasNext()) {
                    cur = new ImmutableDoubleIterator() {
                        private final double[] cur = iter.next();
                        private int cursor = 0;

                        @Override
                        public boolean hasNext() {
                            return cursor < cur.length;
                        }

                        @Override
                        public double next() {
                            if (cursor >= cur.length) {
                                throw new NoSuchElementException();
                            }

                            return cur[cursor++];
                        }
                    };
                }
                return cur != null && cur.hasNext();
            }

            @Override
            public double next() {
                if ((cur == null || cur.hasNext() == false) && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return cur.next();
            }
        });
    }

    public static DoubleStream concat(final DoubleIterator... a) {
        return N.isNullOrEmpty(a) ? empty() : new IteratorDoubleStream(new ImmutableDoubleIterator() {
            private final Iterator<? extends DoubleIterator> iter = N.asList(a).iterator();
            private DoubleIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public double next() {
                if ((cur == null || cur.hasNext() == false) && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return cur.next();
            }
        });
    }

    public static DoubleStream concat(final DoubleStream... a) {
        return N.isNullOrEmpty(a) ? empty() : concat(N.asList(a));
    }

    public static DoubleStream concat(final Collection<? extends DoubleStream> c) {
        return N.isNullOrEmpty(c) ? empty() : new IteratorDoubleStream(new ImmutableDoubleIterator() {
            private final Iterator<? extends DoubleStream> iter = c.iterator();
            private ImmutableDoubleIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && iter.hasNext()) {
                    cur = iter.next().doubleIterator();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public double next() {
                if ((cur == null || cur.hasNext() == false) && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return cur.next();
            }
        }).onClose(new Runnable() {
            @Override
            public void run() {
                RuntimeException runtimeException = null;

                for (DoubleStream stream : c) {
                    try {
                        stream.close();
                    } catch (Throwable throwable) {
                        if (runtimeException == null) {
                            runtimeException = N.toRuntimeException(throwable);
                        } else {
                            runtimeException.addSuppressed(throwable);
                        }
                    }
                }

                if (runtimeException != null) {
                    throw runtimeException;
                }
            }
        });
    }

    /**
     * Zip together the "a" and "b" arrays until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static DoubleStream zip(final double[] a, final double[] b, final DoubleBiFunction<Double> zipFunction) {
        return Stream.zip(a, b, zipFunction).mapToDouble(ToDoubleFunction.UNBOX);
    }

    /**
     * Zip together the "a", "b" and "c" arrays until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static DoubleStream zip(final double[] a, final double[] b, final double[] c, final DoubleTriFunction<Double> zipFunction) {
        return Stream.zip(a, b, c, zipFunction).mapToDouble(ToDoubleFunction.UNBOX);
    }

    /**
     * Zip together the "a" and "b" iterators until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static DoubleStream zip(final DoubleIterator a, final DoubleIterator b, final DoubleBiFunction<Double> zipFunction) {
        return Stream.zip(a, b, zipFunction).mapToDouble(ToDoubleFunction.UNBOX);
    }

    /**
     * Zip together the "a", "b" and "c" iterators until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static DoubleStream zip(final DoubleIterator a, final DoubleIterator b, final DoubleIterator c, final DoubleTriFunction<Double> zipFunction) {
        return Stream.zip(a, b, c, zipFunction).mapToDouble(ToDoubleFunction.UNBOX);
    }

    /**
     * Zip together the "a" and "b" streams until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static DoubleStream zip(final DoubleStream a, final DoubleStream b, final DoubleBiFunction<Double> zipFunction) {
        return Stream.zip(a, b, zipFunction).mapToDouble(ToDoubleFunction.UNBOX);
    }

    /**
     * Zip together the "a", "b" and "c" streams until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static DoubleStream zip(final DoubleStream a, final DoubleStream b, final DoubleStream c, final DoubleTriFunction<Double> zipFunction) {
        return Stream.zip(a, b, c, zipFunction).mapToDouble(ToDoubleFunction.UNBOX);
    }

    /**
     * Zip together the iterators until one of them runs out of values.
     * Each array of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param c
     * @param zipFunction
     * @return
     */
    public static DoubleStream zip(final Collection<? extends DoubleStream> c, final DoubleNFunction<Double> zipFunction) {
        return Stream.zip(c, zipFunction).mapToDouble(ToDoubleFunction.UNBOX);
    }

    /**
     * Zip together the "a" and "b" iterators until all of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param valueForNoneA value to fill if "a" runs out of values first.
     * @param valueForNoneB value to fill if "b" runs out of values first.
     * @param zipFunction
     * @return
     */
    public static DoubleStream zip(final double[] a, final double[] b, final double valueForNoneA, final double valueForNoneB,
            final DoubleBiFunction<Double> zipFunction) {
        return Stream.zip(a, b, valueForNoneA, valueForNoneB, zipFunction).mapToDouble(ToDoubleFunction.UNBOX);
    }

    /**
     * Zip together the "a", "b" and "c" iterators until all of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA value to fill if "a" runs out of values.
     * @param valueForNoneB value to fill if "b" runs out of values.
     * @param valueForNoneC value to fill if "c" runs out of values.
     * @param zipFunction
     * @return
     */
    public static DoubleStream zip(final double[] a, final double[] b, final double[] c, final double valueForNoneA, final double valueForNoneB,
            final double valueForNoneC, final DoubleTriFunction<Double> zipFunction) {
        return Stream.zip(a, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction).mapToDouble(ToDoubleFunction.UNBOX);
    }

    /**
     * Zip together the "a" and "b" iterators until all of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param valueForNoneA value to fill if "a" runs out of values first.
     * @param valueForNoneB value to fill if "b" runs out of values first.
     * @param zipFunction
     * @return
     */
    public static DoubleStream zip(final DoubleIterator a, final DoubleIterator b, final double valueForNoneA, final double valueForNoneB,
            final DoubleBiFunction<Double> zipFunction) {
        return Stream.zip(a, b, valueForNoneA, valueForNoneB, zipFunction).mapToDouble(ToDoubleFunction.UNBOX);
    }

    /**
     * Zip together the "a", "b" and "c" iterators until all of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA value to fill if "a" runs out of values.
     * @param valueForNoneB value to fill if "b" runs out of values.
     * @param valueForNoneC value to fill if "c" runs out of values.
     * @param zipFunction
     * @return
     */
    public static DoubleStream zip(final DoubleIterator a, final DoubleIterator b, final DoubleIterator c, final double valueForNoneA,
            final double valueForNoneB, final double valueForNoneC, final DoubleTriFunction<Double> zipFunction) {
        return Stream.zip(a, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction).mapToDouble(ToDoubleFunction.UNBOX);
    }

    /**
     * Zip together the "a" and "b" iterators until all of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param valueForNoneA value to fill if "a" runs out of values first.
     * @param valueForNoneB value to fill if "b" runs out of values first.
     * @param zipFunction
     * @return
     */
    public static DoubleStream zip(final DoubleStream a, final DoubleStream b, final double valueForNoneA, final double valueForNoneB,
            final DoubleBiFunction<Double> zipFunction) {
        return Stream.zip(a, b, valueForNoneA, valueForNoneB, zipFunction).mapToDouble(ToDoubleFunction.UNBOX);
    }

    /**
     * Zip together the "a", "b" and "c" iterators until all of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA value to fill if "a" runs out of values.
     * @param valueForNoneB value to fill if "b" runs out of values.
     * @param valueForNoneC value to fill if "c" runs out of values.
     * @param zipFunction
     * @return
     */
    public static DoubleStream zip(final DoubleStream a, final DoubleStream b, final DoubleStream c, final double valueForNoneA, final double valueForNoneB,
            final double valueForNoneC, final DoubleTriFunction<Double> zipFunction) {
        return Stream.zip(a, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction).mapToDouble(ToDoubleFunction.UNBOX);
    }

    /**
     * Zip together the iterators until all of them runs out of values.
     * Each array of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param c
     * @param valuesForNone value to fill for any iterator runs out of values.
     * @param zipFunction
     * @return
     */
    public static DoubleStream zip(final Collection<? extends DoubleStream> c, final double[] valuesForNone, final DoubleNFunction<Double> zipFunction) {
        return Stream.zip(c, valuesForNone, zipFunction).mapToDouble(ToDoubleFunction.UNBOX);
    }

    /**
     * 
     * @param a
     * @param b
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static DoubleStream merge(final double[] a, final double[] b, final DoubleBiFunction<Nth> nextSelector) {
        if (N.isNullOrEmpty(a)) {
            return of(b);
        } else if (N.isNullOrEmpty(b)) {
            return of(a);
        }

        return new IteratorDoubleStream(new ImmutableDoubleIterator() {
            private final int lenA = a.length;
            private final int lenB = b.length;
            private int cursorA = 0;
            private int cursorB = 0;

            @Override
            public boolean hasNext() {
                return cursorA < lenA || cursorB < lenB;
            }

            @Override
            public double next() {
                if (cursorA < lenA) {
                    if (cursorB < lenB) {
                        if (nextSelector.apply(a[cursorA], b[cursorB]) == Nth.FIRST) {
                            return a[cursorA++];
                        } else {
                            return b[cursorB++];
                        }
                    } else {
                        return a[cursorA++];
                    }
                } else if (cursorB < lenB) {
                    return b[cursorB++];
                } else {
                    throw new NoSuchElementException();
                }
            }
        });
    }

    /**
     * 
     * @param a
     * @param b
     * @param c
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static DoubleStream merge(final double[] a, final double[] b, final double[] c, final DoubleBiFunction<Nth> nextSelector) {
        return merge(merge(a, b, nextSelector).doubleIterator(), DoubleStream.of(c).doubleIterator(), nextSelector);
    }

    /**
     * 
     * @param a
     * @param b
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static DoubleStream merge(final DoubleIterator a, final DoubleIterator b, final DoubleBiFunction<Nth> nextSelector) {
        if (a.hasNext() == false) {
            return of(b);
        } else if (b.hasNext() == false) {
            return of(a);
        }

        return new IteratorDoubleStream(new ImmutableDoubleIterator() {
            private double nextA = 0;
            private double nextB = 0;
            private boolean hasNextA = false;
            private boolean hasNextB = false;

            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext() || hasNextA || hasNextB;
            }

            @Override
            public double next() {
                if (hasNextA) {
                    if (b.hasNext()) {
                        if (nextSelector.apply(nextA, (nextB = b.next())) == Nth.FIRST) {
                            hasNextA = false;
                            hasNextB = true;
                            return nextA;
                        } else {
                            return nextB;
                        }
                    } else {
                        hasNextA = false;
                        return nextA;
                    }
                } else if (hasNextB) {
                    if (a.hasNext()) {
                        if (nextSelector.apply((nextA = a.next()), nextB) == Nth.FIRST) {
                            return nextA;
                        } else {
                            hasNextA = true;
                            hasNextB = false;
                            return nextB;
                        }
                    } else {
                        hasNextB = false;
                        return nextB;
                    }
                } else if (a.hasNext()) {
                    if (b.hasNext()) {
                        if (nextSelector.apply((nextA = a.next()), (nextB = b.next())) == Nth.FIRST) {
                            hasNextB = true;
                            return nextA;
                        } else {
                            hasNextA = true;
                            return nextB;
                        }
                    } else {
                        return a.next();
                    }
                } else if (b.hasNext()) {
                    return b.next();
                } else {
                    throw new NoSuchElementException();
                }
            }
        });
    }

    /**
     * 
     * @param a
     * @param b
     * @param c
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static DoubleStream merge(final DoubleIterator a, final DoubleIterator b, final DoubleIterator c, final DoubleBiFunction<Nth> nextSelector) {
        return merge(merge(a, b, nextSelector).doubleIterator(), c, nextSelector);
    }

    /**
     * 
     * @param a
     * @param b
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static DoubleStream merge(final DoubleStream a, final DoubleStream b, final DoubleBiFunction<Nth> nextSelector) {
        return merge(a.doubleIterator(), b.doubleIterator(), nextSelector).onClose(new Runnable() {
            @Override
            public void run() {
                RuntimeException runtimeException = null;

                for (DoubleStream stream : N.asList(a, b)) {
                    try {
                        stream.close();
                    } catch (Throwable throwable) {
                        if (runtimeException == null) {
                            runtimeException = N.toRuntimeException(throwable);
                        } else {
                            runtimeException.addSuppressed(throwable);
                        }
                    }
                }

                if (runtimeException != null) {
                    throw runtimeException;
                }
            }
        });
    }

    /**
     * 
     * @param a
     * @param b
     * @param c
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static DoubleStream merge(final DoubleStream a, final DoubleStream b, final DoubleStream c, final DoubleBiFunction<Nth> nextSelector) {
        return merge(N.asList(a, b, c), nextSelector);
    }

    /**
     * 
     * @param c
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static DoubleStream merge(final Collection<? extends DoubleStream> c, final DoubleBiFunction<Nth> nextSelector) {
        if (N.isNullOrEmpty(c)) {
            return empty();
        } else if (c.size() == 1) {
            return c.iterator().next();
        } else if (c.size() == 2) {
            final Iterator<? extends DoubleStream> iter = c.iterator();
            return merge(iter.next(), iter.next(), nextSelector);
        }

        final Iterator<? extends DoubleStream> iter = c.iterator();
        DoubleStream result = merge(iter.next().doubleIterator(), iter.next().doubleIterator(), nextSelector);

        while (iter.hasNext()) {
            result = merge(result.doubleIterator(), iter.next().doubleIterator(), nextSelector);
        }

        return result.onClose(new Runnable() {
            @Override
            public void run() {
                RuntimeException runtimeException = null;

                for (DoubleStream stream : c) {
                    try {
                        stream.close();
                    } catch (Throwable throwable) {
                        if (runtimeException == null) {
                            runtimeException = N.toRuntimeException(throwable);
                        } else {
                            runtimeException.addSuppressed(throwable);
                        }
                    }
                }

                if (runtimeException != null) {
                    throw runtimeException;
                }
            }
        });
    }

    /**
     * 
     * @param c
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static DoubleStream parallelMerge(final Collection<? extends DoubleStream> c, final DoubleBiFunction<Nth> nextSelector) {
        return parallelMerge(c, nextSelector, DEFAULT_MAX_THREAD_NUM);
    }

    /**
     * 
     * @param c
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @param maxThreadNum
     * @return
     */
    public static DoubleStream parallelMerge(final Collection<? extends DoubleStream> c, final DoubleBiFunction<Nth> nextSelector, final int maxThreadNum) {
        if (maxThreadNum < 1 || maxThreadNum > MAX_THREAD_NUM_PER_OPERATION) {
            throw new IllegalArgumentException("'maxThreadNum' must not less than 1 or exceeded: " + MAX_THREAD_NUM_PER_OPERATION);
        }

        if (N.isNullOrEmpty(c)) {
            return empty();
        } else if (c.size() == 1) {
            return c.iterator().next();
        } else if (c.size() == 2) {
            final Iterator<? extends DoubleStream> iter = c.iterator();
            return merge(iter.next(), iter.next(), nextSelector);
        } else if (maxThreadNum <= 1) {
            return merge(c, nextSelector);
        }

        final Queue<DoubleIterator> queue = N.newLinkedList();

        for (DoubleStream e : c) {
            queue.add(e.doubleIterator());
        }

        final Holder<Throwable> eHolder = new Holder<>();
        final MutableInt cnt = MutableInt.of(c.size());
        final List<CompletableFuture<Void>> futureList = new ArrayList<>(c.size() - 1);

        for (int i = 0, n = N.min(maxThreadNum, c.size() / 2 + 1); i < n; i++) {
            futureList.add(asyncExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    DoubleIterator a = null;
                    DoubleIterator b = null;
                    DoubleIterator c = null;

                    try {
                        while (eHolder.value() == null) {
                            synchronized (queue) {
                                if (cnt.intValue() > 2 && queue.size() > 1) {
                                    a = queue.poll();
                                    b = queue.poll();

                                    cnt.decrement();
                                } else {
                                    break;
                                }
                            }

                            c = ImmutableDoubleIterator.of(merge(a, b, nextSelector).toArray());

                            synchronized (queue) {
                                queue.offer(c);
                            }
                        }
                    } catch (Throwable e) {
                        setError(eHolder, e);
                    }
                }
            }));
        }

        if (eHolder.value() != null) {
            throw N.toRuntimeException(eHolder.value());
        }

        try {
            for (CompletableFuture<Void> future : futureList) {
                future.get();
            }
        } catch (Exception e) {
            throw N.toRuntimeException(e);
        }

        // Should never happen.
        if (queue.size() != 2) {
            throw new AbacusException("Unknown error happened.");
        }

        return merge(queue.poll(), queue.poll(), nextSelector).onClose(new Runnable() {
            @Override
            public void run() {
                RuntimeException runtimeException = null;

                for (DoubleStream stream : c) {
                    try {
                        stream.close();
                    } catch (Throwable throwable) {
                        if (runtimeException == null) {
                            runtimeException = N.toRuntimeException(throwable);
                        } else {
                            runtimeException.addSuppressed(throwable);
                        }
                    }
                }

                if (runtimeException != null) {
                    throw runtimeException;
                }
            }
        });
    }
}
