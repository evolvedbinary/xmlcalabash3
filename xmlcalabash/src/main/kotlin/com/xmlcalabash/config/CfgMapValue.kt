package com.xmlcalabash.config

class CfgMapValue<S,T>(private val default: Map<S,T>? = null): CfgMapInterface<S,T> {
    private var map: MutableMap<S,T>? = null

    override val isSet: Boolean
        get() = map != null

    override var options: MutableMap<String, String>? = null

    override val size: Int
        get() = map?.size ?: 0

    override fun get(key: S): T? {
        return map?.get(key)
    }

    override fun put(key: S, value: T) {
        if (map == null) {
            map = mutableMapOf()
        }
        map!![key] = value
    }

    override fun set(newValue: Map<S, T>?) {
        if (newValue == null) {
            map = null
        } else {
            map = mutableMapOf()
            map!!.putAll(newValue)
        }
        options = null
    }

    override fun get(): Map<S, T>? {
        return map
    }

    override fun getOrDefault(): Map<S, T>? {
        return map ?: default
    }

    override fun update(newValue: CfgValueInterface<Map<S, T>>) {
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