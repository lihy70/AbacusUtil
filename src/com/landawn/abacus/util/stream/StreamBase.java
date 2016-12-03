/*
 * Copyright (C) 2016 HaiYang Li
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

package com.landawn.abacus.util.stream;

import java.lang.reflect.Field;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.util.AsyncExecutor;
import com.landawn.abacus.util.BiMap;
import com.landawn.abacus.util.BooleanList;
import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.ByteList;
import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.CharList;
import com.landawn.abacus.util.CompletableFuture;
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.DoubleList;
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.FloatList;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.LongList;
import com.landawn.abacus.util.LongMultiset;
import com.landawn.abacus.util.Multimap;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.ObjectList;
import com.landawn.abacus.util.Sheet;
import com.landawn.abacus.util.ShortIterator;
import com.landawn.abacus.util.ShortList;
import com.landawn.abacus.util.Try;
import com.landawn.abacus.util.Wrapper;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BinaryOperator;

/**
 * This class is a sequential, stateful and immutable stream implementation.
 *
 * @param <T>
 * @since 0.8
 * 
 * @author Haiyang Li
 */
abstract class StreamBase<T, S extends StreamBase<T, S>> implements BaseStream<T, S> {
    static final Logger logger = LoggerFactory.getLogger(StreamBase.class);

    static final Object NONE = new Object();

    static final int CORE_THREAD_POOL_SIZE = 16;
    static final AsyncExecutor asyncExecutor;

    static {
        final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(CORE_THREAD_POOL_SIZE, MAX_THREAD_POOL_SIZE, 300L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(1));

        asyncExecutor = new AsyncExecutor(threadPoolExecutor) {
            @Override
            public CompletableFuture<Void> execute(final Runnable command) {
                //    if (threadPoolExecutor.getActiveCount() >= MAX_THREAD_POOL_SIZE) {
                //        throw new RejectedExecutionException("Task is rejected due to execeeded max thread pool size: " + MAX_THREAD_POOL_SIZE);
                //    }

                return super.execute(command);
            }

            @Override
            public <T> CompletableFuture<T> execute(final Callable<T> command) {
                //    if (threadPoolExecutor.getActiveCount() >= MAX_THREAD_POOL_SIZE) {
                //        throw new RejectedExecutionException("Task is rejected due to execeeded max thread pool size: " + MAX_THREAD_POOL_SIZE);
                //    }

                return super.execute(command);
            }
        };
    }

    static final int DEFAULT_MAX_THREAD_NUM = N.CPU_CORES;
    static final int DEFAULT_READING_THREAD_NUM = 8;

    static final int MAX_QUEUE_SIZE = 8192;
    static final int DEFAULT_QUEUE_SIZE_PER_ITERATOR = 16;
    static final Splitter DEFAULT_SPILTTER = Splitter.ITERATOR;
    static final Random RAND = new SecureRandom();

    static final Comparator<Character> CHAR_COMPARATOR = new Comparator<Character>() {
        @Override
        public int compare(Character o1, Character o2) {
            return Character.compare(o1, o2);
        }
    };

    static final Comparator<Byte> BYTE_COMPARATOR = new Comparator<Byte>() {
        @Override
        public int compare(Byte o1, Byte o2) {
            return Byte.compare(o1, o2);
        }
    };

    static final Comparator<Short> SHORT_COMPARATOR = new Comparator<Short>() {
        @Override
        public int compare(Short o1, Short o2) {
            return Short.compare(o1, o2);
        }
    };

    static final Comparator<Integer> INT_COMPARATOR = new Comparator<Integer>() {
        @Override
        public int compare(Integer o1, Integer o2) {
            return Integer.compare(o1, o2);
        }
    };

    static final Comparator<Long> LONG_COMPARATOR = new Comparator<Long>() {
        @Override
        public int compare(Long o1, Long o2) {
            return Long.compare(o1, o2);
        }
    };

    static final Comparator<Float> FLOAT_COMPARATOR = new Comparator<Float>() {
        @Override
        public int compare(Float o1, Float o2) {
            return Float.compare(o1, o2);
        }
    };

    static final Comparator<Double> DOUBLE_COMPARATOR = new Comparator<Double>() {
        @Override
        public int compare(Double o1, Double o2) {
            return Double.compare(o1, o2);
        }
    };

    @SuppressWarnings("rawtypes")
    static final Comparator OBJECT_COMPARATOR = new Comparator<Comparable>() {
        @Override
        public int compare(final Comparable a, final Comparable b) {
            return a == null ? (b == null ? 0 : -1) : (b == null ? 1 : a.compareTo(b));
        }
    };

    static final BiMap<Class<?>, Comparator<?>> defaultComparator = new BiMap<>();
    static final Field listElementDataField;
    static final Field listSizeField;
    static volatile boolean isListElementDataFieldGettable = true;
    static volatile boolean isListElementDataFieldSettable = true;
    static final Map<Class<?>, Integer> clsNum = new BiMap<>();

    @SuppressWarnings("rawtypes")
    static final BinaryOperator reducingCombiner = new BinaryOperator() {
        @Override
        public Object apply(Object t, Object u) {
            if (t instanceof Multiset) {
                ((Multiset) t).addAll((Multiset) u);
                return t;
            } else if (t instanceof LongMultiset) {
                ((LongMultiset) t).addAll((LongMultiset) u);
                return t;
            } else if (t instanceof Multimap) {
                ((Multimap) t).putAll((Multimap) u);
                return t;
            } else if (t instanceof Collection) {
                ((Collection) t).addAll((Collection) u);
                return t;
            } else if (t instanceof Map) {
                ((Map) t).putAll((Map) u);
                return t;
            } else if (t instanceof Object[]) {
                return N.concat((Object[]) t, (Object[]) u);
            } else if (t instanceof Sheet) {
                ((Sheet) t).putAll((Sheet) u);
                return t;
            } else {
                final Class<?> cls = t.getClass();
                final Integer num = clsNum.get(cls);

                if (num == null) {
                    throw new RuntimeException(cls.getCanonicalName()
                            + " can't be combined by default. Only Map/Collection/Multiset/LongMultiset/Multimap/Sheet/BooleanList ... ObjectList/boolean[] ... Object[] are supported");
                }

                switch (num.intValue()) {
                    case 0:
                        return N.concat((boolean[]) t, (boolean[]) u);
                    case 1:
                        return N.concat((char[]) t, (char[]) u);
                    case 2:
                        return N.concat((byte[]) t, (byte[]) u);
                    case 3:
                        return N.concat((short[]) t, (short[]) u);
                    case 4:
                        return N.concat((int[]) t, (int[]) u);
                    case 5:
                        return N.concat((long[]) t, (long[]) u);
                    case 6:
                        return N.concat((float[]) t, (float[]) u);
                    case 7:
                        return N.concat((double[]) t, (double[]) u);
                    case 8:
                        ((BooleanList) t).addAll((BooleanList) u);
                        return t;
                    case 9:
                        ((CharList) t).addAll((CharList) u);
                        return t;
                    case 10:
                        ((ByteList) t).addAll((ByteList) u);
                        return t;
                    case 11:
                        ((ShortList) t).addAll((ShortList) u);
                        return t;
                    case 12:
                        ((IntList) t).addAll((IntList) u);
                        return t;
                    case 13:
                        ((LongList) t).addAll((LongList) u);
                        return t;
                    case 14:
                        ((FloatList) t).addAll((FloatList) u);
                        return t;
                    case 15:
                        ((DoubleList) t).addAll((DoubleList) u);
                        return t;
                    case 16:
                        ((ObjectList) t).addAll((ObjectList) u);
                        return t;

                    default:
                        throw new RuntimeException(cls.getCanonicalName()
                                + " can't be combined by default. Only Map/Collection/Multiset/LongMultiset/Multimap/Sheet/BooleanList ... ObjectList/boolean[] ... Object[] are supported");
                }
            }
        }
    };

    @SuppressWarnings("rawtypes")
    static final BiConsumer collectingCombiner = new BiConsumer() {
        @Override
        public void accept(Object t, Object u) {
            if (t instanceof Multiset) {
                ((Multiset) t).addAll((Multiset) u);
            } else if (t instanceof LongMultiset) {
                ((LongMultiset) t).addAll((LongMultiset) u);
            } else if (t instanceof Multimap) {
                ((Multimap) t).putAll((Multimap) u);
            } else if (t instanceof Map) {
                ((Map) t).putAll((Map) u);
            } else if (t instanceof Collection) {
                ((Collection) t).addAll((Collection) u);
            } else if (t instanceof Map) {
                ((Map) t).putAll((Map) u);
            } else if (t instanceof Sheet) {
                ((Sheet) t).putAll((Sheet) u);
            } else {
                final Class<?> cls = t.getClass();
                Integer num = clsNum.get(cls);

                if (num == null) {
                    throw new RuntimeException(cls.getCanonicalName()
                            + " can't be combined by default. Only Map/Collection/Multiset/LongMultiset/Multimap/Sheet/BooleanList ... ObjectList are supported");
                }

                switch (num.intValue()) {
                    case 8:
                        ((BooleanList) t).addAll((BooleanList) u);
                        break;
                    case 9:
                        ((CharList) t).addAll((CharList) u);
                        break;
                    case 10:
                        ((ByteList) t).addAll((ByteList) u);
                        break;
                    case 11:
                        ((ShortList) t).addAll((ShortList) u);
                        break;
                    case 12:
                        ((IntList) t).addAll((IntList) u);
                        break;
                    case 13:
                        ((LongList) t).addAll((LongList) u);
                        break;
                    case 14:
                        ((FloatList) t).addAll((FloatList) u);
                        break;
                    case 15:
                        ((DoubleList) t).addAll((DoubleList) u);
                        break;
                    case 16:
                        ((ObjectList) t).addAll((ObjectList) u);
                        break;

                    default:
                        throw new RuntimeException(cls.getCanonicalName()
                                + " can't be combined by default. Only Map/Collection/Multiset/LongMultiset/Multimap/Sheet/BooleanList ... ObjectList are supported");
                }
            }
        }
    };

    static {
        defaultComparator.put(char.class, CHAR_COMPARATOR);
        defaultComparator.put(byte.class, BYTE_COMPARATOR);
        defaultComparator.put(short.class, SHORT_COMPARATOR);
        defaultComparator.put(int.class, INT_COMPARATOR);
        defaultComparator.put(long.class, LONG_COMPARATOR);
        defaultComparator.put(float.class, FLOAT_COMPARATOR);
        defaultComparator.put(double.class, DOUBLE_COMPARATOR);
        defaultComparator.put(Object.class, OBJECT_COMPARATOR);
    }

    static {
        Field tmp = null;

        try {
            tmp = ArrayList.class.getDeclaredField("elementData");
        } catch (Exception e) {
            // ignore.
        }

        listElementDataField = tmp != null && tmp.getType().equals(Object[].class) ? tmp : null;

        if (listElementDataField != null) {
            listElementDataField.setAccessible(true);
        }

        tmp = null;

        try {
            tmp = ArrayList.class.getDeclaredField("size");
        } catch (Exception e) {
            // ignore.
        }

        listSizeField = tmp != null && tmp.getType().equals(int.class) ? tmp : null;

        if (listSizeField != null) {
            listSizeField.setAccessible(true);
        }
    }

    static {
        int idx = 0;
        clsNum.put(boolean[].class, idx++); // 0
        clsNum.put(char[].class, idx++);
        clsNum.put(byte[].class, idx++);
        clsNum.put(short[].class, idx++);
        clsNum.put(int[].class, idx++);
        clsNum.put(long[].class, idx++);
        clsNum.put(float[].class, idx++);
        clsNum.put(double[].class, idx++); // 7
        clsNum.put(BooleanList.class, idx++); // 8
        clsNum.put(CharList.class, idx++);
        clsNum.put(ByteList.class, idx++);
        clsNum.put(ShortList.class, idx++);
        clsNum.put(IntList.class, idx++);
        clsNum.put(LongList.class, idx++);
        clsNum.put(FloatList.class, idx++);
        clsNum.put(DoubleList.class, idx++);
        clsNum.put(ObjectList.class, idx++); // 16
    }

    final Set<Runnable> closeHandlers;
    final boolean sorted;
    final Comparator<? super T> cmp;
    private boolean isClosed = false;

    StreamBase(final Collection<Runnable> closeHandlers, final boolean sorted, final Comparator<? super T> cmp) {
        this.closeHandlers = N.isNullOrEmpty(closeHandlers) ? null
                : (closeHandlers instanceof LocalLinkedHashSet ? (LocalLinkedHashSet<Runnable>) closeHandlers : new LocalLinkedHashSet<>(closeHandlers));
        this.sorted = sorted;
        this.cmp = cmp;
    }

    @Override
    public S prepend(S s) {
        return s.append((S) this);
    }

    @Override
    public boolean isParallel() {
        return false;
    }

    @Override
    public S sequential() {
        return (S) this;
    }

    @Override
    public Try<S> tried() {
        return Try.of((S) this);
    }

    @Override
    public S parallel() {
        return parallel(DEFAULT_SPILTTER);
    }

    @Override
    public S parallel(int maxThreadNum) {
        return parallel(maxThreadNum, DEFAULT_SPILTTER);
    }

    @Override
    public S parallel(BaseStream.Splitter splitter) {
        return parallel(DEFAULT_MAX_THREAD_NUM, splitter);
    }

    @Override
    public int maxThreadNum() {
        // throw new UnsupportedOperationException("It's not supported sequential stream.");

        // ignore, do nothing if it's sequential stream.
        return 1;
    }

    @Override
    public S maxThreadNum(int maxThreadNum) {
        // throw new UnsupportedOperationException("It's not supported sequential stream.");  

        // ignore, do nothing if it's sequential stream.
        return (S) this;
    }

    @Override
    public Splitter splitter() {
        // throw new UnsupportedOperationException("It's not supported sequential stream.");

        // ignore, do nothing if it's sequential stream.
        return DEFAULT_SPILTTER;
    }

    @Override
    public S splitter(Splitter splitter) {
        // throw new UnsupportedOperationException("It's not supported sequential stream.");

        // ignore, do nothing if it's sequential stream.
        return (S) this;
    }

    @Override
    public synchronized void close() {
        if (isClosed) {
            return;
        }

        isClosed = true;

        if (N.notNullOrEmpty(closeHandlers)) {
            Throwable ex = null;

            for (Runnable closeHandler : closeHandlers) {
                try {
                    closeHandler.run();
                } catch (Throwable e) {
                    if (ex == null) {
                        ex = e;
                    } else {
                        ex.addSuppressed(e);
                    }
                }
            }

            if (ex != null) {
                throw N.toRuntimeException(ex);
            }
        }
    }

    protected CharStream newStream(final char[] a, final boolean sorted) {
        if (this.isParallel()) {
            return new ParallelArrayCharStream(a, 0, a.length, closeHandlers, sorted, this.maxThreadNum(), this.splitter());
        } else {
            return new ArrayCharStream(a, closeHandlers, sorted);
        }
    }

    protected CharStream newStream(final CharIterator iter, final boolean sorted) {
        if (this.isParallel()) {
            final ImmutableCharIterator charIter = iter instanceof ImmutableCharIterator ? (ImmutableCharIterator) iter : new ImmutableCharIterator() {
                @Override
                public boolean hasNext() {
                    return iter.hasNext();
                }

                @Override
                public char next() {
                    return iter.next();
                }
            };

            return new ParallelIteratorCharStream(charIter, closeHandlers, sorted, this.maxThreadNum(), this.splitter());
        } else {
            final ImmutableCharIterator charIter = iter instanceof ImmutableCharIterator ? (ImmutableCharIterator) iter : new ImmutableCharIterator() {
                @Override
                public boolean hasNext() {
                    return iter.hasNext();
                }

                @Override
                public char next() {
                    return iter.next();
                }
            };

            return new IteratorCharStream(charIter, closeHandlers, sorted);
        }
    }

    protected ByteStream newStream(final byte[] a, final boolean sorted) {
        if (this.isParallel()) {
            return new ParallelArrayByteStream(a, 0, a.length, closeHandlers, sorted, this.maxThreadNum(), this.splitter());
        } else {
            return new ArrayByteStream(a, closeHandlers, sorted);
        }
    }

    protected ByteStream newStream(final ByteIterator iter, final boolean sorted) {
        if (this.isParallel()) {
            final ImmutableByteIterator byteIter = iter instanceof ImmutableByteIterator ? (ImmutableByteIterator) iter : new ImmutableByteIterator() {
                @Override
                public boolean hasNext() {
                    return iter.hasNext();
                }

                @Override
                public byte next() {
                    return iter.next();
                }
            };

            return new ParallelIteratorByteStream(byteIter, closeHandlers, sorted, this.maxThreadNum(), this.splitter());
        } else {
            final ImmutableByteIterator byteIter = iter instanceof ImmutableByteIterator ? (ImmutableByteIterator) iter : new ImmutableByteIterator() {
                @Override
                public boolean hasNext() {
                    return iter.hasNext();
                }

                @Override
                public byte next() {
                    return iter.next();
                }
            };

            return new IteratorByteStream(byteIter, closeHandlers, sorted);
        }
    }

    protected ShortStream newStream(final short[] a, final boolean sorted) {
        if (this.isParallel()) {
            return new ParallelArrayShortStream(a, 0, a.length, closeHandlers, sorted, this.maxThreadNum(), this.splitter());
        } else {
            return new ArrayShortStream(a, closeHandlers, sorted);
        }
    }

    protected ShortStream newStream(final ShortIterator iter, final boolean sorted) {
        if (this.isParallel()) {
            final ImmutableShortIterator shortIter = iter instanceof ImmutableShortIterator ? (ImmutableShortIterator) iter : new ImmutableShortIterator() {
                @Override
                public boolean hasNext() {
                    return iter.hasNext();
                }

                @Override
                public short next() {
                    return iter.next();
                }
            };

            return new ParallelIteratorShortStream(shortIter, closeHandlers, sorted, this.maxThreadNum(), this.splitter());
        } else {
            final ImmutableShortIterator shortIter = iter instanceof ImmutableShortIterator ? (ImmutableShortIterator) iter : new ImmutableShortIterator() {
                @Override
                public boolean hasNext() {
                    return iter.hasNext();
                }

                @Override
                public short next() {
                    return iter.next();
                }
            };

            return new IteratorShortStream(shortIter, closeHandlers, sorted);
        }
    }

    protected IntStream newStream(final int[] a, final boolean sorted) {
        if (this.isParallel()) {
            return new ParallelArrayIntStream(a, 0, a.length, closeHandlers, sorted, this.maxThreadNum(), this.splitter());
        } else {
            return new ArrayIntStream(a, closeHandlers, sorted);
        }
    }

    protected IntStream newStream(final IntIterator iter, final boolean sorted) {
        if (this.isParallel()) {
            final ImmutableIntIterator intIter = iter instanceof ImmutableIntIterator ? (ImmutableIntIterator) iter : new ImmutableIntIterator() {
                @Override
                public boolean hasNext() {
                    return iter.hasNext();
                }

                @Override
                public int next() {
                    return iter.next();
                }
            };

            return new ParallelIteratorIntStream(intIter, closeHandlers, sorted, this.maxThreadNum(), this.splitter());
        } else {
            final ImmutableIntIterator intIter = iter instanceof ImmutableIntIterator ? (ImmutableIntIterator) iter : new ImmutableIntIterator() {
                @Override
                public boolean hasNext() {
                    return iter.hasNext();
                }

                @Override
                public int next() {
                    return iter.next();
                }
            };

            return new IteratorIntStream(intIter, closeHandlers, sorted);
        }
    }

    protected LongStream newStream(final long[] a, final boolean sorted) {
        if (this.isParallel()) {
            return new ParallelArrayLongStream(a, 0, a.length, closeHandlers, sorted, this.maxThreadNum(), this.splitter());
        } else {
            return new ArrayLongStream(a, closeHandlers, sorted);
        }
    }

    protected LongStream newStream(final LongIterator iter, final boolean sorted) {
        if (this.isParallel()) {
            final ImmutableLongIterator longIter = iter instanceof ImmutableLongIterator ? (ImmutableLongIterator) iter : new ImmutableLongIterator() {
                @Override
                public boolean hasNext() {
                    return iter.hasNext();
                }

                @Override
                public long next() {
                    return iter.next();
                }
            };

            return new ParallelIteratorLongStream(longIter, closeHandlers, sorted, this.maxThreadNum(), this.splitter());
        } else {
            final ImmutableLongIterator longIter = iter instanceof ImmutableLongIterator ? (ImmutableLongIterator) iter : new ImmutableLongIterator() {
                @Override
                public boolean hasNext() {
                    return iter.hasNext();
                }

                @Override
                public long next() {
                    return iter.next();
                }
            };

            return new IteratorLongStream(longIter, closeHandlers, sorted);
        }
    }

    protected FloatStream newStream(final float[] a, final boolean sorted) {
        if (this.isParallel()) {
            return new ParallelArrayFloatStream(a, 0, a.length, closeHandlers, sorted, this.maxThreadNum(), this.splitter());
        } else {
            return new ArrayFloatStream(a, closeHandlers, sorted);
        }
    }

    protected FloatStream newStream(final FloatIterator iter, final boolean sorted) {
        if (this.isParallel()) {
            final ImmutableFloatIterator floatIter = iter instanceof ImmutableFloatIterator ? (ImmutableFloatIterator) iter : new ImmutableFloatIterator() {
                @Override
                public boolean hasNext() {
                    return iter.hasNext();
                }

                @Override
                public float next() {
                    return iter.next();
                }
            };

            return new ParallelIteratorFloatStream(floatIter, closeHandlers, sorted, this.maxThreadNum(), this.splitter());
        } else {
            final ImmutableFloatIterator floatIter = iter instanceof ImmutableFloatIterator ? (ImmutableFloatIterator) iter : new ImmutableFloatIterator() {
                @Override
                public boolean hasNext() {
                    return iter.hasNext();
                }

                @Override
                public float next() {
                    return iter.next();
                }
            };

            return new IteratorFloatStream(floatIter, closeHandlers, sorted);
        }
    }

    protected DoubleStream newStream(final double[] a, final boolean sorted) {
        if (this.isParallel()) {
            return new ParallelArrayDoubleStream(a, 0, a.length, closeHandlers, sorted, this.maxThreadNum(), this.splitter());
        } else {
            return new ArrayDoubleStream(a, closeHandlers, sorted);
        }
    }

    protected DoubleStream newStream(final DoubleIterator iter, final boolean sorted) {
        if (this.isParallel()) {
            final ImmutableDoubleIterator doubleIter = iter instanceof ImmutableDoubleIterator ? (ImmutableDoubleIterator) iter
                    : new ImmutableDoubleIterator() {
                        @Override
                        public boolean hasNext() {
                            return iter.hasNext();
                        }

                        @Override
                        public double next() {
                            return iter.next();
                        }
                    };

            return new ParallelIteratorDoubleStream(doubleIter, closeHandlers, sorted, this.maxThreadNum(), this.splitter());
        } else {
            final ImmutableDoubleIterator doubleIter = iter instanceof ImmutableDoubleIterator ? (ImmutableDoubleIterator) iter
                    : new ImmutableDoubleIterator() {
                        @Override
                        public boolean hasNext() {
                            return iter.hasNext();
                        }

                        @Override
                        public double next() {
                            return iter.next();
                        }
                    };

            return new IteratorDoubleStream(doubleIter, closeHandlers, sorted);
        }
    }

    protected <E> Stream<E> newStream(final E[] a, final boolean sorted, final Comparator<? super E> comparator) {
        if (this.isParallel()) {
            return new ParallelArrayStream<E>(a, 0, a.length, closeHandlers, sorted, comparator, this.maxThreadNum(), this.splitter());
        } else {
            return new ArrayStream<E>(a, closeHandlers, sorted, comparator);
        }
    }

    protected <E> Stream<E> newStream(final Iterator<E> iter, final boolean sorted, final Comparator<? super E> comparator) {
        if (this.isParallel()) {
            return new ParallelIteratorStream<E>(iter, closeHandlers, sorted, comparator, this.maxThreadNum(), this.splitter());
        } else {
            return new IteratorStream<E>(iter, closeHandlers, sorted, comparator);
        }
    }

    static void setError(final Holder<Throwable> errorHolder, Throwable e, final MutableBoolean onGoing) {
        onGoing.setFalse();

        synchronized (errorHolder) {
            if (errorHolder.value() == null) {
                errorHolder.setValue(e);
            } else {
                errorHolder.value().addSuppressed(e);
            }
        }
    }

    static void throwError(final Holder<Throwable> errorHolder, final MutableBoolean onGoing) {
        onGoing.setFalse();

        throw N.toRuntimeException(errorHolder.value());
    }

    static void setError(final Holder<Throwable> errorHolder, Throwable e) {
        synchronized (errorHolder) {
            if (errorHolder.value() == null) {
                errorHolder.setValue(e);
            } else {
                errorHolder.value().addSuppressed(e);
            }
        }
    }

    static void throwError(final Holder<Throwable> errorHolder) {
        throw N.toRuntimeException(errorHolder.value());
    }

    static int calculateQueueSize(int len) {
        return N.min(MAX_QUEUE_SIZE, len * DEFAULT_QUEUE_SIZE_PER_ITERATOR);
    }

    static boolean isSameComparator(Comparator<?> a, Comparator<?> b) {
        if (a == b) {
            return true;
        } else if (a == null) {
            return defaultComparator.containsValue(b);
        } else if (b == null) {
            return defaultComparator.containsValue(a);
        }

        return false;
    }

    static void checkIndex(int fromIndex, int toIndex, int length) {
        if (fromIndex < 0 || toIndex < fromIndex || toIndex > length) {
            throw new IllegalArgumentException("Invalid fromIndex(" + fromIndex + ") or toIndex(" + toIndex + ")");
        }
    }

    static int toInt(long max) {
        return max > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) max;
    }

    static ImmutableCharIterator charIterator(final ImmutableIterator<Character> iter) {
        final ImmutableCharIterator charIter = new ImmutableCharIterator() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public char next() {
                return iter.next();
            }

            @Override
            public long count() {
                return iter.count();
            }

            @Override
            public void skip(long n) {
                iter.skip(n);
            }
        };

        return charIter;
    }

    static ImmutableByteIterator byteIterator(final ImmutableIterator<Byte> iter) {
        final ImmutableByteIterator byteIter = new ImmutableByteIterator() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public byte next() {
                return iter.next();
            }

            @Override
            public long count() {
                return iter.count();
            }

            @Override
            public void skip(long n) {
                iter.skip(n);
            }
        };

        return byteIter;
    }

    static ImmutableShortIterator shortIterator(final ImmutableIterator<Short> iter) {
        final ImmutableShortIterator shortIter = new ImmutableShortIterator() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public short next() {
                return iter.next();
            }

            @Override
            public long count() {
                return iter.count();
            }

            @Override
            public void skip(long n) {
                iter.skip(n);
            }
        };

        return shortIter;
    }

    static ImmutableIntIterator intIterator(final ImmutableIterator<Integer> iter) {
        final ImmutableIntIterator intIter = new ImmutableIntIterator() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public int next() {
                return iter.next();
            }

            @Override
            public long count() {
                return iter.count();
            }

            @Override
            public void skip(long n) {
                iter.skip(n);
            }
        };

        return intIter;
    }

    static ImmutableLongIterator longIterator(final ImmutableIterator<Long> iter) {
        final ImmutableLongIterator longIter = new ImmutableLongIterator() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public long next() {
                return iter.next();
            }

            @Override
            public long count() {
                return iter.count();
            }

            @Override
            public void skip(long n) {
                iter.skip(n);
            }
        };

        return longIter;
    }

    static ImmutableFloatIterator floatIterator(final ImmutableIterator<Float> iter) {
        final ImmutableFloatIterator floatIter = new ImmutableFloatIterator() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public float next() {
                return iter.next();
            }

            @Override
            public long count() {
                return iter.count();
            }

            @Override
            public void skip(long n) {
                iter.skip(n);
            }
        };

        return floatIter;
    }

    static ImmutableDoubleIterator doubleIterator(final ImmutableIterator<Double> iter) {
        final ImmutableDoubleIterator doubleIter = new ImmutableDoubleIterator() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public double next() {
                return iter.next();
            }

            @Override
            public long count() {
                return iter.count();
            }

            @Override
            public void skip(long n) {
                iter.skip(n);
            }
        };

        return doubleIter;
    }

    static Set<Runnable> mergeCloseHandlers(final StreamBase<?, ?> stream, Set<Runnable> closeHandlers) {
        if (N.isNullOrEmpty(closeHandlers) && N.isNullOrEmpty(stream.closeHandlers)) {
            return null;
        }

        final Set<Runnable> newCloseHandlers = new LocalLinkedHashSet<>();

        if (N.notNullOrEmpty(closeHandlers)) {
            newCloseHandlers.addAll(closeHandlers);
        }

        if (N.notNullOrEmpty(stream.closeHandlers)) {
            newCloseHandlers.addAll(stream.closeHandlers);
        }

        return newCloseHandlers;
    }

    static Object getHashKey(Object obj) {
        return obj == null || obj.getClass().isArray() == false ? obj : Wrapper.of(obj);
    }

    static final class LocalLinkedHashSet<T> extends LinkedHashSet<T> {
        private static final long serialVersionUID = -97425473105100734L;

        public LocalLinkedHashSet() {
            super();
        }

        public LocalLinkedHashSet(int initialCapacity) {
            super(initialCapacity);
        }

        public LocalLinkedHashSet(int initialCapacity, float loadFactor) {
            super(initialCapacity, loadFactor);
        }

        public LocalLinkedHashSet(Collection<? extends T> c) {
            super(c);
        }
    }
}