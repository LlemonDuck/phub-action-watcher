package com.duckblade.phubreview

import kotlinx.io.IOException
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

object InstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlin.time.Instant", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): Instant = Instant.parse(decoder.decodeString())
}

fun runCommand(vararg cmd: String, env: Map<String, String> = emptyMap()): String? {
    try {
        val proc = ProcessBuilder(*cmd).apply {
            environment().putAll(env)
            redirectOutput(ProcessBuilder.Redirect.PIPE)
            redirectError(ProcessBuilder.Redirect.PIPE)
        }.start()

        proc.waitFor(5, TimeUnit.SECONDS)
        return proc.inputStream.bufferedReader().readText().trim()
    } catch(e: IOException) {
        e.printStackTrace()
        return null
    }
}

fun Duration.humanFormat(): String {
    return if (this >= 1.days) {
        "${this.inWholeDays}d ${this.inWholeHours % 1.days.inWholeHours}h"
    } else if (this >= 1.hours) {
        "${this.inWholeHours}h ${this.inWholeMinutes % 1.hours.inWholeMinutes}m"
    } else if (this >= 1.minutes) {
        "${this.inWholeMinutes}m ${this.inWholeSeconds % 1.minutes.inWholeSeconds}s"
    } else {
        "${this.inWholeSeconds}s"
    }
}
