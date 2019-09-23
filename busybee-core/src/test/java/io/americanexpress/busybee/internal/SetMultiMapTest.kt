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
package io.americanexpress.busybee.internal

import org.assertj.core.api.Java6Assertions.assertThat
import org.assertj.core.api.Java6Assertions.fail
import org.junit.Before
import org.junit.Test

class SetMultiMapTest {
    private lateinit var map: SetMultiMap<Int, String>

    @Before
    fun setUp() {
        map = SetMultiMap()
    }

    @Test
    fun whenValueAdd_thenCanBeRetrieved() {
        map.add(1, "1")
        map.add(1, "2")
        map.add(2, "4")
        map.add(2, "3")

        assertThat(map.values(1))
            .containsExactlyInAnyOrder("1", "2")
        assertThat(map.values(2))
            .containsExactlyInAnyOrder("4", "3")
        assertThat(map.allValues())
            .containsExactlyInAnyOrder("1", "2", "3", "4")
    }

    @Test
    fun whenValueAddedFirstTime_thenReturnTrue() {
        val wasAdded = map.add(1, "2")

        assertThat(wasAdded).isTrue()
    }

    @Test
    fun whenKeyValueAddedSecondTime_thenReturnFalse() {
        map.add(2, "2")
        val resultForSecondAdd = map.add(2, "2")

        assertThat(resultForSecondAdd).isFalse()
    }

    @Test
    fun whenNoValues_thenReturnEmptySet() {
        assertThat(map.values(1)).isEmpty()
    }

    @Test
    fun whenKeyFor_thenReturnValue() {
        map.add(1, "1")
        map.add(2, "3")
        map.add(1, "2")

        assertThat(map.keyFor("1")).isEqualTo(1)
    }

    @Test
    fun whenAddSameValue_thenThrowException() {
        try {
            map.add(1, "1")
            map.add(2, "1")
            fail("Adding the same value twice must throw an exception")
        } catch (e: IllegalStateException) {
            // expected
        }
    }

    @Test
    fun whenRemoveValue_thenCanAddBackWithDifferentKey() {
        map.add(1, "1")
        map.add(1, "3")
        map.removeValue("1")
        map.add(2, "1")
        map.add(1, "2")

        assertThat(map.keyFor("1")).isEqualTo(2)
    }

    @Test
    fun whenRemoveTwice_thenReturnFalseOnSecondTime() {
        map.add(1, "1")
        map.add(2, "2")
        val resultWhenValuePresent = map.removeValue("1")
        val resultAfterValueRemoved = map.removeValue("1")

        assertThat(resultWhenValuePresent).isTrue()
        assertThat(resultAfterValueRemoved).isFalse()
    }

    @Test
    fun whenAllKeys_thenAllKeysArePresent() {
        map.add(1, "1")
        map.add(2, "2")
        map.add(2, "3")
        assertThat(map.allKeys()).containsExactlyInAnyOrder(1, 2)
    }

    @Test
    fun whenAllValues_thenAllValuesArePresent() {
        map.add(1, "1")
        map.add(2, "2")
        map.add(2, "3")

        assertThat(map.allValues()).containsExactlyInAnyOrder("1", "2", "3")
    }

    @Test
    fun valuesReturnsOnlyValuesForTheSpecifiedKey() {
        map.add(1, "1")
        map.add(2, "2")
        map.add(2, "3")

        assertThat(map.values(2)).containsExactlyInAnyOrder("2", "3")
    }

    @Test
    fun whenAllValuesRemoved_ThenIsEmpty() {
        map.add(1, "1")
        map.removeValue("1")

        assertThat(map.hasNoValues()).isTrue()
    }

    @Test
    fun whenHasValue_ThenNotIsEmpty() {
        map.add(1, "1")

        assertThat(map.hasNoValues()).isFalse()
    }

    @Test
    fun whenIterateRemove_ThenIsActuallyRemoved() {
        map.add(1, "A")
        map.add(1, "B")
        map.add(2, "C")
        map.add(2, "C")
        map.add(3, "E")
        map.add(3, "D")
        val stringIterator = map.valuesIterator()
        while (stringIterator.hasNext()) {
            val next = stringIterator.next()
            if (next == "C") {
                stringIterator.remove()
            }
        }

        assertThat(map.allValues()).containsExactlyInAnyOrder("A", "B", "D", "E")
        assertThat(map.allKeys()).containsExactlyInAnyOrder(1, 2, 3)
        assertThat(map.values(2)).isEmpty()
    }

    @Test
    fun whenIterateValuesForKeyAndRemove_ThenIsActuallyRemoved() {
        map.add(1, "A")
        map.add(1, "B")
        map.add(2, "X")
        map.add(2, "Y")
        map.add(3, "E")
        map.add(3, "C")
        map.add(3, "D")
        val stringIterator = map.valuesIterator(3)
        while (stringIterator.hasNext()) {
            val next = stringIterator.next()
            if (next == "C") {
                stringIterator.remove()
            }
        }
        assertThat(map.allValues()).containsExactlyInAnyOrder("A", "B", "X", "E", "Y", "D")
        assertThat(map.allKeys()).containsExactlyInAnyOrder(1, 2, 3)
        assertThat(map.values(3)).containsExactlyInAnyOrder("D", "E")
    }

    @Test
    fun whenIterateValuesForKeyWithNoValues_thenNoExceptionThrown() {
        map.add(1, "A")
        map.add(1, "B")
        map.add(2, "X")
        map.add(2, "Y")
        map.add(3, "E")
        map.add(3, "C")
        map.add(3, "D")
        val stringIterator = map.valuesIterator(4)
        while (stringIterator.hasNext()) {
            val next = stringIterator.next()
            if (next == "C") {
                stringIterator.remove()
            }
        }
        assertThat(map.allValues()).containsExactlyInAnyOrder("A", "C", "B", "X", "E", "Y", "D")
        assertThat(map.allKeys()).containsExactlyInAnyOrder(1, 2, 3)
        assertThat(map.values(3)).containsExactlyInAnyOrder("D", "E", "C")
    }

    @Test
    fun toString_thenPrintSomethingReasonable() {
        map.add(1, "A")
        map.add(1, "B")
        map.add(2, "X")
        map.add(2, "Y")
        map.add(3, "E")
        map.add(3, "C")
        map.add(3, "D")

        assertThat(map.toString()).isEqualTo(
            "\n" +
                    """
               {
               '1'
                ├─ 'A'
                └─ 'B'
               '2'
                ├─ 'X'
                └─ 'Y'
               '3'
                ├─ 'C'
                ├─ 'D'
                └─ 'E'
               }""".trimIndent()
        )

    }
}