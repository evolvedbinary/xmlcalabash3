package com.xmlcalabash.config

interface CfgMapInterface<S,T>: CfgValueInterface<Map<S,T>> {
    val size: Int
    fun get(key: S): T?
    fun put(key: S, value: T)
}