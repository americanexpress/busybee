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
package io.americanexpress.busybee

import io.americanexpress.busybee.BusyBee.Category.NETWORK
import io.americanexpress.busybee.BusyBee.NoLongerBusyCallback
import io.americanexpress.busybee.internal.RealBusyBee
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.util.concurrent.Executor

class BusyBeeTest {
    private lateinit var busyBee: BusyBee

    @Before
    fun setUp() {
        busyBee = RealBusyBee(IMMEDIATE)
    }

    @Test
    fun whenBusy_thenIsNotBusyReturnsFalse() {
        busyBee.busyWith(this)

        assertIsBusy(busyBee)
    }

    @Test
    fun whenNetworkBusyAndNotTrackingNetwork_thenIsNotBusy() {
        busyBee.busyWith(this, NETWORK)
        busyBee.ignoreCategory(NETWORK)

        // should not be busy because we aren't tracking network requests
        assertNotBusy(busyBee)
    }

    @Test
    fun whenNetworkBusyAndPayAttention_thenIsBusy() {
        busyBee.busyWith(this, NETWORK)
        busyBee.ignoreCategory(NETWORK)
        busyBee.payAttentionToCategory(NETWORK)

        assertIsBusy(busyBee)
    }

    @Test
    fun whenNetworkBusyAndCompletedNetwork_thenIsNotBusy() {
        busyBee.busyWith(this, NETWORK)
        busyBee.completedEverythingInCategory(NETWORK)

        assertNotBusy(busyBee)
    }

    @Test
    fun whenCompletedNetwork_thenIsNotBusy() {
        busyBee.completedEverythingInCategory(NETWORK)

        assertNotBusy(busyBee)
    }

    @Test
    fun whenNetworkBusy_thenIsNotBusyReturnsFalse() {
        busyBee.busyWith(this, NETWORK)

        assertIsBusy(busyBee)
    }

    @Test
    fun whenCompleted_thenIsNotBusyReturnsTrue() {
        busyBee.busyWith(this)
        busyBee.completed(this)

        assertNotBusy(busyBee)
    }

    // so we can pass in null as if we are calling from Java
    private fun nullForTesting(): Any = null!!

    @Test
    fun whenBusyWithNull_thenThrowNullPointerException() {
        try {
            busyBee.busyWith(nullForTesting())
            fail("busyWith(null) should throw NullPointerException")
        } catch (e: NullPointerException) {
            // expected
        }
    }

    @Test
    fun whenCompletedNull_thenThrowNullPointerException() {
        try {
            busyBee.completed(nullForTesting())
            fail("completed(null) should throw NullPointerException")
        } catch (e: NullPointerException) {
            // expected
        }
    }

    @Test
    fun whenBusyWithSomething_thenNameIncludesSomething() {
        busyBee.busyWith("some thing")

        assertThat(busyBee.name).contains("some thing")
    }

    @Test
    fun whenCompletedEverything_thenIsNotBusyReturnsTrue() {
        busyBee.busyWith(this)
        busyBee.busyWith(Any())
        busyBee.busyWith(Any())
        busyBee.completedEverything()

        assertNotBusy(busyBee)
    }

    @Test
    fun whenCompleteString_thenOnlyNonStringOperationsRemain() {
        val operation = Any()
        busyBee.busyWith(operation)
        busyBee.busyWith("Network 1", NETWORK)
        busyBee.busyWith("General")
        busyBee.completedEverythingMatching { o: Any? -> o is String }

        // still busy with 1 non-String operation
        assertIsBusy(busyBee)

        busyBee.completed(operation)

        assertNotBusy(busyBee)
    }

    @Test
    fun whenCompleted_thenNotifyCallback() {
        busyBee.busyWith("Op1")
        val noLongerBusyCallback = mock(NoLongerBusyCallback::class.java)
        busyBee.registerNoLongerBusyCallback(noLongerBusyCallback)
        busyBee.completed("Op1")

        verify(noLongerBusyCallback).noLongerBusy()
    }

    @Test
    fun whenIgnoreRemainingOperations_thenNotifyCallback() {
        busyBee.payAttentionToCategory(NETWORK)
        busyBee.busyWith("Op1")
        busyBee.busyWith("Network", NETWORK)
        val noLongerBusyCallback = mock(NoLongerBusyCallback::class.java)
        busyBee.registerNoLongerBusyCallback(noLongerBusyCallback)
        busyBee.completed("Op1")
        busyBee.ignoreCategory(NETWORK)

        verify(noLongerBusyCallback).noLongerBusy()
    }

    @Test
    fun whenCompletedOnlySome_thenStillBusy() {
        val o1 = Any()
        val o2 = Any()
        assertNotBusy(busyBee)

        busyBee.busyWith(o1)
        assertIsBusy(busyBee)

        busyBee.busyWith(o2)
        assertIsBusy(busyBee)

        busyBee.completed(o2)
        assertIsBusy(busyBee)

        busyBee.completed(o1)
        assertNotBusy(busyBee)
    }

    companion object {
        private val IMMEDIATE: Executor = Executor { obj: Runnable -> obj.run() }

        private fun assertNotBusy(busyBee: BusyBee) {
            assertTrue(busyBee.isNotBusy)
        }

        private fun assertIsBusy(busyBee: BusyBee) {
            assertFalse(busyBee.isNotBusy)
        }
    }
}