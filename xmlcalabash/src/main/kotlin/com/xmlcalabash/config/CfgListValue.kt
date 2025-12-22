package com.xmlcalabash.config

class CfgListValue<T>(private var default: List<T>? = null): CfgListInterface<T> {
    private var value: MutableList<T>? = null

    override val isSet: Boolean
        get() = value != null

    override var options: MutableMap<String, String>? = null

    override val size: Int
        get() = value?.size ?: 0

    override fun get(index: Int): T {
        return value!![index]
    }

    override fun set(newValue: List<T>?) {
        if (newValue == null) {
            value = null
        } else {
            value = mutableListOf()
            value!!.addAll(newValue)
        }
        options = null
    }

    override fun add(item: T) {
        if (value == null) {
            value = mutableListOf()
        }
        value!!.add(item)
    }

    override fun get(): List<T>? {
        return value
    }

    override fun getOrDefault(): List<T>? {
        return value ?: default
    }

    override fun update(newValue: CfgValueInterface<List<T>>) {
        if (!isSet) {
            set(newValue.get())
            if (newValue.options != null) {
                options = mutableMapOf()
                options!!.putAll(newValue.options!!)
            } else {
                options = null
            }
        }
    }
}