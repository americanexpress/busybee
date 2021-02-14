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
import java.util.concurrent.Executor
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import java.util.logging.Logger
import kotlin.concurrent.withLock

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
class RealBusyBee(private val completedOnThread: Executor) : BusyBee {
    @GuardedBy("lock")
    private val operationsInProgress = SetMultiMap<BusyBee.Category, Any>()

    @GuardedBy("lock")
    private val currentlyTrackedCategories = EnumSet.allOf(BusyBee.Category::class.java)

    @GuardedBy("lock")
    private val noLongerBusyCallbacks: MutableList<NoLongerBusyCallback> =
        ArrayList(1) // Espresso use case will only have 1 callback
    private val lock: Lock = ReentrantLock()
    private val defaultCategory: BusyBee.Category = BusyBee.Category.defaultCategory()
    override fun getName(): String = lock.withLock {
        "${this.javaClass.simpleName}@${System.identityHashCode(this)} with operations: $operationsInProgress"
    }

    override fun busyWith(operation: Any) {
        busyWith(operation, defaultCategory)
    }

    override fun busyWith(operation: Any, category: BusyBee.Category) {
        lock.withLock {
            if (operationsInProgress.add(category, operation)) {
                log.info("busyWith -> [$operation] was added to active operations in category $category")
            }
        }
    }

    override fun registerNoLongerBusyCallback(noLongerBusyCallback: NoLongerBusyCallback) {
        lock.withLock {
            noLongerBusyCallbacks.add(noLongerBusyCallback)
        }
    }

    override fun payAttentionToCategory(category: BusyBee.Category) {
        lock.withLock {
            log.info("Paying attention to category: $category")
            currentlyTrackedCategories.add(category)
        }
    }

    override fun ignoreCategory(category: BusyBee.Category) {
        lock.withLock {
            log.info("Ignoring category: $category")
            val wasBusyBefore = isBusy()
            val wasRemoved = currentlyTrackedCategories.remove(category)
            val notBusyNow = isNotBusy()
            if (wasRemoved && wasBusyBefore && notBusyNow) {
                notifyNoLongerBusyCallbacks()
            }
        }
    }

    override fun completedEverythingInCategory(category: BusyBee.Category) {
        completedOnThread.execute(object : Runnable {
            override fun run() {
                lock.withLock {
                    val iterator = operationsInProgress.valuesIterator(category)
                    while (iterator.hasNext()) {
                        val next = iterator.next()
                        completeOnCurrentThread(next, iterator)
                    }
                }
            }

            override fun toString() = "completedEverythingInCategory($category)"
        })
    }

    override fun completedEverything() {
        completedOnThread.execute(object : Runnable {
            override fun run() {
                lock.withLock {
                    val iterator = operationsInProgress.valuesIterator()
                    while (iterator.hasNext()) {
                        val next = iterator.next()
                        completeOnCurrentThread(next, iterator)
                    }
                }
            }

            override fun toString() = "completedEverything()"
        })
    }

    override fun completedEverythingMatching(matcher: OperationMatcher) {
        completedOnThread.execute(object : Runnable {
            override fun run() {
                lock.withLock {
                    val iterator = operationsInProgress.valuesIterator()
                    while (iterator.hasNext()) {
                        val next = iterator.next()
                        if (matcher.matches(next)) {
                            completeOnCurrentThread(next, iterator)
                        }
                    }
                }
            }

            override fun toString() = "completedEverythingMatching($matcher)"
        })
    }

    override fun completed(operation: Any) {
        completedOnThread.execute(object : Runnable {
            override fun run() {
                completeOnCurrentThread(operation, null)
            }

            override fun toString() = "completed($operation)"
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
    private fun completeOnCurrentThread(operation: Any, iterator: MutableIterator<Any?>?) {
        lock.withLock {
            val wasRemoved = if (iterator != null) {
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
            if (wasRemoved && isNotBusy()) {
                notifyNoLongerBusyCallbacks()
            }
        }
    }

    private fun notifyNoLongerBusyCallbacks() {
        for (noLongerBusyCallback in noLongerBusyCallbacks) {
            log.info("All operations are now finished, we are now idle")
            noLongerBusyCallback.noLongerBusy()
        }
    }

    override fun isNotBusy(): Boolean {
        lock.withLock {
            for (category in currentlyTrackedCategories) {
                if (isBusyWithAnythingIn(category)) return false
            }
            return true
        }
    }

    override fun isBusy(): Boolean = !isNotBusy()

    private fun isBusyWithAnythingIn(category: BusyBee.Category) = lock.withLock {
        operationsInProgress.values(category).isNotEmpty()
    }

    override fun toStringVerbose(): String = lock.withLock {
        val operations = operationsInProgress
        buildString {
            appendLine(
                """
                ***********************
                **BusyBee Information**
                ***********************
                """.trimIndent()
            )
            try {
                appendLine(
                    """
                    Total Operations: ${operations.allValues().size}
                    List of operations in progress:
                    ****************************
                    """.trimIndent()
                )
                for (category in operations.allKeys()) {
                    appendLine(
                        """
                        CATEGORY: ======= ${category.name}  =======
                    """.trimIndent()
                    )
                    for (operation in operations.values(category)) {
                        appendLine(operation)
                    }
                }
            } catch (e: Exception) {
                appendLine(
                    """
                    ${e.message}
                    ****!!!!FAILED TO GET LIST OF IN PROGRESS OPERATIONS!!!!****
                    """.trimIndent()
                )
            }
            appendLine("****************************")
        }
    }

    companion object {
        private val log = Logger.getLogger("io.americanexpress.busybee")
    }
}