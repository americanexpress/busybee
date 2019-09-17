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
package io.americanexpress.busybee;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Executor;

import io.americanexpress.busybee.internal.RealBusyBee;

import static io.americanexpress.busybee.BusyBee.Category.NETWORK;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class BusyBeeTest {

    private BusyBee busyBee;

    private static final Executor IMMEDIATE = Runnable::run;

    @Before
    public void setUp() {
        busyBee = new RealBusyBee(IMMEDIATE);
    }

    @Test
    public void whenBusy_thenIsNotBusyReturnsFalse() {
        busyBee.busyWith(this);
        assertIsBusy(busyBee);
    }

    @Test
    public void whenNetworkBusyAndNotTrackingNetwork_thenIsNotBusy() {
        busyBee.busyWith(this, NETWORK);
        busyBee.ignoreCategory(NETWORK);

        // should not be busy because we aren't tracking network requests
        assertNotBusy(busyBee);
    }

    @Test
    public void whenNetworkBusyAndPayAttention_thenIsBusy() {
        busyBee.busyWith(this, NETWORK);
        busyBee.ignoreCategory(NETWORK);
        busyBee.payAttentionToCategory(NETWORK);

        assertIsBusy(busyBee);
    }

    @Test
    public void whenNetworkBusyAndCompletedNetwork_thenIsNotBusy() {
        busyBee.busyWith(this, NETWORK);
        busyBee.completedEverythingInCategory(NETWORK);

        assertNotBusy(busyBee);
    }


    @Test
    public void whenCompletedNetwork_thenIsNotBusy() {
        busyBee.completedEverythingInCategory(NETWORK);

        assertNotBusy(busyBee);
    }

    @Test
    public void whenNetworkBusy_thenIsNotBusyReturnsFalse() {
        busyBee.busyWith(this, NETWORK);

        assertIsBusy(busyBee);
    }

    @Test
    public void whenCompleted_thenIsNotBusyReturnsTrue() {
        busyBee.busyWith(this);
        busyBee.completed(this);

        assertNotBusy(busyBee);
    }

    @Test
    public void whenBusyWithNull_thenThrowNullPointerException() {
        try {
            //noinspection ConstantConditions testing that null is not accepted
            busyBee.busyWith(null);
            fail("busyWith(null) should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
    }


    @Test
    public void whenCompletedNull_thenThrowNullPointerException() {
        try {
            //noinspection ConstantConditions testing that null is not accepted
            busyBee.completed(null);
            fail("completed(null) should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
    }


    @Test
    public void whenBusyWithSomething_thenNameIncludesSomething() {
        busyBee.busyWith("some thing");
        assertThat(busyBee.getName()).contains("some thing");
    }

    @Test
    public void whenCompletedEverything_thenIsNotBusyReturnsTrue() {
        busyBee.busyWith(this);
        busyBee.busyWith(new Object());
        busyBee.busyWith(new Object());
        busyBee.completedEverything();
        assertNotBusy(busyBee);
    }

    @Test
    public void whenCompleteString_thenOnlyNonStringOperationsRemain() {
        Object operation = new Object();
        busyBee.busyWith(operation);
        busyBee.busyWith("Network 1", NETWORK);
        busyBee.busyWith("General");
        busyBee.completedEverythingMatching(o -> o instanceof String);
        // still busy with 1 non-String operation
        assertIsBusy(busyBee);

        busyBee.completed(operation);
        assertNotBusy(busyBee);
    }

    @Test
    public void whenCompleted_thenNotifyCallback() {
        busyBee.busyWith("Op1");

        final BusyBee.NoLongerBusyCallback noLongerBusyCallback
                = mock(BusyBee.NoLongerBusyCallback.class);
        busyBee.registerNoLongerBusyCallback(noLongerBusyCallback);
        busyBee.completed("Op1");

        verify(noLongerBusyCallback).noLongerBusy();
    }

    @Test
    public void whenIgnoreRemainingOperations_thenNotifyCallback() {
        busyBee.payAttentionToCategory(NETWORK);

        busyBee.busyWith("Op1");
        busyBee.busyWith("Network", NETWORK);

        final BusyBee.NoLongerBusyCallback noLongerBusyCallback
                = mock(BusyBee.NoLongerBusyCallback.class);
        busyBee.registerNoLongerBusyCallback(noLongerBusyCallback);
        busyBee.completed("Op1");
        busyBee.ignoreCategory(NETWORK);

        verify(noLongerBusyCallback).noLongerBusy();
    }


    @Test
    public void whenCompletedOnlySome_thenStillBusy() {
        Object o1 = new Object();
        Object o2 = new Object();
        assertNotBusy(busyBee);
        busyBee.busyWith(o1);
        assertIsBusy(busyBee);
        busyBee.busyWith(o2);
        assertIsBusy(busyBee);
        busyBee.completed(o2);
        assertIsBusy(busyBee);
        busyBee.completed(o1);
        assertNotBusy(busyBee);
    }

    private static void assertNotBusy(final BusyBee busyBee) {
        assertTrue(busyBee.isNotBusy());
    }

    private static void assertIsBusy(final BusyBee busyBee) {
        assertFalse(busyBee.isNotBusy());
    }
}
