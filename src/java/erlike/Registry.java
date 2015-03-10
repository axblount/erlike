/*
 * Copyright (C) 2015 Alex Blount <axblount@email.arizona.edu>
 *
 * This file is part of Erlike, see <https://github.com/axblount/erlike>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package erlike;

import java.util.Map;
import java.util.Set;
import java.util.AbstractMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Stores and tracks objects by an associated id. Access and id assignment are thread-safe.
 *
 * @param <T> The types of objects tracked by the registry.
 */
public class Registry<T> extends AbstractMap<Integer, T> implements Map<Integer, T> {
    /** The lock used to synchronize access to {@link #byId} and {@link #byValue}. */
    private final Object lock = new Object();

    /** Represents the nextId to be assigned. */
    private final AtomicInteger nextId;

    /** An index of the registered objects by their ids. */
    private final ConcurrentHashMap<Integer, T> byId;

    /** An index of ids by the objects they're registered to. */
    private final ConcurrentHashMap<T, Integer> byValue;

    /**
     * Create a new registry.
     *
     * @param startId The initial id for registered objects.
     */
    public Registry(int startId) {
        this.nextId = new AtomicInteger(startId);
        this.byId = new ConcurrentHashMap<>();
        this.byValue = new ConcurrentHashMap<>();
    }

    /** Create a new registry with an starting id of 0. */
    public Registry() {
        this(0);
    }

    @Override
    public T get(Object key) {
        return byId.get(key);
    }

    /**
     * Retrieve the id of a given object.
     *
     * @throws NullPointerException When the given object is null.
     * @throws IllegalArgumentException When the given object is not part of the registry.
     *
     * @param value The object whose id is to be retrieved.
     * @return The id of the object.
     */
    public Integer getId(T value) {
        if (value == null)
            throw new NullPointerException();
        if (byValue.containsKey(value))
            return byValue.get(value);
        else
            throw new IllegalArgumentException();
    }

    /**
     * Add an object to the registry and assign it an id.
     *
     * @throws NullPointerException The given object is null.
     *
     * @param value The object to be registered.
     * @return The id of the newly registered object.
     */
    public Integer register(T value) {
        if (value == null)
            throw new NullPointerException();
        if (byValue.containsKey(value))
            return byValue.get(value);
        int id = nextId.getAndIncrement();
        synchronized (lock) {
            byId.put(id, value);
            byValue.put(value, id);
        }
        return id;
    }

    @Override
    public T put(Integer key, T value) {
        throw new UnsupportedOperationException("use register instead");
    }

    @Override
    public T remove(Object key) {
        T value;
        synchronized (lock) {
            value = byId.remove(key);
            if (value != null)
                byValue.remove(value);
        }
        return value;
    }

    /**
     * Remove an object from the registry.
     *
     * @param value The object to be removed.
     * @return The id of the removed object.
     */
    public Integer removeValue(T value) {
        Integer id;
        synchronized (lock) {
            id = byValue.remove(value);
            if (id != null)
                byId.remove(id);
        }
        return id;
    }

    @Override
    public Set<Entry<Integer, T>> entrySet() {
        return byId.entrySet();
    }

    @Override
    public boolean containsKey(Object id) {
        return byId.containsKey(id);
    }

    /**
     * A 'more appropriate' alias for {@link #containsKey}.
     *
     * @see #containsKey(Object)
     *
     * @param id The id to be checked.
     * @return true, if the id is present, false otherwise.
     */
    public boolean containsId(Object id) {
        return containsKey(id);
    }

    @Override
    public boolean containsValue(Object value) {
        return byValue.containsKey(value);
    }
}
