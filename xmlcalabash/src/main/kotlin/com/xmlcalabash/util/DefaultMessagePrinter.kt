package com.xmlcalabash.util

import com.xmlcalabash.io.MessagePrinter
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.PrintStream
import java.nio.charset.Charset

class DefaultMessagePrinter() : MessagePrinter {
    override val encoding = Charset.defaultCharset().name()

    // You would think that the very nature of a default character set was that it would be...the
    // default. But no. If you don't do this trick where you create a print stream from the
    // stderr file descriptor with the right "default" encoding, then you get...UTF-8 always, maybe?
    // Hard to tell. But this seems to work.
    private val stream = PrintStream(FileOutputStream(FileDescriptor.err), true, encoding)

    private val mustSanitize = !encoding.lowercase().startsWith("utf")

    override fun print(message: String) {
        if (mustSanitize) {
            stream.print(sanitize(message))
        } else {
            stream.print(message)
        }
    }

    override fun println(message: String) {
        if (mustSanitize) {
            stream.println(sanitize(message))
        } else {
            stream.println(message)
        }
    }

    private fun sanitize(message: String): String {
        return message.replace('“', '"')
            .replace('”', '"')
            .replace('‘', '\'')
            .replace('’', '\'')
            .replace("→", "->")
            .replace("…".toRegex(), "...")
    }
}