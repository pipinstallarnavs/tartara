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
    const val CURRENT = 5

    private val steps: Map<Int, (JsonObject) -> JsonObject> = mapOf(
        1 to ::v1_to_v2,
        2 to ::v2_to_v3,
        3 to ::v3_to_v4,
        4 to ::v4_to_v5,
    )

    /**
     * v2 added nullable-with-default fields to settings (heightCm, birthYear, sex,
     * lastProcessedWeekEnd); v3 added lastHabitDayClosed; v4 added restSeconds and
     * targetRpe to routineItem; v5 added activityLevel to settings. Absent keys
     * decode to their defaults, so the only work is the version stamp.
     */
    private fun v1_to_v2(root: JsonObject): JsonObject = stamped(root, 2)

    private fun v2_to_v3(root: JsonObject): JsonObject = stamped(root, 3)

    private fun v3_to_v4(root: JsonObject): JsonObject = stamped(root, 4)

    private fun v4_to_v5(root: JsonObject): JsonObject = stamped(root, 5)

    private fun stamped(root: JsonObject, version: Int): JsonObject =
        JsonObject(root.toMutableMap().apply {
            put("schemaVersion", kotlinx.serialization.json.JsonPrimitive(version))
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
