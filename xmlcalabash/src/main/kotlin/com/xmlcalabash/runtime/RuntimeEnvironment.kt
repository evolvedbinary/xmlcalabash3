package com.xmlcalabash.runtime

import com.xmlcalabash.api.Monitor
import com.xmlcalabash.config.XProcEnvironment
import com.xmlcalabash.datamodel.CompileEnvironment
import java.util.*

class RuntimeEnvironment(override val episode: String, private val environment: CompileEnvironment): XProcEnvironment by environment {
    companion object {
        fun newInstance(environment: CompileEnvironment): RuntimeEnvironment {
            val episode = "E-${UUID.randomUUID()}"
            return RuntimeEnvironment(episode, environment)
        }
    }

    // IntelliJ says monitors is unused, but it's wrong...somehow
    override val monitors = mutableListOf<Monitor>()
    internal var threadsAvailable = environment.xmlCalabashConfig.maxThreadCount
}