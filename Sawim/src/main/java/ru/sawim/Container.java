package ru.sawim;

import java.util.HashMap;

public class Container {

    private static HashMap<Class<?>, Object> _instances = new HashMap<Class<?>, Object>();

    public static <T> void put(Class<T> from, T to) {
        _instances.put(from, to);
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(Class<T> type) {
        return (T) _instances.get(type);
    }

    public static <T> void remove(Class<T> type) {
        _instances.remove(type);
    }
}
