@file:UseSerializers(InstantSerializer::class)
@file:OptIn(ExperimentalTime::class)

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
import kotlinx.io.IOException
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
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
import java.awt.GraphicsEnvironment
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.swing.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

object InstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlin.time.Instant", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): Instant = Instant.parse(decoder.decodeString())
}

@Serializable
data class Repository(
    @SerialName("full_name") val fullName: String,
)

@Serializable
data class WorkflowRunsQuery(
    @SerialName("workflow_runs") val workflowRuns: List<WorkflowRun>,
)

@Serializable
data class WorkflowRun(
    val status: String,
    val conclusion: String?,
    @SerialName("updated_at") val updatedAt: Instant,
    @SerialName("head_repository") val headRepository: Repository,
)

fun main() {
    val token = System.getenv("GITHUB_TOKEN")
        ?: runCommand("gh", "auth", "token")
        ?: throw RuntimeException("No github token provided, use GITHUB_TOKEN env or gh auth login")

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
                            .addHeader("Authorization", "Bearer $token")
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

    val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
    val wb = ge.maximumWindowBounds
    jf.setLocation(0, wb.height - jf.height)

    val lastRunAtomic = AtomicReference<WorkflowRun>(null)

    runBlocking {
        coroutineScope {
            launch {
                while (true) {
                    delay(5.seconds)
                    try {
                        val workflows = client.get("https://api.github.com/repos/runelite/plugin-hub/actions/runs?branch=master&per_page=100")
                            .body<WorkflowRunsQuery>()

                        val mostRecent = workflows.workflowRuns
                            .filter { it.headRepository.fullName == "runelite/plugin-hub" } // filter out forks' "master" branches
                            .maxByOrNull { it.updatedAt }
                        lastRunAtomic.set(mostRecent)
                    } catch (t: Throwable) {}
                }
            }

            launch {
                while (true) {
                    delay(20.milliseconds)

                    val lastRun = lastRunAtomic.get()
                    SwingUtilities.invokeLater {
                        label.text = lastRun?.updatedAt?.let { (Clock.System.now() - it).humanFormat() } ?: "no data"
                        jf.contentPane.background = when (lastRun?.status) {
                            "completed" -> {
                                if (lastRun.conclusion == "success")
                                    Color(0x39753B)
                                else
                                    Color(0x9F2929)
                            }

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

fun runCommand(vararg cmd: String): String? {
    try {
        val proc = ProcessBuilder(*cmd)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        proc.waitFor(5, TimeUnit.SECONDS)
        return proc.inputStream.bufferedReader().readText().trim()
    } catch(e: IOException) {
        e.printStackTrace()
        return null
    }
}
