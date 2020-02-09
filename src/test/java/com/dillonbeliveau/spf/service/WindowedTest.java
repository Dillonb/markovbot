package com.dillonbeliveau.spf.service;

import com.dillonbeliveau.spf.util.Windowed;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WindowedTest {

    private <E> void check(Windowed<String> iter, ImmutableList<ImmutableList<String>> expected) {
        for (ImmutableList<String> expectedGroup : expected) {
            assertTrue(iter.hasNext());
            List<String> actualGroup = iter.next();

            assertEquals(expectedGroup.size(), actualGroup.size());

            for (int i = 0; i < expectedGroup.size(); i++) {
                assertEquals(expectedGroup.get(i), actualGroup.get(i));
            }

            System.out.println(Joiner.on(",").join(expectedGroup));
        }

        assertFalse(iter.hasNext());
    }

    @Test
    public void asdf() {
        Windowed<String> two = new Windowed<>(ImmutableList.of("a", "b", "c", "d", "e", "f", "g"), 2);
        Windowed<String> three = new Windowed<>(ImmutableList.of("a", "b", "c", "d", "e", "f", "g"), 3);

        check(two, ImmutableList.of(
                ImmutableList.of("a","b"),
                ImmutableList.of("b","c"),
                ImmutableList.of("c","d"),
                ImmutableList.of("d","e"),
                ImmutableList.of("e","f"),
                ImmutableList.of("f","g")));

        check(three, ImmutableList.of(
                ImmutableList.of("a","b","c"),
                ImmutableList.of("b","c","d"),
                ImmutableList.of("c","d","e"),
                ImmutableList.of("d","e","f"),
                ImmutableList.of("e","f","g")));
    }


}