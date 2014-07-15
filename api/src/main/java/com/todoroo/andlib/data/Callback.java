package com.todoroo.andlib.data;

public interface Callback<T> {
    void apply(T entry);
}
