package com.xmlcalabash.steps.internal

import com.xmlcalabash.steps.AbstractAtomicStep

class EmptyStep(): AbstractAtomicStep()  {
    override fun toString(): String = "cx:empty"
}