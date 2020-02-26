package i2.act.util;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class LRUCache<K, V> implements Map<K, V> {

  public static interface EvictionStrategy {

    public boolean evictionNecessary(final LRUCache<?, ?> cache);

  }

  public static final class NoEviction implements EvictionStrategy {

    @Override
    public final boolean evictionNecessary(final LRUCache<?, ?> cache) {
      return false;
    }

  }

  public static final class EvictionFixedSize implements EvictionStrategy {

    private final int maxSize;

    public EvictionFixedSize(final int maxSize) {
      this.maxSize = maxSize;
    }

    public final int getMaxSize() {
      return this.maxSize;
    }

    @Override
    public final boolean evictionNecessary(final LRUCache<?, ?> cache) {
      return cache.size() > this.maxSize;
    }

  }

  private static final class LinkedEntry<K, V> {
    
    public final K key;
    public V value;

    public LinkedEntry<K, V> prev;
    public LinkedEntry<K, V> next;

    public LinkedEntry(final K key, final V value) {
      this.key = key;
      this.value = value;
    }

    @Override
    public final boolean equals(final Object other) {
      if (!(other instanceof LinkedEntry)) {
        return false;
      }

      final LinkedEntry<?, ?> otherLinkedEntry = (LinkedEntry<?, ?>) other;
      return this.key.equals(otherLinkedEntry.key);
    }

    @Override
    public final int hashCode() {
      return this.key.hashCode();
    }

  }

  private final EvictionStrategy evictionStrategy;

  private final HashMap<K, LinkedEntry<K, V>> cache;

  private final LinkedEntry<K, V> headSentinel;
  private final LinkedEntry<K, V> tailSentinel;

  public LRUCache(final EvictionStrategy evictionStrategy) {
    this.evictionStrategy = evictionStrategy;

    this.cache = new HashMap<K, LinkedEntry<K, V>>();

    this.headSentinel = new LinkedEntry<K, V>(null, null);
    this.tailSentinel = new LinkedEntry<K, V>(null, null);

    this.headSentinel.next = this.tailSentinel;
    this.tailSentinel.prev = this.headSentinel;
  }

  @Override
  public final void clear() {
    this.cache.clear();
  }

  @Override
  public final boolean containsKey(final Object key) {
    return this.cache.containsKey(key);
  }

  @Override
  public final boolean containsValue(final Object value) {
    for (final LinkedEntry<K, V> linkedEntry : this.cache.values()) {
      final V currentValue = linkedEntry.value;
      if ((value == null && currentValue == null)
          || (value != null && value.equals(currentValue))) {
        return true;
      }
    }

    return false;
  }

  @Override
  public final Set<Map.Entry<K, V>> entrySet() {
    final Set<Map.Entry<K, V>> entrySet = new HashSet<Map.Entry<K, V>>();

    for (final Map.Entry<K, LinkedEntry<K, V>> entry : this.cache.entrySet()) {
      final Map.Entry<K, V> actualEntry =
          new AbstractMap.SimpleEntry<K, V>(entry.getKey(), entry.getValue().value);
      entrySet.add(actualEntry);
    }

    return entrySet;
  }

  private final void deque(final LinkedEntry<K, V> linkedEntry) {
    assert (linkedEntry.prev != null);
    assert (linkedEntry.next != null);

    linkedEntry.prev.next = linkedEntry.next;
    linkedEntry.next.prev = linkedEntry.prev;
  }

  private final void enque(final LinkedEntry<K, V> linkedEntry) {
    this.headSentinel.next.prev = linkedEntry;
    linkedEntry.next = this.headSentinel.next;

    this.headSentinel.next = linkedEntry;
    linkedEntry.prev = this.headSentinel;
  }

  @Override
  public final V get(final Object key) {
    final LinkedEntry<K, V> linkedEntry = this.cache.get(key);

    if (linkedEntry == null) {
      return null;
    } else {
      // update LRU info
      deque(linkedEntry);
      enque(linkedEntry);

      return linkedEntry.value;
    }
  }

  @Override
  public final boolean isEmpty() {
    return this.cache.isEmpty();
  }

  @Override
  public final Set<K> keySet() {
    return this.cache.keySet();
  }

  @Override
  public final V put(final K key, final V value) {
    if (this.cache.containsKey(key)) {
      final LinkedEntry<K, V> linkedEntry = this.cache.get(key);
      final V oldValue = linkedEntry.value;
      linkedEntry.value = value;

      // update LRU info
      deque(linkedEntry);
      enque(linkedEntry);

      return oldValue;
    } else {
      final LinkedEntry<K, V> linkedEntry = new LinkedEntry<K, V>(key, value);
      this.cache.put(key, linkedEntry);

      // update LRU info
      enque(linkedEntry);

      // delete old elements (if necessary)
      if (this.evictionStrategy.evictionNecessary(this)) {
        final LinkedEntry<K, V> entryToRemove = this.tailSentinel.prev;

        this.cache.remove(entryToRemove.key);
        deque(entryToRemove);
      }

      return null;
    }
  }

  @Override
  public final void putAll(final Map<? extends K, ? extends V> map) {
    for (final Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public final V remove(final Object key) {
    final LinkedEntry<K, V> linkedEntry = this.cache.remove(key);
    if (linkedEntry == null) {
      return null;
    }

    // update LRU info
    deque(linkedEntry);

    return linkedEntry.value;
  }

  @Override
  public final int size() {
    return this.cache.size();
  }

  @Override
  public final Collection<V> values() {
    final List<V> values = new ArrayList<V>(this.cache.size());

    for (final LinkedEntry<K, V> linkedEntry : this.cache.values()) {
      values.add(linkedEntry.value);
    }

    return values;
  }

}
