package io.mbrc.newsfetch.util;

import lombok.Getter;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

@Getter
public class KeyValuePair<K, V> {
    private K key;
    private V value;

    private KeyValuePair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public void acceptedBy(BiConsumer<K, V> consumer) {
        consumer.accept(key, value);
    }

    public <R> R appliedTo(BiFunction<K, V, R> function) {
        return function.apply(key, value);
    }

    public static <K, V> KeyValuePair<K, V> pairOf(K key, V value) {
        return new KeyValuePair(key, value);
    }
}
