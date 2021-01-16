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

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.logging.Logger;

import io.americanexpress.busybee.internal.NoOpBusyBee;

import static io.americanexpress.busybee.BusyBee.Category.defaultCategory;
import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.util.Locale.US;

/**
 * This is an implementation of the Executor interface that will track operations
 * using the BusyBee. I.e. when this executor is executing something, BusyBee
 * will be "busyWith" all operations in progress that are submitted to this executor.
 * <p>
 * All executed runnables will be execute using the wrapped executor.
 */
public class BusyBeeExecutorWrapper implements Executor {
    private static final Logger log = Logger.getLogger("io.americanexpress.busybee");

    private final BusyBee busyBee;
    private final BusyBee.Category category;
    private final Executor delegate;

    private BusyBeeExecutorWrapper(final BusyBee busyBee, final BusyBee.Category category, final Executor delegate) {
        this.busyBee = busyBee;
        this.delegate = delegate;
        this.category = category;
    }

    @Override
    public void execute(@NonNull final Runnable command) {
        log.info(format(US, "Starting %s on thread %s", command, currentThread()));
        busyBee.busyWith(command, category);
        delegate.execute(() -> {
            try {
                command.run();
            } finally {
                busyBee.completed(command);
            }
        });
    }

    public static BusyBeeExecutorWrapper.Builder with(@NonNull BusyBee busyBee) {
        return new Builder().busyBee(busyBee);
    }

    public static class Builder {
        private BusyBee busyBee;
        private BusyBee.Category category = defaultCategory();
        private Executor wrappedExecutor;

        private Builder() {
        }

        private Builder busyBee(@NonNull final BusyBee busyBee) {
            this.busyBee = busyBee;
            return this;
        }

        public Builder executeInCategory(@NonNull final BusyBee.Category category) {
            this.category = category;
            return this;
        }

        public Builder wrapExecutor(@NonNull final Executor delegate) {
            this.wrappedExecutor = delegate;
            return this;
        }

        public Executor build() {
            if (wrappedExecutor == null) {
                throw new NullPointerException("BusyBeeExecutorWrapper must has an underlying executor to wrap, can't be null.");
            }
            if (busyBee instanceof NoOpBusyBee) {
                return wrappedExecutor;
            } else {
                return new BusyBeeExecutorWrapper(busyBee, category, wrappedExecutor);
            }
        }
    }

}
