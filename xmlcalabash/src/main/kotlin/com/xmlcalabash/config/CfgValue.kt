package com.xmlcalabash.config

class CfgValue<T>(private val default: T? = null): CfgValueInterface<T> {
    private var value: T? = null

    override val isSet: Boolean
        get() = value != null

    override var options: MutableMap<String, String>? = null

    override fun set(newValue: T?) {
        value = newValue
        options = null
    }

    override fun get(): T? {
        return value
    }

    override fun getOrDefault(): T? {
        return value ?: default
    }

    override fun update(newValue: CfgValueInterface<T>) {
        if (!isSet) {
            value = newValue.get()
            if (newValue.options != null) {
                options = mutableMapOf()
                options!!.putAll(newValue.options!!)
            } else {
                options = null
            }
        }
    }
}