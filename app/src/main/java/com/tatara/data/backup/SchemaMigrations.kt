package com.tatara.data.backup

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

class UnsupportedSchemaException(fileVersion: Int, appVersion: Int) : Exception(
    "Backup file is schema v$fileVersion; this app reads up to v$appVersion. Update the app, then import."
)

class NotABackupException : Exception("Not a Tatara backup: missing or invalid schemaVersion.")

/**
 * §2.3 — JSON schema migrations for import. One pure function per step, keyed by the
 * version it converts FROM. Each step must return a JsonObject whose schemaVersion is
 * exactly one higher.
 */
object SchemaMigrations {
    const val CURRENT = 2

    private val steps: Map<Int, (JsonObject) -> JsonObject> = mapOf(
        1 to ::v1_to_v2,
    )

    /**
     * v2 added nullable-with-default fields to settings (heightCm, birthYear, sex,
     * lastProcessedWeekEnd). Absent keys decode to their defaults, so the only work
     * is the version stamp.
     */
    private fun v1_to_v2(root: JsonObject): JsonObject =
        JsonObject(root.toMutableMap().apply {
            put("schemaVersion", kotlinx.serialization.json.JsonPrimitive(2))
        })

    fun migrate(root: JsonObject): JsonObject {
        var current = root
        var version = versionOf(current)
        if (version > CURRENT) throw UnsupportedSchemaException(version, CURRENT)
        while (version < CURRENT) {
            val step = steps[version]
                ?: throw IllegalStateException("No converter from schema v$version")
            current = step(current)
            val next = versionOf(current)
            check(next == version + 1) { "Converter from v$version produced v$next" }
            version = next
        }
        return current
    }

    private fun versionOf(root: JsonObject): Int =
        root["schemaVersion"]?.jsonPrimitive?.intOrNull ?: throw NotABackupException()
}
