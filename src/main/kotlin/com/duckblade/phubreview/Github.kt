@file:UseSerializers(InstantSerializer::class)

package com.duckblade.phubreview

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.Json
import kotlin.time.Instant

@Serializable
data class PullRequestHead(
    val sha: String,
)

@Serializable
data class PullRequest(
    val number: Int,
    @SerialName("updated_at")
    val updatedAt: Instant,
    val head: PullRequestHead,
)

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

@Serializable
data class PullRequestFile(
    val filename: String,
    val status: String,
)

class Github(
    private val repo: String,
) {

    private val token = System.getenv("GITHUB_TOKEN")
        ?: runCommand("gh", "auth", "token")
        ?: throw RuntimeException("No github token provided, use GITHUB_TOKEN env or gh auth login")

    private val json = Json {
        ignoreUnknownKeys = true
    }
    private val client = HttpClient(OkHttp) {
        defaultRequest {
            url {
                protocol = URLProtocol.HTTPS
                host = "api.github.com"
            }
        }
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

    suspend fun getWorkflowRuns(): WorkflowRunsQuery {
        return client.get("/repos/$repo/actions/runs?branch=master&per_page=100")
            .body<WorkflowRunsQuery>()
    }

    suspend fun getPullRequest(number: Int): PullRequest {
        return client.get("/repos/$repo/pulls/$number")
            .body<PullRequest>()
    }

    suspend fun getMergeSubject(pr: Int): String? {
        val pluginFile = client.get("/repos/runelite/plugin-hub/pulls/$pr/files")
            .body<Array<PullRequestFile>>()
            .singleOrNull() ?: return null

        if (!pluginFile.filename.startsWith("plugins/")) {
            return null
        }

        val verb = when (pluginFile.status) {
            "added" -> "add"
            "modified" -> "update"
            else -> return null
        }

        val glob = pluginFile.filename.substringAfter("plugins/")

        return "$verb $glob"
    }

    suspend fun merge(pr: QueuedItem) {
        runCommand(
            "gh",
            "pr",
            "merge",
            "--admin",
            "--squash",
            "--repo", repo,
            "${pr.number}",
            "-t", pr.subject!!,
            "--match-head-commit", pr.sha!!,
            env = mapOf("GH_TOKEN" to token)
        )
    }

}