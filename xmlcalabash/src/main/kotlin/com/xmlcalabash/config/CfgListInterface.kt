package com.xmlcalabash.config

interface CfgListInterface<T>: CfgValueInterface<List<T>> {
    val size: Int
    fun get(index: Int): T
    fun add(item: T)
}