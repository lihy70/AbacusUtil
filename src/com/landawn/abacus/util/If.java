/*
 * Copyright (C) 2017 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.landawn.abacus.util;

import java.util.Collection;
import java.util.Map;

import com.landawn.abacus.annotation.Beta;

/**
 * This class is mainly designed for functional programming. 
 * Generally the traditional "{@code if-else}" or ternary operator: "{@code ? : }" is preferred over this class.
 * 
 * @author HaiYang Li
 *
 */
@Beta
public final class If {
    private static final If TRUE = new If(true);
    private static final If FALSE = new If(false);

    private final boolean b;

    If(boolean b) {
        this.b = b;
    }

    public static If of(boolean b) {
        return b ? TRUE : FALSE;
    }

    public static If not(boolean b) {
        return b ? FALSE : TRUE;
    }

    public static If isNullOrEmpty(final CharSequence s) {
        return of(N.isNullOrEmpty(s));
    }

    public static If isNullOrEmpty(final boolean[] a) {
        return of(N.isNullOrEmpty(a));
    }

    public static If isNullOrEmpty(final char[] a) {
        return of(N.isNullOrEmpty(a));
    }

    public static If isNullOrEmpty(final byte[] a) {
        return of(N.isNullOrEmpty(a));
    }

    public static If isNullOrEmpty(final short[] a) {
        return of(N.isNullOrEmpty(a));
    }

    public static If isNullOrEmpty(final int[] a) {
        return of(N.isNullOrEmpty(a));
    }

    public static If isNullOrEmpty(final long[] a) {
        return of(N.isNullOrEmpty(a));
    }

    public static If isNullOrEmpty(final float[] a) {
        return of(N.isNullOrEmpty(a));
    }

    public static If isNullOrEmpty(final double[] a) {
        return of(N.isNullOrEmpty(a));
    }

    public static If isNullOrEmpty(final Object[] a) {
        return of(N.isNullOrEmpty(a));
    }

    public static If isNullOrEmpty(final Collection<?> c) {
        return of(N.isNullOrEmpty(c));
    }

    public static If isNullOrEmpty(final Map<?, ?> m) {
        return of(N.isNullOrEmpty(m));
    }

    @SuppressWarnings("rawtypes")
    public static If isNullOrEmpty(final PrimitiveList list) {
        return of(N.isNullOrEmpty(list));
    }

    public static If isNullOrEmpty(final Multiset<?> s) {
        return of(N.isNullOrEmpty(s));
    }

    public static If isNullOrEmpty(final LongMultiset<?> s) {
        return of(N.isNullOrEmpty(s));
    }

    public static If isNullOrEmpty(final Multimap<?, ?, ?> m) {
        return of(N.isNullOrEmpty(m));
    }

    // DON'T change 'OrEmptyOrBlank' to 'OrBlank' because of the occurring order in the auto-completed context menu. 
    public static If isNullOrEmptyOrBlank(final CharSequence s) {
        return of(N.isNullOrEmptyOrBlank(s));
    }

    public static If notNullOrEmpty(final CharSequence s) {
        return of(N.notNullOrEmpty(s));
    }

    public static If notNullOrEmpty(final boolean[] a) {
        return of(N.notNullOrEmpty(a));
    }

    public static If notNullOrEmpty(final char[] a) {
        return of(N.notNullOrEmpty(a));
    }

    public static If notNullOrEmpty(final byte[] a) {
        return of(N.notNullOrEmpty(a));
    }

    public static If notNullOrEmpty(final short[] a) {
        return of(N.notNullOrEmpty(a));
    }

    public static If notNullOrEmpty(final int[] a) {
        return of(N.notNullOrEmpty(a));
    }

    public static If notNullOrEmpty(final long[] a) {
        return of(N.notNullOrEmpty(a));
    }

    public static If notNullOrEmpty(final float[] a) {
        return of(N.notNullOrEmpty(a));
    }

    public static If notNullOrEmpty(final double[] a) {
        return of(N.notNullOrEmpty(a));
    }

    public static If notNullOrEmpty(final Object[] a) {
        return of(N.notNullOrEmpty(a));
    }

    public static If notNullOrEmpty(final Collection<?> c) {
        return of(N.notNullOrEmpty(c));
    }

    public static If notNullOrEmpty(final Map<?, ?> m) {
        return of(N.notNullOrEmpty(m));
    }

    @SuppressWarnings("rawtypes")
    public static If notNullOrEmpty(final PrimitiveList list) {
        return of(N.notNullOrEmpty(list));
    }

    public static If notNullOrEmpty(final Multiset<?> s) {
        return of(N.notNullOrEmpty(s));
    }

    public static If notNullOrEmpty(final LongMultiset<?> s) {
        return of(N.notNullOrEmpty(s));
    }

    public static If notNullOrEmpty(final Multimap<?, ?, ?> m) {
        return of(N.notNullOrEmpty(m));
    }

    // DON'T change 'OrEmptyOrBlank' to 'OrBlank' because of the occurring order in the auto-completed context menu. 
    public static If notNullOrEmptyOrBlank(final CharSequence s) {
        return of(N.notNullOrEmptyOrBlank(s));
    }

    //    public <E extends Exception> void thenRun(final Try.Runnable<E> cmd) throws E {
    //        if (b) {
    //            cmd.run();
    //        }
    //    }
    //
    //    public <T, E extends Exception> void thenRun(final T seed, final Try.Consumer<? super T, E> action) throws E {
    //        if (b) {
    //            action.accept(seed);
    //        }
    //    }
    //
    //    public <T, E extends Exception> Nullable<T> thenCall(final Try.Callable<? extends T, E> callable) throws E {
    //        return b ? Nullable.of(callable.call()) : Nullable.<T> empty();
    //    }
    //
    //    public <T, R, E extends Exception> Nullable<R> thenCall(final T seed, final Try.Function<? super T, R, E> func) throws E {
    //        return b ? Nullable.of(func.apply(seed)) : Nullable.<R> empty();
    //    }

    public Or thenDoNothing() {
        return Or.of(b);
    }

    public <E extends Exception> Or then(final Try.Runnable<E> cmd) throws E {
        N.requireNonNull(cmd);

        if (b) {
            cmd.run();
        }

        return Or.of(b);
    }

    public <U, E extends Exception> Or then(final U seed, final Try.Consumer<? super U, E> action) throws E {
        N.requireNonNull(action);

        if (b) {
            action.accept(seed);
        }

        return Or.of(b);
    }

    //    public <T, E extends Exception> Nullable<T> then(final Try.Callable<T, E> callable) throws E {
    //        return b ? Nullable.of(callable.call()) : Nullable.<T> empty();
    //    }
    //
    //    public <T, R, E extends Exception> Nullable<R> then(final T seed, final Try.Function<? super T, R, E> func) throws E {
    //        return b ? Nullable.of(func.apply(seed)) : Nullable.<R> empty();
    //    }

    public static final class Or {
        private static final Or TRUE = new Or(true);
        private static final Or FALSE = new Or(false);

        private final boolean b;

        Or(final boolean b) {
            this.b = b;
        }

        static Or of(boolean b) {
            return b ? TRUE : FALSE;
        }

        void orElseDoNothing() {
            // Do nothing.
        }

        public <E extends Exception> void orElse(final Try.Runnable<E> cmd) throws E {
            N.requireNonNull(cmd);

            if (!b) {
                cmd.run();
            }
        }

        public <U, E extends Exception> void orElse(final U seed, final Try.Consumer<? super U, E> action) throws E {
            N.requireNonNull(action);

            if (!b) {
                action.accept(seed);
            }
        }
    }

    /**
     * This class is mainly designed for functional programming. 
     * Generally the traditional "{@code if-else}" or ternary operator: "{@code ? : }" is preferred over this class.
     * 
     * @author HaiYang Li
     *
     */
    @Beta
    public static final class IF {
        private static final IF TRUE = new IF(true);
        private static final IF FALSE = new IF(false);

        @SuppressWarnings("rawtypes")
        private static final Or FALSE_OR = new FalseOr();

        private final boolean b;

        IF(boolean b) {
            this.b = b;
        }

        public static IF of(boolean b) {
            return b ? TRUE : FALSE;
        }

        public static IF not(boolean b) {
            return b ? FALSE : TRUE;
        }

        public static IF isNullOrEmpty(final CharSequence s) {
            return of(N.isNullOrEmpty(s));
        }

        public static IF isNullOrEmpty(final boolean[] a) {
            return of(N.isNullOrEmpty(a));
        }

        public static IF isNullOrEmpty(final char[] a) {
            return of(N.isNullOrEmpty(a));
        }

        public static IF isNullOrEmpty(final byte[] a) {
            return of(N.isNullOrEmpty(a));
        }

        public static IF isNullOrEmpty(final short[] a) {
            return of(N.isNullOrEmpty(a));
        }

        public static IF isNullOrEmpty(final int[] a) {
            return of(N.isNullOrEmpty(a));
        }

        public static IF isNullOrEmpty(final long[] a) {
            return of(N.isNullOrEmpty(a));
        }

        public static IF isNullOrEmpty(final float[] a) {
            return of(N.isNullOrEmpty(a));
        }

        public static IF isNullOrEmpty(final double[] a) {
            return of(N.isNullOrEmpty(a));
        }

        public static IF isNullOrEmpty(final Object[] a) {
            return of(N.isNullOrEmpty(a));
        }

        public static IF isNullOrEmpty(final Collection<?> c) {
            return of(N.isNullOrEmpty(c));
        }

        public static IF isNullOrEmpty(final Map<?, ?> m) {
            return of(N.isNullOrEmpty(m));
        }

        @SuppressWarnings("rawtypes")
        public static IF isNullOrEmpty(final PrimitiveList list) {
            return of(N.isNullOrEmpty(list));
        }

        public static IF isNullOrEmpty(final Multiset<?> s) {
            return of(N.isNullOrEmpty(s));
        }

        public static IF isNullOrEmpty(final LongMultiset<?> s) {
            return of(N.isNullOrEmpty(s));
        }

        public static IF isNullOrEmpty(final Multimap<?, ?, ?> m) {
            return of(N.isNullOrEmpty(m));
        }

        // DON'T change 'OrEmptyOrBlank' to 'OrBlank' because of the occurring order in the auto-completed context menu. 
        public static IF isNullOrEmptyOrBlank(final CharSequence s) {
            return of(N.isNullOrEmptyOrBlank(s));
        }

        public static IF notNullOrEmpty(final CharSequence s) {
            return of(N.notNullOrEmpty(s));
        }

        public static IF notNullOrEmpty(final boolean[] a) {
            return of(N.notNullOrEmpty(a));
        }

        public static IF notNullOrEmpty(final char[] a) {
            return of(N.notNullOrEmpty(a));
        }

        public static IF notNullOrEmpty(final byte[] a) {
            return of(N.notNullOrEmpty(a));
        }

        public static IF notNullOrEmpty(final short[] a) {
            return of(N.notNullOrEmpty(a));
        }

        public static IF notNullOrEmpty(final int[] a) {
            return of(N.notNullOrEmpty(a));
        }

        public static IF notNullOrEmpty(final long[] a) {
            return of(N.notNullOrEmpty(a));
        }

        public static IF notNullOrEmpty(final float[] a) {
            return of(N.notNullOrEmpty(a));
        }

        public static IF notNullOrEmpty(final double[] a) {
            return of(N.notNullOrEmpty(a));
        }

        public static IF notNullOrEmpty(final Object[] a) {
            return of(N.notNullOrEmpty(a));
        }

        public static IF notNullOrEmpty(final Collection<?> c) {
            return of(N.notNullOrEmpty(c));
        }

        public static IF notNullOrEmpty(final Map<?, ?> m) {
            return of(N.notNullOrEmpty(m));
        }

        @SuppressWarnings("rawtypes")
        public static IF notNullOrEmpty(final PrimitiveList list) {
            return of(N.notNullOrEmpty(list));
        }

        public static IF notNullOrEmpty(final Multiset<?> s) {
            return of(N.notNullOrEmpty(s));
        }

        public static IF notNullOrEmpty(final LongMultiset<?> s) {
            return of(N.notNullOrEmpty(s));
        }

        public static IF notNullOrEmpty(final Multimap<?, ?, ?> m) {
            return of(N.notNullOrEmpty(m));
        }

        // DON'T change 'OrEmptyOrBlank' to 'OrBlank' because of the occurring order in the auto-completed context menu. 
        public static IF notNullOrEmptyOrBlank(final CharSequence s) {
            return of(N.notNullOrEmptyOrBlank(s));
        }

        public <T, E extends Exception> Nullable<T> thenGet(Try.Supplier<T, E> supplier) throws E {
            return b ? Nullable.of(supplier.get()) : Nullable.<T> empty();
        }

        public <T, U, E extends Exception> Nullable<T> thenApply(final U seed, final Try.Function<? super U, T, E> func) throws E {
            return b ? Nullable.of(func.apply(seed)) : Nullable.<T> empty();
        }

        public <T, E extends Exception> Or<T> then(final Try.Callable<T, E> callable) throws E {
            N.requireNonNull(callable);

            return b ? new TrueOr<>(Nullable.of(callable.call())) : FALSE_OR;
        }

        public <T, U, E extends Exception> Or<T> then(final U seed, final Try.Function<? super U, T, E> func) throws E {
            N.requireNonNull(func);

            return b ? new TrueOr<>(Nullable.of(func.apply(seed))) : FALSE_OR;
        }

        public static abstract class Or<T> {
            Or() {
            }

            public abstract <E extends Exception> Nullable<T> orElse(final Try.Callable<T, E> callable) throws E;

            public abstract <U, E extends Exception> Nullable<T> orElse(final U seed, final Try.Function<? super U, T, E> func) throws E;
        }

        static final class TrueOr<T> extends Or<T> {
            private final Nullable<T> result;

            TrueOr(final Nullable<T> result) {
                this.result = result;
            }

            @Override
            public <E extends Exception> Nullable<T> orElse(final Try.Callable<T, E> callable) throws E {
                N.requireNonNull(callable);

                return result;
            }

            @Override
            public <U, E extends Exception> Nullable<T> orElse(final U seed, final Try.Function<? super U, T, E> func) throws E {
                N.requireNonNull(func);

                return result;
            }
        }

        static final class FalseOr<T> extends Or<T> {
            FalseOr() {
            }

            @Override
            public <E extends Exception> Nullable<T> orElse(final Try.Callable<T, E> callable) throws E {
                return Nullable.of(callable.call());
            }

            @Override
            public <U, E extends Exception> Nullable<T> orElse(final U seed, final Try.Function<? super U, T, E> func) throws E {
                return Nullable.of(func.apply(seed));
            }
        }
    }
}
