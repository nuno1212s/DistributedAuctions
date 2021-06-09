package me.evmanu.util;

import lombok.EqualsAndHashCode;

import java.util.function.BiFunction;
import java.util.function.Function;

@EqualsAndHashCode
public class Pair<K, T> {

    private K key;

    private T value;

    private Pair(K key, T value) {
        this.key = key;
        this.value = value;
    }

    public K getKey() {
        return key;
    }

    public T getValue() {
        return value;
    }

    public void setKey(K key) {
        this.key = key;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public <S> Pair<S, T> mapKey(Function<K, S> function) {
        return Pair.of(function.apply(getKey()), getValue());
    }

    public <S> Pair<K, S> mapValue(Function<T, S> function) {
        return Pair.of(getKey(), function.apply(getValue()));
    }

    public <S> S apply(BiFunction<K, T, S> function) {
        return function.apply(getKey(), getValue());
    }

    public static <K, T> Pair<K, T> of(K key, T value) {
        return new Pair<>(key, value);
    }

    @Override
    public String toString() {
        return "{" + key.toString() + ", " + value.toString() + "}";
    }
}