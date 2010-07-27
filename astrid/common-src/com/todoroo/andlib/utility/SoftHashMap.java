package com.todoroo.andlib.utility;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * SoftHashMap from javaspecialists.eu Issue 98
 * <http://www.javaspecialists.eu/archive/Issue098.html>
 *
 * @param <K>
 * @param <V>
 */
public class SoftHashMap<K, V> extends AbstractMap<K, V> {
    private static final long serialVersionUID = -3796460667941300642L;

    /** The internal HashMap that will hold the SoftReference. */
    private final Map<K, SoftReference<V>> hash = new HashMap<K, SoftReference<V>>();

    private final Map<SoftReference<V>, K> reverseLookup = new HashMap<SoftReference<V>, K>();

    /** Reference queue for cleared SoftReference objects. */
    protected final ReferenceQueue<V> queue = new ReferenceQueue<V>();

    @Override
    public V get(Object key) {
        expungeStaleEntries();
        V result = null;
        // We get the SoftReference represented by that key
        SoftReference<V> soft_ref = hash.get(key);
        if (soft_ref != null) {
            // From the SoftReference we get the value, which can be
            // null if it has been garbage collected
            result = soft_ref.get();
            if (result == null) {
                // If the value has been garbage collected, remove the
                // entry from the HashMap.
                hash.remove(key);
                reverseLookup.remove(soft_ref);
            }
        }
        return result;
    }

    private void expungeStaleEntries() {
        Reference<? extends V> sv;
        while ((sv = queue.poll()) != null) {
            hash.remove(reverseLookup.remove(sv));
        }
    }

    @Override
    public V put(K key, V value) {
        expungeStaleEntries();
        SoftReference<V> soft_ref = new SoftReference<V>(value, queue);
        reverseLookup.put(soft_ref, key);
        SoftReference<V> result = hash.put(key, soft_ref);
        if (result == null)
            return null;
        reverseLookup.remove(result);
        return result.get();
    }

    @Override
    public V remove(Object key) {
        expungeStaleEntries();
        SoftReference<V> result = hash.remove(key);
        if (result == null)
            return null;
        return result.get();
    }

    @Override
    public void clear() {
        hash.clear();
        reverseLookup.clear();
    }

    @Override
    public int size() {
        expungeStaleEntries();
        return hash.size();
    }

    /**
     * Returns a copy of the key/values in the map at the point of calling.
     * However, setValue still sets the value in the actual SoftHashMap.
     */
    @Override
    public Set<Entry<K, V>> entrySet() {
        expungeStaleEntries();
        Set<Entry<K, V>> result = new LinkedHashSet<Entry<K, V>>();
        for (final Entry<K, SoftReference<V>> entry : hash.entrySet()) {
            final V value = entry.getValue().get();
            if (value != null) {
                result.add(new Entry<K, V>() {
                    public K getKey() {
                        return entry.getKey();
                    }

                    public V getValue() {
                        return value;
                    }

                    public V setValue(V v) {
                        entry.setValue(new SoftReference<V>(v, queue));
                        return value;
                    }
                });
            }
        }
        return result;
    }
}
