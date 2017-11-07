package com.landawn.abacus.util;

import java.util.NoSuchElementException;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.util.function.Consumer;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.stream.Stream;

@Beta
public abstract class Any<T> {

    final T value;
    final boolean isPresent;

    /**
     * Constructs an empty instance.
     *
     * @implNote Generally only one empty instance, {@link Any#EMPTY},
     * should exist per VM.
     */
    Any() {
        this.value = null;
        isPresent = false;
    }

    /**
     * Constructs an instance with the value present.
     *
     * @param value the nullable value to be present
     */
    Any(T value) {
        this.value = value;
        isPresent = true;
    }

    /**
     * Returns an empty {@code Any} instance.  No value is present for this
     * Optional.
     *
     * @apiNote Though it may be tempting to do so, avoid testing if an object
     * is empty by comparing with {@code ==} against instances returned by
     * {@code Any.empty()}. There is no guarantee that it is a singleton.
     * Instead, use {@link #isPresent()}.
     *
     * @param <T> Type of the non-existent value
     * @return an empty {@code Any}
     */
    public static <T> Any<T> empty() {
        return Nullable.empty();
    }

    /**
     * Returns an {@code Any} with the specified present nullable value.
     *
     * @param value the value to be present, which could be null
     * @return an {@code Any} with the value present
     */
    public static <T> Any<T> of(T value) {
        return Nullable.of(value);
    }

    /**
     * Returns a {@code Any} with the value returned by {@code action} or an empty {@code Any} if exception happens.
     * 
     * @param action
     * @return
     */
    public static <R> Any<R> tryOrEmpty(final Try.Callable<R, ? extends Exception> action) {
        return Nullable.tryOrEmpty(action);
    }

    //    public static <T> Any<T> ifOrEmpty(boolean b, final T val) {
    //        return b ? Any.of(val) : Any.<T> empty();
    //    }

    //    public static <T> Any<T> from(Optional<T> optional) {
    //        return optional.isPresent() ? new Any<T>(optional.get()) : (Any<T>) empty();
    //    }

    /**
     * If a value is present in this {@code Any}, returns the value,
     * otherwise throws {@code NoSuchElementException}.
     *
     * @return the nullable value held by this {@code Any}
     * @throws NoSuchElementException if there is no value present
     *
     * @see Any#isPresent()
     */
    public T get() {
        if (isPresent()) {
            return value;
        } else {
            throw new NoSuchElementException("No value present");
        }
    }

    /**
     * Return {@code true} if there is a value present, otherwise {@code false}.
     *
     * @return {@code true} if there is a value present, otherwise {@code false}
     */
    public boolean isPresent() {
        return isPresent;
    }

    /**
     * Return {@code true} if there is a value not null, otherwise {@code false}.
     *
     * @return {@code true} if there is a value not null, otherwise {@code false}
     */
    public boolean isNotNull() {
        return value != null;
    }

    /**
     * If a value is present, invoke the specified consumer with the value,
     * otherwise do nothing.
     *
     * @param consumer block to be executed if a value is present
     * @throws NullPointerException if value is present and {@code consumer} is
     * null
     */
    public void ifPresent(Consumer<? super T> consumer) {
        if (isPresent()) {
            consumer.accept(value);
        }
    }

    /**
    * If a value is present, performs the given action with the value, otherwise performs the given empty-based action.
    *
    * @param action
    * @param emptyAction
    */
    public void ifPresentOrElse(Consumer<? super T> action, Runnable emptyAction) {
        if (isPresent()) {
            action.accept(value);
        } else {
            emptyAction.run();
        }
    }

    /**
     * If a value is not null, invoke the specified consumer with the value,
     * otherwise do nothing.
     *
     * @param consumer block to be executed if a value is not null.
     * @throws NullPointerException if value is present and {@code consumer} is
     * null
     */
    public void ifNotNull(Consumer<? super T> consumer) {
        if (isNotNull()) {
            consumer.accept(value);
        }
    }

    /**
    * If a value is not null, performs the given action with the value, otherwise performs the given empty-based action.
    *
    * @param action
    * @param emptyAction
    */
    public void ifNotNullOrElse(Consumer<? super T> action, Runnable emptyAction) {
        if (isNotNull()) {
            action.accept(value);
        } else {
            emptyAction.run();
        }
    }

    /**
     * If a value is present, and the value matches the given predicate,
     * return an {@code Any} describing the value, otherwise return an
     * empty {@code Any}.
     *
     * @param predicate a predicate to apply to the value, if present
     * @return an {@code Any} describing the value of this {@code Any}
     * if a value is present and the value matches the given predicate,
     * otherwise an empty {@code Any}
     * @throws NullPointerException if the predicate is null
     */
    public abstract Any<T> filter(Predicate<? super T> predicate);

    /**
     * If a value is present, apply the provided mapping function to it,
     *
     * @apiNote This method supports post-processing on optional values, without
     * the need to explicitly check for a return status.  For example, the
     * following code traverses a stream of file names, selects one that has
     * not yet been processed, and then opens that file, returning an
     * {@code Optional<FileInputStream>}:
     *
     * <pre>{@code
     *     Any<FileInputStream> fis =
     *         names.stream().filter(name -> !isProcessedYet(name))
     *                       .findFirst()
     *                       .map(name -> new FileInputStream(name));
     * }</pre>
     *
     * Here, {@code findFirst} returns an {@code Any<String>}, and then
     * {@code map} returns an {@code Any<FileInputStream>} for the desired
     * file if one exists.
     *
     * @param <U> The type of the result of the mapping function
     * @param mapper a mapping function to apply to the value, if present
     * @return an {@code Any} describing the result of applying a mapping
     * function to the value of this {@code Any}, if a value is present,
     * otherwise an empty {@code Any}
     * @throws NullPointerException if the mapping function is null
     */
    public abstract <U> Any<U> map(Function<? super T, ? extends U> mapper);

    /**
     * If a value is present, apply the provided {@code Any}-bearing
     * mapping function to it, return that result, otherwise return an empty
     * {@code Any}.  This method is similar to {@link #map(Function)},
     * but the provided mapper is one whose result is already an {@code Any},
     * and if invoked, {@code flatMap} does not wrap it with an additional
     * {@code Any}.
     *
     * @param <U> The type parameter to the {@code Any} returned by
     * @param mapper a mapping function to apply to the value, if present
     *           the mapping function
     * @return the result of applying an {@code Any}-bearing mapping
     * function to the value of this {@code Any}, if a value is present,
     * otherwise an empty {@code Any}
     * @throws NullPointerException if the mapping function is null or returns
     * a null result
     */
    public abstract <U> Any<U> flatMap(Function<? super T, ? extends Any<U>> mapper);

    /**
     * If a value is not null, and the value matches the given predicate,
     * return an {@code Any} describing the value, otherwise return an
     * empty {@code Any}.
     *
     * @param predicate a predicate to apply to the value, if present
     * @return an {@code Any} describing the value of this {@code Any}
     * if a value is present and the value matches the given predicate,
     * otherwise an empty {@code Any}
     * @throws NullPointerException if the predicate is null
     */
    public abstract Any<T> filterIfNotNull(Predicate<? super T> predicate);

    /**
     * If a value is not null, apply the provided mapping function to it,
     *
     * @apiNote This method supports post-processing on optional values, without
     * the need to explicitly check for a return status.  For example, the
     * following code traverses a stream of file names, selects one that has
     * not yet been processed, and then opens that file, returning an
     * {@code Optional<FileInputStream>}:
     *
     * <pre>{@code
     *     Any<FileInputStream> fis =
     *         names.stream().filter(name -> !isProcessedYet(name))
     *                       .findFirst()
     *                       .map(name -> new FileInputStream(name));
     * }</pre>
     *
     * Here, {@code findFirst} returns an {@code Any<String>}, and then
     * {@code map} returns an {@code Optional<FileInputStream>} for the desired
     * file if one exists.
     *
     * @param <U> The type of the result of the mapping function
     * @param mapper a mapping function to apply to the value, if not null
     * @return an {@code Any} describing the result of applying a mapping
     * function to the value of this {@code Any}, if a value is not null,
     * otherwise an empty {@code Any}
     * @throws NullPointerException if the mapping function is null
     */
    public abstract <U> Any<U> mapIfNotNull(Function<? super T, ? extends U> mapper);

    /**
     * If a value is not null, apply the provided {@code Any}-bearing
     * mapping function to it, return that result, otherwise return an empty
     * {@code Any}.  This method is similar to {@link #map(Function)},
     * but the provided mapper is one whose result is already an {@code Any},
     * and if invoked, {@code flatMap} does not wrap it with an additional
     * {@code Any}.
     *
     * @param <U> The type parameter to the {@code Any} returned by
     * @param mapper a mapping function to apply to the value, if not null
     *           the mapping function
     * @return the result of applying an {@code Any}-bearing mapping
     * function to the value of this {@code Any}, if a value is not null,
     * otherwise an empty {@code Any}
     * @throws NullPointerException if the mapping function is null or returns
     * a null result
     */
    public abstract <U> Any<U> flatMapIfNotNull(Function<? super T, ? extends Any<U>> mapper);

    /**
     * 
     * @return an empty stream if it's not present.
     */
    public Stream<T> stream() {
        return isPresent() ? Stream.of(value) : Stream.<T> empty();
    }

    /**
     * 
     * @return an empty stream if it's null.
     */
    public Stream<T> streamIfNotNull() {
        return isNotNull() ? Stream.of(value) : Stream.<T> empty();
    }

    /**
     * Return the value if present, otherwise return {@code other}.
     *
     * @param other the value to be returned if there is no value present, may be null
     * @return the value, if present, otherwise {@code other}
     */
    public T orElse(T other) {
        return isPresent() ? value : other;
    }

    /**
     * Return the value if present, otherwise invoke {@code other} and return the result of that invocation.
     *
     * @param other a {@code Supplier} whose result is returned if no value is present
     * @return the value if present otherwise the result of {@code other.get()}
     * @throws NullPointerException if value is not present and {@code other} is
     * null
     */
    public T orElseGet(Supplier<? extends T> other) {
        return isPresent() ? value : other.get();
    }

    /**
     * Return the contained value, if present, otherwise throw an exception to be created by the provided supplier.
     *
     * @apiNote A method reference to the exception constructor with an empty
     * argument list can be used as the supplier. For example,
     * {@code IllegalStateException::new}
     *
     * @param <X> Type of the exception to be thrown
     * @param exceptionSupplier The supplier which will return the exception to
     * be thrown
     * @return the present value
     * @throws X if there is no value present
     * @throws NullPointerException if no value is present and
     * {@code exceptionSupplier} is null
     */
    public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        if (isPresent()) {
            return value;
        } else {
            throw exceptionSupplier.get();
        }
    }

    /**
     * Return the value is not null, otherwise return {@code other}.
     *
     * @param other the value to be returned if not present or null, may be null
     * @return the value, if not present or null, otherwise {@code other}
     */
    public T orIfNull(T other) {
        return isNotNull() ? value : other;
    }

    /**
     * Return the value is not null, otherwise invoke {@code other} and return the result of that invocation.
     *
     * @param other a {@code Supplier} whose result is returned if not present or null
     * @return the value if not present or null otherwise the result of {@code other.get()}
     * @throws NullPointerException if value is not present and {@code other} is null
     */
    public T orGetIfNull(Supplier<? extends T> other) {
        return isNotNull() ? value : other.get();
    }

    /**
     * Return the value is not null, otherwise throw an exception to be created by the provided supplier.
     *
     * @apiNote A method reference to the exception constructor with an empty
     * argument list can be used as the supplier. For example,
     * {@code IllegalStateException::new}
     *
     * @param <X> Type of the exception to be thrown
     * @param exceptionSupplier The supplier which will return the exception to be thrown
     * @return the present value
     * @throws X if not present or null
     * @throws NullPointerException if not present or null and
     * {@code exceptionSupplier} is null
     */
    public <X extends Throwable> T orThrowIfNull(Supplier<? extends X> exceptionSupplier) throws X {
        if (isNotNull()) {
            return value;
        } else {
            throw exceptionSupplier.get();
        }
    }

    /**
     * 
     * @return <code>Optional.empty()</code> if the value is not present or {@code null}.
     */
    public Optional<T> toOptional() {
        return value == null ? Optional.<T> empty() : Optional.of(value);
    }

    /**
     * 
     * @return <code>java.util.Optional.empty()</code> if the value is not present or {@code null}.
     */
    public java.util.Optional<T> toJdkOptional() {
        return value == null ? java.util.Optional.<T> empty() : java.util.Optional.of(value);
    }
}
