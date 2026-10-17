package io.github.nexalloy

import com.google.gson.GsonBuilder
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.name

object FingerprintTestReport {
    data class Result(
        val fingerprint: String,
        val status: String,
        val matches: List<String> = emptyList(),
        val duration_ms: Long? = null,
        val reason: String? = null,
        val error: String? = null,
    )

    data class Group(
        val package_name: String,
        val results: MutableList<Result> = mutableListOf(),
        var completed: Boolean = false,
        var error: String? = null,
    )

    data class Apk(
        val apk: String,
        val app: String,
        var version: String? = null,
        val groups: MutableList<Group> = mutableListOf(),
    )

    private val apks = linkedMapOf<String, Apk>()
    private var startedAt = Instant.now().toString()

    fun appFor(path: Path): String? = when {
        path.name.startsWith("com.google.android.youtube") -> "youtube"
        path.name.startsWith("com.google.android.apps.youtube.music") -> "music"
        path.name.startsWith("com.reddit.frontpage") -> "reddit"
        else -> null
    }

    fun start(paths: List<Path>) {
        startedAt = Instant.now().toString()
        apks.clear()
        paths.forEach { path ->
            appFor(path)?.let { app ->
                val key = path.toAbsolutePath().normalize().toString()
                apks[key] = Apk(key, app)
            }
        }
        write()
    }

    fun forApk(path: Path, version: AppVersion): Apk =
        apks.getValue(path.toAbsolutePath().normalize().toString()).apply {
            this.version = version.versionString
        }

    fun write() {
        val complete = apks.isNotEmpty() && apks.values.all { apk ->
            apk.version != null && apk.groups.isNotEmpty() && apk.groups.all { it.completed }
        }
        val report = linkedMapOf(
            "schema_version" to 1,
            "started_at" to startedAt,
            "written_at" to Instant.now().toString(),
            "complete" to complete,
            "apks" to apks.values.toList(),
        )
        val file = Path.of(System.getProperty(
            "fingerprints.report", "build/reports/fingerprints/results.json"
        )).toFile()
        file.parentFile.mkdirs()
        file.writer().use {
            GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(report, it)
        }
    }
}
