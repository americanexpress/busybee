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

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import io.americanexpress.busybee.BusyBee;

import static io.americanexpress.busybee.BusyBee.Category.defaultCategory;
import static java.lang.String.format;
import static java.lang.System.identityHashCode;
import static java.util.Locale.US;

/**
 * This allows the app to let Espresso (test framework) know when it is busy and Espresso should wait.
 * It is not directly tied to Espresso and could be used in any case where you need to know if the app is "busy".
 * <p>
 * Call busyWith when you start being "busy"
 * Call completed when you stop being "busy" and start being "idle".
 * <p>
 * Generally, you should call completed from a finally block.
 * <p>
 * Espresso will wait for the app to be "idle" (i.e. not busy).
 * <p>
 * Proper use of the BusyBee will avoid having to "wait" or "sleep" in tests.
 * BE SURE NOT BE "BUSY" LONGER THAN NECESSARY, otherwise it will slow down your tests.
 */
public class RealBusyBee implements BusyBee {
    private static final Logger log = Logger.getLogger("io.americanexpress.busybee");

    @GuardedBy("lock")
    private final SetMultiMap<Category, Object> operationsInProgress = new SetMultiMap<>();
    @GuardedBy("lock")
    private final EnumSet<Category> currentlyTrackedCategories = EnumSet.allOf(Category.class);
    @GuardedBy("lock")
    private final List<NoLongerBusyCallback> noLongerBusyCallbacks = new ArrayList<>(1); // Espresso use case will only have 1 callback

    private final Lock lock = new ReentrantLock();
    private final Category defaultCategory = defaultCategory();
    private final Executor completedOnThread;

    public RealBusyBee(Executor completedOnThread) {
        this.completedOnThread = completedOnThread;
    }


    @NonNull
    @Override
    public String getName() {
        lock.lock();
        try {
            return format(US, this.getClass().getSimpleName() + "@%d with operations: %s",
                    identityHashCode(this),
                    operationsInProgress);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void busyWith(@NonNull Object operation) {
        busyWith(operation, defaultCategory);
    }

    @Override
    public void busyWith(@NonNull Object operation, @NonNull final Category category) {
        //noinspection ConstantConditions
        if (operation == null) {
            throw new NullPointerException("Can not be `busyWith` null, operation must be non-null");
        }
        lock.lock();
        boolean wasAdded;
        try {
            wasAdded = operationsInProgress.add(category, operation);
            if (wasAdded) {
                log.info("busyWith -> [" + operation + "] was added to active operations in category " + category);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void registerNoLongerBusyCallback(@NonNull final NoLongerBusyCallback noLongerBusyCallback) {
        lock.lock();
        try {
            noLongerBusyCallbacks.add(noLongerBusyCallback);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void payAttentionToCategory(@NonNull final Category category) {
        lock.lock();
        try {
            log.info("Paying attention to category: " + category);
            currentlyTrackedCategories.add(category);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void ignoreCategory(@NonNull final Category category) {
        lock.lock();
        try {
            log.info("Ignoring category: " + category);
            final boolean wasBusyBefore = isBusy();
            final boolean wasRemoved = currentlyTrackedCategories.remove(category);
            final boolean notBusyNow = isNotBusy();
            if (wasRemoved && wasBusyBefore && notBusyNow) {
                notifyNoLongerBusyCallbacks();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void completedEverythingInCategory(@NonNull final Category category) {
        completedOnThread.execute(new Runnable() {
            @Override
            public void run() {
                lock.lock();
                try {
                    for (Iterator<Object> iterator = operationsInProgress.valuesIterator(category); iterator.hasNext(); ) {
                        Object next = iterator.next();
                        completeOnCurrentThread(next, iterator);
                    }
                } finally {
                    lock.unlock();
                }
            }

            @Override
            public String toString() {
                return "completedEverythingInCategory(" + category.toString() + ")";
            }
        });
    }

    @Override
    public void completedEverything() {
        completedOnThread.execute(new Runnable() {
            @Override
            public void run() {
                lock.lock();
                try {
                    for (Iterator<Object> iterator = operationsInProgress.valuesIterator(); iterator.hasNext(); ) {
                        Object next = iterator.next();
                        completeOnCurrentThread(next, iterator);
                    }
                } finally {
                    lock.unlock();
                }
            }

            @Override
            public String toString() {
                return "completedEverything()";
            }
        });
    }

    @Override
    public void completedEverythingMatching(@NonNull OperationMatcher matcher) {
        completedOnThread.execute(new Runnable() {
            @Override
            public void run() {
                lock.lock();
                try {
                    for (Iterator<Object> iterator = operationsInProgress.valuesIterator(); iterator.hasNext(); ) {
                        Object next = iterator.next();
                        if (matcher.matches(next)) {
                            completeOnCurrentThread(next, iterator);
                        }
                    }
                } finally {
                    lock.unlock();
                }
            }

            @Override
            public String toString() {
                return "completedEverythingMatching(" + matcher.toString() + ")";
            }
        });
    }

    @Override
    public void completed(@NonNull final Object operation) {
        completedOnThread.execute(new Runnable() {
            @Override
            public void run() {
                completeOnCurrentThread(operation, null);
            }

            @Override
            public String toString() {
                return "completed(" + operation.toString() + ")";
            }
        });
    }

    /**
     * Precondition: Iterator must be pointing to the operation passed in (or iterator must be null).
     * <p>
     * This method "completes" the operation on the current thread.
     *
     * @param operation the operation to be completed
     * @param iterator  must be pointing to operation
     */
    private void completeOnCurrentThread(Object operation, Iterator<Object> iterator) {
        if (operation == null) {
            throw new NullPointerException("null can not be `completed` null, operation must be non-null");
        }
        lock.lock();
        boolean wasRemoved;
        try {
            if (iterator != null) {
                // if the collection is being iterated,
                // then we HAVE to use the iterator for removal to avoid ConcurrentModificationException
                iterator.remove();
                wasRemoved = true;
            } else {
                wasRemoved = operationsInProgress.removeValue(operation);
            }
            if (wasRemoved) {
                log.info("completed -> [" + operation + "] was removed from active operations");
            }
            if (wasRemoved && isNotBusy()) {
                notifyNoLongerBusyCallbacks();
            }
        } finally {
            lock.unlock();
        }
    }

    private void notifyNoLongerBusyCallbacks() {
        for (NoLongerBusyCallback noLongerBusyCallback : noLongerBusyCallbacks) {
            log.info("All operations are now finished, we are now idle");
            noLongerBusyCallback.noLongerBusy();
        }
    }

    @Override
    public boolean isNotBusy() {
        lock.lock();
        try {
            for (Category category : currentlyTrackedCategories) {
                if (isBusyWithAnythingIn(category)) {
                    return false;
                }
            }
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isBusy() {
        return !isNotBusy();
    }

    private boolean isBusyWithAnythingIn(final Category category) {
        lock.lock();
        try {
            return !operationsInProgress.values(category).isEmpty();
        } finally {
            lock.unlock();
        }
    }

    @NonNull
    @Override
    public String toStringVerbose() {
        try {
            lock.lock();
            SetMultiMap<Category, Object> operations = operationsInProgress;
            StringBuilder sb = new StringBuilder()
                    .append("\n***********************")
                    .append("\n**BusyBee Information**")
                    .append("\n***********************");
            try {
                sb.append("\nTotal Operations:")
                        .append(operations.allValues().size())
                        .append("\nList of operations in progress:")
                        .append("\n****************************");
                for (Category category : operations.allKeys()) {
                    sb.append("\nCATEGORY: ======= ").append(category.name()).append(" =======");
                    for (Object operation : operations.values(category)) {
                        sb.append("\n").append(operation.toString());
                    }
                }
            } catch (Exception e) {
                sb.append(e.getMessage());
                sb.append("\n****!!!!FAILED TO GET LIST OF IN PROGRESS OPERATIONS!!!!****");
            }
            return sb.append("\n****************************\n").toString();
        } finally {
            lock.unlock();
        }
    }
}
