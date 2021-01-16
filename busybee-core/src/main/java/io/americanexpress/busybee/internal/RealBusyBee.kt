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

import androidx.annotation.GuardedBy
import io.americanexpress.busybee.BusyBee
import io.americanexpress.busybee.BusyBee.NoLongerBusyCallback
import io.americanexpress.busybee.BusyBee.OperationMatcher
import java.util.ArrayList
import java.util.EnumSet
import java.util.Locale
import java.util.concurrent.Executor
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import java.util.logging.Logger

/**
 * This allows the app to let Espresso (test framework) know when it is busy and Espresso should wait.
 * It is not directly tied to Espresso and could be used in any case where you need to know if the app is "busy".
 *
 *
 * Call busyWith when you start being "busy"
 * Call completed when you stop being "busy" and start being "idle".
 *
 *
 * Generally, you should call completed from a finally block.
 *
 *
 * Espresso will wait for the app to be "idle" (i.e. not busy).
 *
 *
 * Proper use of the BusyBee will avoid having to "wait" or "sleep" in tests.
 * BE SURE NOT BE "BUSY" LONGER THAN NECESSARY, otherwise it will slow down your tests.
 */
class RealBusyBee(private val completedOnThread: Executor?) : BusyBee {
    @GuardedBy("lock")
    private val operationsInProgress = SetMultiMap<BusyBee.Category, Any>()

    @GuardedBy("lock")
    private val currentlyTrackedCategories = EnumSet.allOf(
        BusyBee.Category::class.java
    )

    @GuardedBy("lock")
    private val noLongerBusyCallbacks: MutableList<NoLongerBusyCallback> =
        ArrayList(1) // Espresso use case will only have 1 callback
    private val lock: Lock = ReentrantLock()
    private val defaultCategory: BusyBee.Category = BusyBee.Category.Companion.defaultCategory()
    override val name: String
        get() {
            lock.lock()
            return try {
                String.format(
                    Locale.US, this.javaClass.simpleName + "@%d with operations: %s",
                    System.identityHashCode(this),
                    operationsInProgress
                )
            } finally {
                lock.unlock()
            }
        }

    override fun busyWith(operation: Any) {
        busyWith(operation, defaultCategory)
    }

    override fun busyWith(operation: Any, category: BusyBee.Category) {
        if (operation == null) {
            throw NullPointerException("Can not be `busyWith` null, operation must be non-null")
        }
        lock.lock()
        val wasAdded: Boolean
        try {
            wasAdded = operationsInProgress.add(category, operation)
            if (wasAdded) {
                log.info("busyWith -> [$operation] was added to active operations in category $category")
            }
        } finally {
            lock.unlock()
        }
    }

    override fun registerNoLongerBusyCallback(noLongerBusyCallback: NoLongerBusyCallback) {
        lock.lock()
        try {
            noLongerBusyCallbacks.add(noLongerBusyCallback)
        } finally {
            lock.unlock()
        }
    }

    override fun payAttentionToCategory(category: BusyBee.Category) {
        lock.lock()
        try {
            log.info("Paying attention to category: $category")
            currentlyTrackedCategories.add(category)
        } finally {
            lock.unlock()
        }
    }

    override fun ignoreCategory(category: BusyBee.Category) {
        lock.lock()
        try {
            log.info("Ignoring category: $category")
            val wasBusyBefore = isBusy
            val wasRemoved = currentlyTrackedCategories.remove(category)
            val notBusyNow = isNotBusy
            if (wasRemoved && wasBusyBefore && notBusyNow) {
                notifyNoLongerBusyCallbacks()
            }
        } finally {
            lock.unlock()
        }
    }

    override fun completedEverythingInCategory(category: BusyBee.Category) {
        completedOnThread!!.execute(object : Runnable {
            override fun run() {
                lock.lock()
                try {
                    val iterator = operationsInProgress.valuesIterator(category)
                    while (iterator.hasNext()) {
                        val next = iterator.next()
                        completeOnCurrentThread(next, iterator)
                    }
                } finally {
                    lock.unlock()
                }
            }

            override fun toString(): String {
                return "completedEverythingInCategory($category)"
            }
        })
    }

    override fun completedEverything() {
        completedOnThread!!.execute(object : Runnable {
            override fun run() {
                lock.lock()
                try {
                    val iterator = operationsInProgress.valuesIterator()
                    while (iterator.hasNext()) {
                        val next = iterator.next()
                        completeOnCurrentThread(next, iterator)
                    }
                } finally {
                    lock.unlock()
                }
            }

            override fun toString(): String {
                return "completedEverything()"
            }
        })
    }

    override fun completedEverythingMatching(matcher: OperationMatcher) {
        completedOnThread!!.execute(object : Runnable {
            override fun run() {
                lock.lock()
                try {
                    val iterator = operationsInProgress.valuesIterator()
                    while (iterator.hasNext()) {
                        val next = iterator.next()
                        if (matcher.matches(next)) {
                            completeOnCurrentThread(next, iterator)
                        }
                    }
                } finally {
                    lock.unlock()
                }
            }

            override fun toString(): String {
                return "completedEverythingMatching($matcher)"
            }
        })
    }

    override fun completed(operation: Any) {
        completedOnThread!!.execute(object : Runnable {
            override fun run() {
                completeOnCurrentThread(operation, null)
            }

            override fun toString(): String {
                return "completed($operation)"
            }
        })
    }

    /**
     * Precondition: Iterator must be pointing to the operation passed in (or iterator must be null).
     *
     *
     * This method "completes" the operation on the current thread.
     *
     * @param operation the operation to be completed
     * @param iterator  must be pointing to operation
     */
    private fun completeOnCurrentThread(operation: Any?, iterator: MutableIterator<Any?>?) {
        if (operation == null) {
            throw NullPointerException("null can not be `completed` null, operation must be non-null")
        }
        lock.lock()
        val wasRemoved: Boolean
        try {
            wasRemoved = if (iterator != null) {
                // if the collection is being iterated,
                // then we HAVE to use the iterator for removal to avoid ConcurrentModificationException
                iterator.remove()
                true
            } else {
                operationsInProgress.removeValue(operation)
            }
            if (wasRemoved) {
                log.info("completed -> [$operation] was removed from active operations")
            }
            if (wasRemoved && isNotBusy) {
                notifyNoLongerBusyCallbacks()
            }
        } finally {
            lock.unlock()
        }
    }

    private fun notifyNoLongerBusyCallbacks() {
        for (noLongerBusyCallback in noLongerBusyCallbacks) {
            log.info("All operations are now finished, we are now idle")
            noLongerBusyCallback.noLongerBusy()
        }
    }

    override val isNotBusy: Boolean
        get() {
            lock.lock()
            return try {
                for (category in currentlyTrackedCategories) {
                    if (isBusyWithAnythingIn(category)) {
                        return false
                    }
                }
                true
            } finally {
                lock.unlock()
            }
        }
    override val isBusy: Boolean
        get() = !isNotBusy

    private fun isBusyWithAnythingIn(category: BusyBee.Category): Boolean {
        lock.lock()
        return try {
            !operationsInProgress.values(category).isEmpty()
        } finally {
            lock.unlock()
        }
    }

    override fun toStringVerbose(): String {
        return try {
            lock.lock()
            val operations = operationsInProgress
            val sb = StringBuilder()
                .append("\n***********************")
                .append("\n**BusyBee Information**")
                .append("\n***********************")
            try {
                sb.append("\nTotal Operations:")
                    .append(operations.allValues().size)
                    .append("\nList of operations in progress:")
                    .append("\n****************************")
                for (category in operations.allKeys()) {
                    sb.append("\nCATEGORY: ======= ").append(category.name).append(" =======")
                    for (operation in operations.values(category)) {
                        sb.append("\n").append(operation.toString())
                    }
                }
            } catch (e: Exception) {
                sb.append(e.message)
                sb.append("\n****!!!!FAILED TO GET LIST OF IN PROGRESS OPERATIONS!!!!****")
            }
            sb.append("\n****************************\n").toString()
        } finally {
            lock.unlock()
        }
    }

    companion object {
        private val log = Logger.getLogger("io.americanexpress.busybee")
    }
}