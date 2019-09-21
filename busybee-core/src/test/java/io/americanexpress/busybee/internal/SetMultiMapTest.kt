/*
 * Copyright 2019 American Express Travel Related Services Company, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.americanexpress.busybee.internal;

import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.fail;

public class SetMultiMapTest {

    private SetMultiMap<Integer, String> map;

    @Before
    public void setUp() {
        map = new SetMultiMap<>();
    }

    @Test
    public void whenValueAdd_thenCanBeRetrieved() {
        final SetMultiMap<Integer, String> map = this.map;
        map.add(1, "1");
        map.add(1, "2");
        map.add(2, "4");
        map.add(2, "3");

        assertThat(map.values(1)).containsExactlyInAnyOrder("1", "2");
        assertThat(map.values(2)).containsExactlyInAnyOrder("4", "3");
        assertThat(map.allValues()).containsExactlyInAnyOrder("1", "2", "3", "4");
    }

    @Test
    public void whenValueAddedFirstTime_thenReturnTrue() {
        final boolean wasAdded = map.add(1, "2");

        assertThat(wasAdded).isTrue();
    }

    @Test
    public void whenKeyValueAddedSecondTime_thenReturnFalse() {
        map.add(2, "2");
        final boolean resultForSecondAdd = map.add(2, "2");

        assertThat(resultForSecondAdd).isFalse();
    }

    @Test
    public void whenNoValues_thenReturnEmptySet() {
        assertThat(map.values(1)).isEmpty();
    }

    @Test
    public void whenKeyFor_thenReturnValue() {
        map.add(1, "1");
        map.add(2, "3");
        map.add(1, "2");

        assertThat(map.keyFor("1")).isEqualTo(1);
    }


    @Test
    public void whenAddSameValue_thenThrowException() {
        try {
            map.add(1, "1");
            map.add(2, "1");
            fail("Adding the same value twice must throw an exception");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    @Test
    public void whenRemoveValue_thenCanAddBackWithDifferentKey() {
        map.add(1, "1");
        map.add(1, "3");
        map.removeValue("1");
        map.add(2, "1");
        map.add(1, "2");

        assertThat(map.keyFor("1")).isEqualTo(2);
    }

    @Test
    public void whenRemoveTwice_thenReturnFalseOnSecondTime() {
        map.add(1, "1");
        map.add(2, "2");
        final boolean resultWhenValuePresent = map.removeValue("1");
        final boolean resultAfterValueRemoved = map.removeValue("1");

        assertThat(resultWhenValuePresent).isTrue();
        assertThat(resultAfterValueRemoved).isFalse();
    }

    @Test
    public void whenAllKeys_thenAllKeysArePresent() {
        map.add(1, "1");
        map.add(2, "2");
        map.add(2, "3");

        assertThat(map.allKeys()).containsExactlyInAnyOrder(1, 2);
    }

    @Test
    public void whenAllValues_thenAllValuesArePresent() {
        map.add(1, "1");
        map.add(2, "2");
        map.add(2, "3");

        assertThat(map.allValues()).containsExactlyInAnyOrder("1", "2", "3");
    }

    @Test
    public void valuesReturnsOnlyValuesForTheSpecifiedKey() {
        map.add(1, "1");
        map.add(2, "2");
        map.add(2, "3");

        assertThat(map.values(2)).containsExactlyInAnyOrder("2", "3");
    }

    @Test
    public void whenAllValuesRemoved_ThenIsEmpty() {
        map.add(1, "1");
        map.removeValue("1");

        assertThat(map.hasNoValues()).isTrue();
    }

    @Test
    public void whenHasValue_ThenNotIsEmpty() {
        map.add(1, "1");

        assertThat(map.hasNoValues()).isFalse();
    }

    @Test
    public void whenIterateRemove_ThenIsActuallyRemoved() {
        map.add(1, "A");
        map.add(1, "B");
        map.add(2, "C");
        map.add(2, "C");
        map.add(3, "E");
        map.add(3, "D");

        final Iterator<String> stringIterator = map.valuesIterator();
        while (stringIterator.hasNext()) {
            String next = stringIterator.next();
            if (next.equals("C")) {
                stringIterator.remove();
            }
        }

        assertThat(map.allValues()).containsExactlyInAnyOrder("A", "B", "D", "E");
        assertThat(map.allKeys()).containsExactlyInAnyOrder(1, 2, 3);
        assertThat(map.values(2)).isEmpty();
    }

    @Test
    public void whenIterateValuesForKeyAndRemove_ThenIsActuallyRemoved() {
        map.add(1, "A");
        map.add(1, "B");
        map.add(2, "X");
        map.add(2, "Y");
        map.add(3, "E");
        map.add(3, "C");
        map.add(3, "D");

        final Iterator<String> stringIterator = map.valuesIterator(3);
        while (stringIterator.hasNext()) {
            String next = stringIterator.next();
            if (next.equals("C")) {
                stringIterator.remove();
            }
        }

        assertThat(map.allValues()).containsExactlyInAnyOrder("A", "B", "X", "E", "Y", "D");
        assertThat(map.allKeys()).containsExactlyInAnyOrder(1, 2, 3);
        assertThat(map.values(3)).containsExactlyInAnyOrder("D", "E");
    }

    @Test
    public void whenIterateValuesForKeyWithNoValues_thenNoExceptionThrown() {
        map.add(1, "A");
        map.add(1, "B");
        map.add(2, "X");
        map.add(2, "Y");
        map.add(3, "E");
        map.add(3, "C");
        map.add(3, "D");

        final Iterator<String> stringIterator = map.valuesIterator(4);
        while (stringIterator.hasNext()) {
            String next = stringIterator.next();
            if (next.equals("C")) {
                stringIterator.remove();
            }
        }

        assertThat(map.allValues()).containsExactlyInAnyOrder("A", "C", "B", "X", "E", "Y", "D");
        assertThat(map.allKeys()).containsExactlyInAnyOrder(1, 2, 3);
        assertThat(map.values(3)).containsExactlyInAnyOrder("D", "E", "C");
    }

    @Test
    public void toString_thenPrintSomethingReasonable() {
        map.add(1, "A");
        map.add(1, "B");
        map.add(2, "X");
        map.add(2, "Y");
        map.add(3, "E");
        map.add(3, "C");
        map.add(3, "D");

        assertThat(map.toString()).isEqualTo("\n{\n"
                + "'1'\n"
                + " ├─ 'A'\n"
                + " └─ 'B'\n"
                + "'2'\n"
                + " ├─ 'X'\n"
                + " └─ 'Y'\n"
                + "'3'\n"
                + " ├─ 'C'\n"
                + " ├─ 'D'\n"
                + " └─ 'E'\n"
                + "}");
    }
}
