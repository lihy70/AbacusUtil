/*
 * Copyright (c) 2017, Haiyang Li.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import com.landawn.abacus.util.function.Supplier;

/**
 * 
 * @since 0.9
 * 
 * @author Haiyang Li
 */
public abstract class ImmutableIterator<E> implements java.util.Iterator<E> {

    @SuppressWarnings("rawtypes")
    private static final ImmutableIterator EMPTY = new ObjIterator() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Object next() {
            throw new NoSuchElementException();
        }
    };

    public static <E> ImmutableIterator<E> empty() {
        return EMPTY;
    }

    public static <E> ImmutableIterator<E> of(final Iterator<E> iter) {
        if (iter == null) {
            return empty();
        } else if (iter instanceof ImmutableIterator) {
            return (ImmutableIterator<E>) iter;
        }

        return new ObjIterator<E>() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public E next() {
                return iter.next();
            }
        };
    }

    /**
     * @deprecated - UnsupportedOperationException
     */
    @Deprecated
    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    public Set<E> toSet() {
        final Set<E> set = new HashSet<>();

        while (hasNext()) {
            set.add(next());
        }

        return set;
    }

    public <C extends Collection<E>> C toCollection(final Supplier<C> supplier) {
        final C c = supplier.get();

        while (hasNext()) {
            c.add(next());
        }

        return c;
    }
}
