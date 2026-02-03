package com.hytale2mc.ecs.data

import com.hytale2mc.ecs.system.Executable
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

internal class LocalDelegate<T>(
    private val executable: Executable<*>,
    private val key: String,
    internal var value: T
) : ReadWriteProperty<Any?, T> {

    override operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return executable.locals.getValue(key).value as T
    }

    override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }
}

class Local<T> internal constructor(
    private val executable: Executable<*>,
    private val initial: T
) {

    operator fun provideDelegate(
        thisRef: Any?,
        prop: KProperty<*>
    ): ReadWriteProperty<Any?, T> {
        val key = prop.toString()
        val cached = executable.locals[key] as? LocalDelegate<T>
        if (cached != null) return cached
        val local = LocalDelegate(executable, prop.toString(), initial)
        executable.locals[key] = local
        return local
    }

}