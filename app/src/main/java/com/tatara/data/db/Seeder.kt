package com.tatara.data.db

import android.content.Context
import com.tatara.data.db.entity.Exercise
import com.tatara.data.db.entity.FatSource
import com.tatara.data.db.entity.Food
import com.tatara.data.db.entity.UnitType

/**
 * Seeds the food database (IFCT 2017, §3.2) and exercise library (§4.1) from bundled
 * CSV assets on first run. One-time and local — the app never touches the network.
 */
object Seeder {

    suspend fun seedIfEmpty(context: Context, db: TataraDatabase) {
        if (db.foodDao().count() == 0) {
            db.foodDao().insertAll(loadFoods(context))
        }
        if (db.trainDao().countExercises() == 0) {
            db.trainDao().insertExercises(loadExercises(context))
        }
    }

    fun loadFoods(context: Context): List<Food> =
        readCsv(context, "foods_seed.csv").map { f ->
            Food(
                name = f[0],
                unitType = UnitType.valueOf(f[1]),
                portionName = f[2].ifEmpty { null },
                kcal = f[3].toFloat(),
                protein = f[4].toFloat(),
                carbs = f[5].toFloat(),
                fat = f[6].toFloat(),
                fatSource = FatSource.valueOf(f[7]),
                isEstimated = f[8].toBoolean(),
                aliases = f[9],
            )
        }

    fun loadExercises(context: Context): List<Exercise> =
        readCsv(context, "exercises_seed.csv").map { f ->
            Exercise(name = f[0], muscleGroup = f[1], equipment = f[2])
        }

    private fun readCsv(context: Context, asset: String): List<List<String>> =
        context.assets.open(asset).bufferedReader().useLines { lines ->
            lines.drop(1).filter { it.isNotBlank() }.map { parseCsvLine(it) }.toList()
        }

    /** Quote-aware split; seed fields never contain embedded newlines. */
    private fun parseCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                inQuotes && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> { sb.append('"'); i++ }
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> { fields.add(sb.toString()); sb.clear() }
                else -> sb.append(c)
            }
            i++
        }
        fields.add(sb.toString())
        return fields
    }
}
