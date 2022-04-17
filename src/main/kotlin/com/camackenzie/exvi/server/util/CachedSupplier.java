package com.camackenzie.exvi.server.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class CachedSupplier<T> implements Supplier<T> {

    @NotNull
    private final Supplier<T> base;

    public CachedSupplier(@NotNull Supplier<T> base) {
        this.base = base;
    }

    @Nullable
    private T cache;

    @Override
    public T get() {
        if (this.cache != null) return cache;
        return this.cache = base.get();
    }
}
