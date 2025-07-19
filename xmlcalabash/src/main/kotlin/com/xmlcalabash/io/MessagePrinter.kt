package com.xmlcalabash.io

interface MessagePrinter {
    val encoding: String
    fun print(message: String)
    fun println(message: String)
}