/*
 * Copyright 2019 American Express Travel Related Services Company, Inc.
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
package io.americanexpress.busybee.internal;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;

/**
 * Simple purpose-built SetMultiMap designed for the needs of BusyBee.
 * We didn't want to have BusyBee depend on guava, so Guava's SetMultiMap wasn't an option.
 * Builds on top of HashMap/HashSet's internally.
 * <p>
 * This collection maps keys of type K to sets of values of type V.
 * Each value is unique across all keys in the collection.
 * <p>
 * Not the most efficient impl, but good enough for the BusyBee use case.
 * <p>
 * WARNING: this Impl has some weird quirks, so probably not good for general purpose use.
 * Biggest quirk is the at each value is unique across all keys.
 * if you want to change the key for a value, you must remove it and re-add it.
 * <p>
 * See the method JavaDocs for details.
 */
public class SetMultiMap<K, V> {

    private final HashMap<K, Set<V>> map = new HashMap<>();
    private final HashMap<V, K> reverseMap = new HashMap<>();

    /**
     * All keys are unique and all values are unique.
     * If the Value already exists under any Key,
     * then it will not be added again.
     * <p>
     * WARNING:
     * To re-associate with another Key, first `removeValue` then re-add the Value
     * Else this method will throw an exception.
     *
     * @return true if a new entry was added.
     * @throws IllegalStateException when you add the SAME value again with a DIFFERENT key.
     */
    boolean add(K key, V value) throws IllegalStateException {
        if (reverseMap.get(value) != null && reverseMap.get(value) != key) {
            throw new IllegalStateException("You can't insert the same value for 2 different keys.\n"
                    + "This mapping already exists: \n"
                    + "'" + reverseMap.get(value) + "' =>\n"
                    + "  '" + value + "'\n"
                    + "but you tried to add this new mapping: \n"
                    + "'" + key + "' =>\n"
                    + "  '" + value + "'\n"
                    + "Remove the old mapping first!");
        }

        if (reverseMap.get(value) == key) {
            return false;
        }

        Set<V> valueSet = map.get(key);
        if (valueSet == null) {
            valueSet = new HashSet<>();
            map.put(key, valueSet);
        }
        valueSet.add(value);
        reverseMap.put(value, key);
        return true;
    }

    /**
     * Each value ONLY appears once, so it is associated with at most one key.
     *
     * @return key for which the value is associated with.
     */
    @Nullable
    K keyFor(V value) {
        return reverseMap.get(value);
    }

    /**
     * Removes the value if and only if it exists in the map.
     *
     * @return true if an entry was removed.
     */
    boolean removeValue(V value) {
        if (reverseMap.containsKey(value)) {
            final K keyForRemoved = reverseMap.remove(value);
            map.get(keyForRemoved).remove(value);
            return true;
        }
        return false;
    }

    /**
     * @return provides an iterator ( with remove support ) for all the values in the collection.
     */
    Iterator<V> valuesIterator() {
        final Iterator<Map.Entry<V, K>> valueIterator = reverseMap.entrySet().iterator();
        return multiMapIteratorFromReverseMapIterator(valueIterator);
    }

    private Iterator<V> multiMapIteratorFromReverseMapIterator(final Iterator<Map.Entry<V, K>> valueIterator) {
        return new Iterator<V>() {
            private Map.Entry<V, K> mapEntry;

            @Override
            public boolean hasNext() {
                return valueIterator.hasNext();
            }

            @Override
            public V next() {
                mapEntry = valueIterator.next();
                return mapEntry.getKey();
            }

            @Override
            public void remove() {
                valueIterator.remove();
                map.get(mapEntry.getValue()).remove(mapEntry.getKey());
            }
        };
    }

    private Iterator<V> multiMapIteratorFromForwardMapIterator(final Iterator<V> valueIterator) {
        return new Iterator<V>() {
            private V nextValue;

            @Override
            public boolean hasNext() {
                return valueIterator.hasNext();
            }

            @Override
            public V next() {
                nextValue = valueIterator.next();
                return nextValue;
            }

            @Override
            public void remove() {
                valueIterator.remove();
                reverseMap.remove(nextValue);
            }
        };
    }

    /**
     * @return All the keys that has ever been used in this map since its creation
     */
    Set<K> allKeys() {
        return unmodifiableSet(map.keySet());
    }

    /**
     * @return All values across all keys
     */
    Set<V> allValues() {
        return unmodifiableSet(reverseMap.keySet());
    }

    /**
     * @return Values for the given key
     */
    Set<V> values(K key) {
        final Set<V> values = map.get(key);
        if (values == null) {
            return emptySet();
        }
        return unmodifiableSet(values);
    }

    /**
     * @return True if and only if there are no values ( there may be keys )
     */
    boolean hasNoValues() {
        return reverseMap.isEmpty();
    }

    Iterator<V> valuesIterator(final K key) {
        Set<V> values = map.get(key);
        if (values == null) {
            values = emptySet();
        }
        final Iterator<V> valueIterator = values.iterator();
        return multiMapIteratorFromForwardMapIterator(valueIterator);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n{\n");
        for (Map.Entry<K, Set<V>> entry : map.entrySet()) {
            sb.append("'").append(entry.getKey()).append("'\n");
            for (Iterator<V> iterator = entry.getValue().iterator(); iterator.hasNext(); ) {
                final V value = iterator.next();
                if (iterator.hasNext()) {
                    sb.append(" ├─ '");
                } else {
                    sb.append(" └─ '");
                }
                sb.append(value).append("'\n");
            }
        }
        sb.append("}");

        return sb.toString();
    }
}
