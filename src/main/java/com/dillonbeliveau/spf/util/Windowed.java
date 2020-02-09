package com.dillonbeliveau.spf.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class Windowed<T> implements Iterator<List<T>> {
    private final Collection<T> source;
    private final int windowSize;
    private final int stepSize;

    private int curIndex;

    public Windowed(Collection<T> source, int windowSize, int stepSize) {
        this.source = source;
        this.windowSize = windowSize;
        this.curIndex = 0;
        this.stepSize = stepSize;
    }

    public Windowed(Collection<T> source, int windowSize) {
        this(source, windowSize, 1);
    }

    @Override
    public boolean hasNext() {
        return curIndex + windowSize <= source.size();
    }

    @Override
    public List<T> next() {
        if (hasNext()) {
            List<T> result = source.stream().skip(curIndex).limit(windowSize).collect(Collectors.toList());
            curIndex += stepSize;
            return result;
        }
        else {
            throw new RuntimeException("help");
        }
    }
}
