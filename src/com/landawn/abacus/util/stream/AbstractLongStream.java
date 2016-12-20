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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import com.landawn.abacus.util.IndexedLong;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.LongList;
import com.landawn.abacus.util.LongSummaryStatistics;
import com.landawn.abacus.util.Multimap;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.MutableLong;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Nth;
import com.landawn.abacus.util.Optional;
import com.landawn.abacus.util.OptionalLong;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BinaryOperator;
import com.landawn.abacus.util.function.LongBiFunction;
import com.landawn.abacus.util.function.LongConsumer;
import com.landawn.abacus.util.function.LongFunction;
import com.landawn.abacus.util.function.LongPredicate;
import com.landawn.abacus.util.function.LongTriFunction;
import com.landawn.abacus.util.function.ObjLongConsumer;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.function.ToLongFunction;

/**
 * This class is a sequential, stateful and immutable stream implementation.
 *
 * @since 0.8
 * 
 * @author Haiyang Li
 */
abstract class AbstractLongStream extends LongStream {

    AbstractLongStream(final Collection<Runnable> closeHandlers, final boolean sorted) {
        super(closeHandlers, sorted);
    }

    @Override
    public LongStream drop(final long n, final LongConsumer action) {
        if (n < 0) {
            throw new IllegalArgumentException("'n' can't be less than 0");
        } else if (n == 0) {
            return this;
        }

        if (this.isParallel()) {
            final AtomicLong cnt = new AtomicLong(n);

            return dropWhile(new LongPredicate() {
                @Override
                public boolean test(long value) {
                    return cnt.decrementAndGet() >= 0;
                }
            }, action);
        } else {
            final MutableLong cnt = MutableLong.of(n);

            return dropWhile(new LongPredicate() {
                @Override
                public boolean test(long value) {
                    return cnt.decrementAndGet() >= 0;
                }
            }, action);
        }
    }

    @Override
    public LongStream dropWhile(final LongPredicate predicate, final LongConsumer action) {
        N.requireNonNull(predicate);
        N.requireNonNull(action);

        return dropWhile(new LongPredicate() {
            @Override
            public boolean test(long value) {
                if (predicate.test(value)) {
                    action.accept(value);
                    return true;
                }

                return false;
            }
        });
    }

    @Override
    public <K> Map<K, List<Long>> toMap(LongFunction<? extends K> classifier) {
        return toMap(classifier, new Supplier<Map<K, List<Long>>>() {
            @Override
            public Map<K, List<Long>> get() {
                return new HashMap<>();
            }
        });
    }

    @Override
    public <K, M extends Map<K, List<Long>>> M toMap(LongFunction<? extends K> classifier, Supplier<M> mapFactory) {
        final Collector<Long, ?, List<Long>> downstream = Collectors.toList();
        return toMap(classifier, downstream, mapFactory);
    }

    @Override
    public <K, A, D> Map<K, D> toMap(LongFunction<? extends K> classifier, Collector<Long, A, D> downstream) {
        return toMap(classifier, downstream, new Supplier<Map<K, D>>() {
            @Override
            public Map<K, D> get() {
                return new HashMap<>();
            }
        });
    }

    @Override
    public <K, U> Map<K, U> toMap(LongFunction<? extends K> keyMapper, LongFunction<? extends U> valueMapper) {
        return toMap(keyMapper, valueMapper, new Supplier<Map<K, U>>() {
            @Override
            public Map<K, U> get() {
                return new HashMap<>();
            }
        });
    }

    @Override
    public <K, U, M extends Map<K, U>> M toMap(LongFunction<? extends K> keyMapper, LongFunction<? extends U> valueMapper, Supplier<M> mapSupplier) {
        final BinaryOperator<U> mergeFunction = Collectors.throwingMerger();
        return toMap(keyMapper, valueMapper, mergeFunction, mapSupplier);
    }

    @Override
    public <K, U> Map<K, U> toMap(LongFunction<? extends K> keyMapper, LongFunction<? extends U> valueMapper, BinaryOperator<U> mergeFunction) {
        return toMap(keyMapper, valueMapper, mergeFunction, new Supplier<Map<K, U>>() {
            @Override
            public Map<K, U> get() {
                return new HashMap<>();
            }
        });
    }

    @Override
    public <K> Multimap<K, Long, List<Long>> toMultimap(LongFunction<? extends K> keyMapper) {
        return toMultimap(keyMapper, LongFunction.BOX);
    }

    @Override
    public <K, V extends Collection<Long>> Multimap<K, Long, V> toMultimap(LongFunction<? extends K> keyMapper, Supplier<Multimap<K, Long, V>> mapSupplier) {
        return toMultimap(keyMapper, LongFunction.BOX, mapSupplier);
    }

    @Override
    public <K, U> Multimap<K, U, List<U>> toMultimap(LongFunction<? extends K> keyMapper, LongFunction<? extends U> valueMapper) {
        return toMultimap(keyMapper, valueMapper, new Supplier<Multimap<K, U, List<U>>>() {
            @Override
            public Multimap<K, U, List<U>> get() {
                return N.newListMultimap();
            }
        });
    }

    @Override
    public LongStream distinct() {
        return newStream(this.sequential().filter(new LongPredicate() {
            private final Set<Object> set = new HashSet<>();

            @Override
            public boolean test(long value) {
                return set.add(value);
            }
        }).longIterator(), sorted);
    }

    @Override
    public OptionalLong first() {
        final LongIterator iter = this.longIterator();

        return iter.hasNext() ? OptionalLong.of(iter.next()) : OptionalLong.empty();
    }

    @Override
    public OptionalLong last() {
        final LongIterator iter = this.longIterator();

        if (iter.hasNext() == false) {
            return OptionalLong.empty();
        }

        long next = 0;

        while (iter.hasNext()) {
            next = iter.next();
        }

        return OptionalLong.of(next);
    }

    @Override
    public LongStream except(final Collection<?> c) {
        final Multiset<?> multiset = Multiset.of(c);

        return newStream(this.sequential().filter(new LongPredicate() {
            @Override
            public boolean test(long value) {
                return multiset.getAndRemove(value) < 1;
            }
        }).longIterator(), sorted);
    }

    @Override
    public LongStream intersect(final Collection<?> c) {
        final Multiset<?> multiset = Multiset.of(c);

        return newStream(this.sequential().filter(new LongPredicate() {
            @Override
            public boolean test(long value) {
                return multiset.getAndRemove(value) > 0;
            }
        }).longIterator(), sorted);
    }

    @Override
    public LongStream xor(final Collection<Long> c) {
        final Multiset<?> multiset = Multiset.of(c);

        return newStream(this.sequential().filter(new LongPredicate() {
            @Override
            public boolean test(long value) {
                return multiset.getAndRemove(value) < 1;
            }
        }).append(Stream.of(c).filter(new Predicate<Long>() {
            @Override
            public boolean test(Long value) {
                return multiset.getAndRemove(value) > 0;
            }
        }).mapToLong(ToLongFunction.UNBOX)).longIterator(), false);
    }

    @Override
    public Stream<LongStream> splitAt(final int n) {
        if (n < 0) {
            throw new IllegalArgumentException("'n' can't be negative");
        }

        final LongIterator iter = this.longIterator();
        final LongList list = new LongList();

        while (list.size() < n && iter.hasNext()) {
            list.add(iter.next());
        }

        final LongStream[] a = new LongStream[] { new ArrayLongStream(list.array(), 0, list.size(), null, sorted),
                new IteratorLongStream(iter instanceof ImmutableLongIterator ? (ImmutableLongIterator) iter : new ImmutableLongIterator() {
                    @Override
                    public boolean hasNext() {
                        return iter.hasNext();
                    }

                    @Override
                    public long next() {
                        return iter.next();
                    }

                }, null, sorted) };

        return this.newStream(a, false, null);
    }

    @Override
    public Stream<LongStream> splitBy(LongPredicate where) {
        N.requireNonNull(where);

        final LongIterator iter = this.longIterator();
        final LongList list = new LongList();
        long next = 0;
        LongStream p = null;

        while (iter.hasNext()) {
            next = iter.next();

            if (where.test(next)) {
                list.add(next);
            } else {
                p = LongStream.of(next);

                break;
            }
        }

        final LongStream[] a = new LongStream[] { new ArrayLongStream(list.array(), 0, list.size(), null, sorted),
                new IteratorLongStream(iter instanceof ImmutableLongIterator ? (ImmutableLongIterator) iter : new ImmutableLongIterator() {
                    @Override
                    public boolean hasNext() {
                        return iter.hasNext();
                    }

                    @Override
                    public long next() {
                        return iter.next();
                    }

                }, null, sorted) };

        if (p != null) {
            if (sorted) {
                new IteratorLongStream(a[1].prepend(p).longIterator(), null, sorted);
            } else {
                a[1] = a[1].prepend(p);
            }
        }

        return this.newStream(a, false, null);
    }

    @Override
    public LongStream reverse() {
        final long[] a = toArray();
        return newStream(new ImmutableLongIterator() {
            private int cursor = a.length;

            @Override
            public boolean hasNext() {
                return cursor > 0;
            }

            @Override
            public long next() {
                if (cursor <= 0) {
                    throw new NoSuchElementException();
                }

                return a[--cursor];
            }

            @Override
            public long count() {
                return cursor - 0;
            }

            @Override
            public void skip(long n) {
                cursor = cursor > n ? cursor - (int) n : 0;
            }
        }, false);
    }

    @Override
    public LongStream shuffle() {
        final long[] a = toArray();

        N.shuffle(a);

        return newStream(a, false);
    }

    @Override
    public LongStream rotate(int distance) {
        final long[] a = toArray();

        N.rotate(a, distance);

        return newStream(a, false);
    }

    @Override
    public Optional<Map<Percentage, Long>> distribution() {
        final long[] a = sorted().toArray();

        if (a.length == 0) {
            return Optional.empty();
        }

        return Optional.of(N.distribution(a));
    }

    @Override
    public Pair<LongSummaryStatistics, Optional<Map<Percentage, Long>>> summarize2() {
        final long[] a = sorted().toArray();

        final LongSummaryStatistics summaryStatistics = new LongSummaryStatistics(a.length, N.sum(a), a[0], a[a.length - 1]);
        final Optional<Map<Percentage, Long>> distribution = a.length == 0 ? Optional.<Map<Percentage, Long>> empty() : Optional.of(N.distribution(a));

        return Pair.of(summaryStatistics, distribution);
    }

    @Override
    public String join(final CharSequence delimiter) {
        return join(delimiter, "", "");
    }

    @Override
    public String join(final CharSequence delimiter, final CharSequence prefix, final CharSequence suffix) {
        final Supplier<Joiner> supplier = new Supplier<Joiner>() {
            @Override
            public Joiner get() {
                return new Joiner(delimiter, prefix, suffix);
            }
        };

        final ObjLongConsumer<Joiner> accumulator = new ObjLongConsumer<Joiner>() {
            @Override
            public void accept(Joiner a, long t) {
                a.add(t);
            }
        };

        final BiConsumer<Joiner, Joiner> combiner = new BiConsumer<Joiner, Joiner>() {
            @Override
            public void accept(Joiner a, Joiner b) {
                a.merge(b);
            }
        };

        final Joiner joiner = collect(supplier, accumulator, combiner);
        return joiner.toString();
    }

    @Override
    public <R> R collect(Supplier<R> supplier, ObjLongConsumer<R> accumulator) {
        final BiConsumer<R, R> combiner = collectingCombiner;
        return collect(supplier, accumulator, combiner);
    }

    @Override
    public Stream<IndexedLong> indexed() {
        return newStream(this.sequential().mapToObj(new LongFunction<IndexedLong>() {
            final MutableLong idx = new MutableLong();

            @Override
            public IndexedLong apply(long t) {
                return IndexedLong.of(idx.getAndIncrement(), t);
            }
        }).iterator(), false, null);
    }

    @Override
    public LongStream append(LongStream stream) {
        return LongStream.concat(this, stream);
    }

    @Override
    public LongStream prepend(LongStream stream) {
        return LongStream.concat(stream, this);
    }

    @Override
    public LongStream merge(LongStream b, LongBiFunction<Nth> nextSelector) {
        return LongStream.merge(this, b, nextSelector);
    }

    @Override
    public LongStream zipWith(LongStream b, LongBiFunction<Long> zipFunction) {
        return LongStream.zip(this, b, zipFunction);
    }

    @Override
    public LongStream zipWith(LongStream b, LongStream c, LongTriFunction<Long> zipFunction) {
        return LongStream.zip(this, b, c, zipFunction);
    }

    @Override
    public LongStream zipWith(LongStream b, long valueForNoneA, long valueForNoneB, LongBiFunction<Long> zipFunction) {
        return LongStream.zip(this, b, valueForNoneA, valueForNoneB, zipFunction);
    }

    @Override
    public LongStream zipWith(LongStream b, LongStream c, long valueForNoneA, long valueForNoneB, long valueForNoneC, LongTriFunction<Long> zipFunction) {
        return LongStream.zip(this, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
    }

    @Override
    public LongStream cached() {
        return this.newStream(toArray(), sorted);
    }
}
