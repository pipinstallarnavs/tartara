package com.tatara.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.tatara.data.db.entity.Exercise
import com.tatara.data.db.entity.Routine
import com.tatara.data.db.entity.RoutineItem
import com.tatara.data.db.entity.Session
import com.tatara.data.db.entity.SetEntry
import java.time.LocalDate

@Dao
interface TrainDao {
    @Insert suspend fun insertExercise(exercise: Exercise): Long
    @Insert suspend fun insertExercises(exercises: List<Exercise>)
    @Query("SELECT * FROM exercise ORDER BY id") suspend fun getAllExercises(): List<Exercise>
    @Query("SELECT COUNT(*) FROM exercise") suspend fun countExercises(): Int
    @Query("DELETE FROM exercise") suspend fun deleteAllExercises()

    @Insert suspend fun insertRoutine(routine: Routine): Long
    @Insert suspend fun insertRoutines(routines: List<Routine>)
    @Query("SELECT * FROM routine ORDER BY sortOrder") suspend fun routinesInOrder(): List<Routine>
    @Query("SELECT * FROM routine ORDER BY id") suspend fun getAllRoutines(): List<Routine>
    @Query("DELETE FROM routine") suspend fun deleteAllRoutines()

    @Insert suspend fun insertRoutineItem(item: RoutineItem): Long
    @Insert suspend fun insertRoutineItems(items: List<RoutineItem>)
    @Update suspend fun updateRoutineItem(item: RoutineItem)
    @Query("SELECT * FROM routine_item WHERE routineId = :routineId ORDER BY sortOrder")
    suspend fun itemsForRoutine(routineId: Long): List<RoutineItem>
    @Query("SELECT * FROM routine_item WHERE id = :id") suspend fun routineItemById(id: Long): RoutineItem?
    @Query("SELECT * FROM routine_item ORDER BY id") suspend fun getAllRoutineItems(): List<RoutineItem>
    @Query("DELETE FROM routine_item") suspend fun deleteAllRoutineItems()

    @Insert suspend fun insertSession(session: Session): Long
    @Insert suspend fun insertSessions(sessions: List<Session>)
    @Query("SELECT * FROM session ORDER BY date DESC, id DESC") suspend fun sessionsNewestFirst(): List<Session>
    @Query("SELECT * FROM session WHERE id = :id") suspend fun sessionById(id: Long): Session?
    @Update suspend fun updateSession(session: Session)
    @Query("DELETE FROM session WHERE id = :id") suspend fun deleteSessionById(id: Long)
    @Query("SELECT * FROM session WHERE date BETWEEN :from AND :to")
    suspend fun sessionsBetween(from: LocalDate, to: LocalDate): List<Session>
    @Query("SELECT * FROM session WHERE routineId IS NOT NULL ORDER BY date DESC, id DESC LIMIT 1")
    suspend fun lastRoutineSession(): Session?
    @Query("SELECT * FROM session ORDER BY id") suspend fun getAllSessions(): List<Session>
    @Query("DELETE FROM session") suspend fun deleteAllSessions()

    @Insert suspend fun insertSet(entry: SetEntry): Long
    @Insert suspend fun insertSets(entries: List<SetEntry>)
    @Query("SELECT * FROM set_entry WHERE sessionId = :sessionId ORDER BY setIndex")
    suspend fun setsForSession(sessionId: Long): List<SetEntry>
    @Query(
        "SELECT * FROM set_entry WHERE routineItemId = :itemId AND sessionId = " +
            "(SELECT sessionId FROM set_entry WHERE routineItemId = :itemId ORDER BY sessionId DESC LIMIT 1) " +
            "ORDER BY setIndex"
    )
    suspend fun lastSetsForItem(itemId: Long): List<SetEntry>
    @Query("SELECT * FROM set_entry WHERE exerciseId = :exerciseId AND isWarmup = 0")
    suspend fun workingSetsForExercise(exerciseId: Long): List<SetEntry>
    @Query("SELECT * FROM set_entry ORDER BY id") suspend fun getAllSets(): List<SetEntry>
    @Query("DELETE FROM set_entry") suspend fun deleteAllSets()
}
