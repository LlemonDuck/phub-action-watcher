@file:UseSerializers(InstantSerializer::class)

package com.duckblade

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.awt.Color
import java.awt.Component
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.WindowConstants
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

object InstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("java.time.Instant", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): Instant = Instant.parse(decoder.decodeString())
}

@Serializable
data class WorkflowRunsQuery(val workflow_runs: List<WorkflowRun>)

@Serializable
data class WorkflowRun(val status: String, val updated_at: Instant)

fun main() {
    val json = Json {
        ignoreUnknownKeys = true
    }
    val client = HttpClient(OkHttp) {
        engine {
            config {
                followRedirects(true)
                addInterceptor { chain ->
                    chain.proceed(
                        chain.request()
                            .newBuilder()
                            .addHeader("Accept", "application/vnd.github+json")
                            .addHeader("Authorization", "Bearer ${System.getenv("GITHUB_TOKEN")}")
                            .addHeader("X-GitHub-Api-Version", "2022-11-28")
                            .addHeader("User-Agent", "runelite/plugin-hub actions watcher by gh/LlemonDuck")
                            .build()
                    )
                }
            }
        }

        install(ContentNegotiation) {
            json(json)
        }
    }

    val label = JLabel("no data")
    label.foreground = Color.white
    label.alignmentX = Component.CENTER_ALIGNMENT
    label.alignmentY = Component.CENTER_ALIGNMENT
    label.verticalAlignment = SwingConstants.CENTER
    label.horizontalAlignment = SwingConstants.CENTER

    val jf = JFrame()
    jf.background = Color.green
    jf.setSize(250, 150)
    jf.add(label)
    jf.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    jf.isAlwaysOnTop = true
    jf.isVisible = true

    val lastRunAtomic = AtomicReference<WorkflowRun>(null)

    runBlocking {
        coroutineScope {
            launch {
                while (true) {
                    delay(5.seconds)

                    val workflows = client.get("https://api.github.com/repos/runelite/plugin-hub/actions/runs?branch=master&per_page=5")
                        .body<WorkflowRunsQuery>()

                    val mostRecent = workflows.workflow_runs.maxByOrNull { it.updated_at }
                    lastRunAtomic.set(mostRecent)
                }
            }

            launch {
                while (true) {
                    delay(20.milliseconds)

                    val lastRun = lastRunAtomic.get()
                    SwingUtilities.invokeLater {
                        label.text = lastRun?.updated_at?.let { Duration.between(it, Instant.now()).humanFormat() } ?: "no data"
                        jf.contentPane.background = when (lastRun?.status) {
                            "completed" -> Color(0x39753B)
                            "failure" -> Color(0x9F2929)
                            null -> Color(0x565656)
                            else -> Color(0xA97B0B)
                        }
                    }
                }
            }
        }
    }
}

private fun Duration.humanFormat(): String {
    return if (this > 1.days) {
        "${this.toDays()}d ${this.toHoursPart()}h"
    } else if (this > 1.hours) {
        "${this.toHours()}h ${this.toMinutesPart()}m"
    } else if (this > 1.minutes) {
        "${this.toMinutes()}m ${this.toSecondsPart()}s"
    } else {
        "${this.toSeconds()}s"
    }
}

private operator fun Duration.compareTo(kDuration: kotlin.time.Duration): Int {
    return compareTo(kDuration.toJavaDuration())
}
