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

import io.americanexpress.busybee.BusyBee.Category.Companion.defaultCategory
import io.americanexpress.busybee.internal.NoOpBusyBee
import java.util.concurrent.Executor
import java.util.logging.Logger

/**
 * This is an implementation of the Executor interface that will track operations
 * using the BusyBee. I.e. when this executor is executing something, BusyBee
 * will be "busyWith" all operations in progress that are submitted to this executor.
 *
 *
 * All executed runnables will be execute using the wrapped executor.
 */
class BusyBeeExecutorWrapper private constructor(
    private val busyBee: BusyBee,
    private val category: BusyBee.Category,
    private val delegate: Executor
) : Executor {
    override fun execute(command: Runnable) {
        log.info("Starting $command on thread ${Thread.currentThread()}")
        busyBee.busyWith(command, category)
        delegate.execute {
            try {
                command.run()
            } finally {
                busyBee.completed(command)
            }
        }
    }

    class Builder {
        private lateinit var busyBee: BusyBee
        private var category: BusyBee.Category = defaultCategory()
        private var wrappedExecutor: Executor? = null
        fun busyBee(busyBee: BusyBee): Builder {
            this.busyBee = busyBee
            return this
        }

        fun executeInCategory(category: BusyBee.Category): Builder {
            this.category = category
            return this
        }

        fun wrapExecutor(delegate: Executor): Builder {
            wrappedExecutor = delegate
            return this
        }

        fun build(): Executor {
            val wrappedExecutorLocal = wrappedExecutor
                ?: throw NullPointerException("BusyBeeExecutorWrapper must have an underlying executor to wrap, can't be null.")
            return if (busyBee is NoOpBusyBee) {
                wrappedExecutorLocal
            } else {
                BusyBeeExecutorWrapper(busyBee, category, wrappedExecutorLocal)
            }
        }
    }

    companion object {
        private val log = Logger.getLogger("io.americanexpress.busybee")
        fun with(busyBee: BusyBee): Builder = Builder().busyBee(busyBee)
    }
}