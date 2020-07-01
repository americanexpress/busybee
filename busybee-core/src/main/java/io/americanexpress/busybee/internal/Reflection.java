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

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

// we are doing a number of type-unsafe things here to seamlessly detect whether we are on Android or not.
// Can't use the actual Android types because they might not exist, if we are in a pure JVM module
@SuppressWarnings({"unchecked", "rawtypes"})
class Reflection {
    static Object getValue(Field instance) {
        try {
            return instance.get(null);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    static Field getField(Class<?> clazz, String fieldName) {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    static Object invokeStaticMethod(Class<?> clazz, String methodName) throws InvocationTargetException {
        try {
            return clazz.getMethod(methodName).invoke(null);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    static Object invokeMethod(Object instance, String methodName, Class[] argTypes, Object[] args) {
        try {
            return instance.getClass().getMethod(methodName, argTypes).invoke(instance, args);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    static Class<?> clazz(String className) {
        return clazz(className, "Error calling Class.forName on " + className);
    }

    @NotNull
    static Class<?> clazz(String className, String notFoundErrorMessage) {
        Class<?> androidExecutorClass;
        try {
            androidExecutorClass = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(notFoundErrorMessage, e);
        }
        return androidExecutorClass;
    }

    static boolean classIsFound(String className) {
        try {
            Class.forName(className);
        } catch (ClassNotFoundException e) {
            return false;
        }
        return true;
    }

    static Object invokeConstructor(Class classToConstruct, Class constructorArgType, Object constructorArg) {
        try {
            return classToConstruct.getConstructor(constructorArgType).newInstance(constructorArg);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
