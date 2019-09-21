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

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.americanexpress.busybee.BusyBeeExecutorWrapper;

import static io.americanexpress.busybee.BusyBee.Category.GENERAL;
import static io.americanexpress.busybee.BusyBee.Category.NETWORK;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class BusyBeeExecutorWrapperTest {
    @Test
    public void whenExecutorWrappedForNoOp_thenItDoesNotGetWrapped() {
        Executor originalExecutor = Runnable::run;
        final Executor wrappedExecutor
                = BusyBeeExecutorWrapper.with(new NoOpBusyBee())
                .wrapExecutor(originalExecutor)
                .build();

        assertThat(wrappedExecutor == originalExecutor)
                .as("Expected wrapped executor to equal original executor for NoOpBusyBee")
                .isTrue();
    }

    @Test
    public void whenWrappedWithBusyBeeExecutorWrapper_thenBusyBeeSaysItIsBusy() throws Exception {
        ExecutorService mainThread = (ExecutorService) MainThread.singletonExecutor();
        RealBusyBee busyBee = new RealBusyBee(mainThread);

        ExecutorService wrappedThread = Executors.newSingleThreadExecutor();
        final Executor executorWrapper = BusyBeeExecutorWrapper.with(busyBee)
                .executeInCategory(NETWORK)
                .wrapExecutor(wrappedThread)
                .build();

        final CountDownLatch busyLatch = new CountDownLatch(1);
        executorWrapper.execute(() -> {
            try {
                busyLatch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        // shouldn't complete anything because our executor uses NETWORK category
        busyBee.completedEverythingInCategory(GENERAL);

        assertThat(busyBee.isBusy())
                .as("Should be \"busy\" waiting for Latch")
                .isEqualTo(true);
        assertThat(busyBee.isNotBusy())
                .as("Should be \"busy\" waiting for Latch")
                .isEqualTo(false);

        busyLatch.countDown();
        // wrappedThread will post a Runnable to MainThread to tell BusyBee it completed.
        wrappedThread.shutdown();
        wrappedThread.awaitTermination(10, SECONDS);

        // wait for the completion message from the wrapped thread to run on the main thread.
        mainThread.shutdown();
        mainThread.awaitTermination(10, SECONDS);

        assertThat(busyBee.isBusy())
                .as("Should no longer be \"busy\" waiting for Latch")
                .isEqualTo(false);
        assertThat(busyBee.isNotBusy())
                .as("Should no longer be \"busy\" waiting for Latch")
                .isEqualTo(true);
    }
}