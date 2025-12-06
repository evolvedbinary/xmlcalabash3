package com.xmlcalabash.runtime.steps

class ThreadCounter(val totalThreads: Int) {
    private var counter = 0

    fun deadlocked(): Boolean {
        synchronized(this) {
            return counter == totalThreads
        }
    }

    fun blocked() {
        synchronized(this) {
            counter++
        }
    }

    fun unblocked() {
        synchronized(this) {
            counter--
        }
    }
}