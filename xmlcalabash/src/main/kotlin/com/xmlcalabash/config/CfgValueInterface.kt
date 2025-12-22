package com.xmlcalabash.config

interface CfgValueInterface<T> {
    val isSet: Boolean
    val options: MutableMap<String,String>?
    fun set(newValue: T?)
    fun get(): T?
    fun getOrDefault(): T?
    fun update(newValue: CfgValueInterface<T>)
}