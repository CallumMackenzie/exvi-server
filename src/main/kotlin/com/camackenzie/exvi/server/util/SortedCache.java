package com.camackenzie.exvi.server.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public class SortedCache<T> {

    private final List<T> cache;
    private final int maxSize;
    private final Comparator<T> cacheComparator;

    public SortedCache(Comparator<T> cacheComparator, int maxSize, List<T> base) {
        this.cache = base;
        this.maxSize = maxSize;
        this.cacheComparator = cacheComparator;
    }

    public SortedCache(Comparator<T> cacheComparator, int maxSize) {
        this(cacheComparator, maxSize, new ArrayList<>(maxSize));
    }

    public T matchFirst(Predicate<T> matcher) {
        for (var item : cache)
            if (matcher.test(item))
                return item;
        return null;
    }

    public T removeFirst(Predicate<T> matcher) {
        for (int i = 0; i < cache.size(); ++i)
            if (matcher.test(cache.get(i)))
                return cache.remove(i);
        return null;
    }

    public void cache(T entry) {
        // Remove least used cached users
        if (cache.size() > maxSize)
            cache.subList(maxSize, cache.size()).clear();
        // Remove entry if it is cached
        cache.remove(entry);
        // Insert item by its accesses
        int toInsert = Collections.binarySearch(cache, entry, cacheComparator);
        if (toInsert < 0) toInsert = -(toInsert + 1);
        cache.add(toInsert, entry);
    }

    public void clear() {
        this.cache.clear();
    }

}
