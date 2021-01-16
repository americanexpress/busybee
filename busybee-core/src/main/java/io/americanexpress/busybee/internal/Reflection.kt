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

import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException

// we are doing a number of type-unsafe things here to seamlessly detect whether we are on Android or not.
// Can't use the actual Android types because they might not exist, if we are in a pure JVM module
internal object Reflection {
    fun getValue(instance: Field?): Any {
        return try {
            instance!![null]
        } catch (e: IllegalAccessException) {
            throw RuntimeException(e)
        }
    }

    fun getField(clazz: Class<*>, fieldName: String?): Field {
        return try {
            clazz.getDeclaredField(fieldName)
        } catch (e: NoSuchFieldException) {
            throw RuntimeException(e)
        }
    }

    fun invokeStaticMethod(clazz: Class<*>, methodName: String?): Any {
        return try {
            clazz.getMethod(methodName).invoke(null)
        } catch (e: NoSuchMethodException) {
            throw RuntimeException(e)
        } catch (e: IllegalAccessException) {
            throw RuntimeException(e)
        } catch (e: InvocationTargetException) {
            throw RuntimeException(e)
        }
    }

    fun invokeMethod(instance: Any?, methodName: String?, argTypes: Array<Class<*>?>, args: Array<Any>): Any {
        return try {
            instance!!.javaClass.getMethod(methodName, *argTypes).invoke(instance, *args)
        } catch (e: NoSuchMethodException) {
            throw RuntimeException(e)
        } catch (e: IllegalAccessException) {
            throw RuntimeException(e)
        } catch (e: InvocationTargetException) {
            throw RuntimeException(e)
        }
    }

    @JvmOverloads
    fun clazz(
        className: String,
        notFoundErrorMessage: String? = "Error calling Class.forName on $className"
    ): Class<*> {
        val androidExecutorClass: Class<*>
        androidExecutorClass = try {
            Class.forName(className)
        } catch (e: ClassNotFoundException) {
            throw RuntimeException(notFoundErrorMessage, e)
        }
        return androidExecutorClass
    }

    fun classIsFound(className: String?): Boolean {
        try {
            Class.forName(className)
        } catch (e: ClassNotFoundException) {
            return false
        }
        return true
    }

    fun invokeConstructor(classToConstruct: Class<*>, constructorArgType: Class<*>?, constructorArg: Any?): Any {
        return try {
            classToConstruct.getConstructor(constructorArgType).newInstance(constructorArg)
        } catch (e: NoSuchMethodException) {
            throw RuntimeException(e)
        } catch (e: InstantiationException) {
            throw RuntimeException(e)
        } catch (e: IllegalAccessException) {
            throw RuntimeException(e)
        } catch (e: InvocationTargetException) {
            throw RuntimeException(e)
        }
    }
}