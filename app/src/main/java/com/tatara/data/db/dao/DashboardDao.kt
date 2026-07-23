package com.tatara.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tatara.data.db.entity.DailyRollup
import com.tatara.data.db.entity.TierCrossing
import com.tatara.data.db.entity.WeeklyReview
import com.tatara.data.db.entity.XpEvent

@Dao
interface DashboardDao {
    @Insert suspend fun insertXpEvent(event: XpEvent): Long
    @Insert suspend fun insertXpEvents(events: List<XpEvent>)
    @Query("SELECT COALESCE(SUM(amount), 0) FROM xp_event") suspend fun totalXp(): Long
    @Query("SELECT * FROM xp_event ORDER BY id") suspend fun getAllXpEvents(): List<XpEvent>
    @Query("DELETE FROM xp_event") suspend fun deleteAllXpEvents()

    @Insert suspend fun insertTierCrossing(crossing: TierCrossing): Long
    @Insert suspend fun insertTierCrossings(crossings: List<TierCrossing>)
    @Query("SELECT * FROM tier_crossing ORDER BY id") suspend fun getAllTierCrossings(): List<TierCrossing>
    @Query("DELETE FROM tier_crossing") suspend fun deleteAllTierCrossings()

    @Insert suspend fun insertReview(review: WeeklyReview): Long
    @Insert suspend fun insertReviews(reviews: List<WeeklyReview>)
    @Query("SELECT * FROM weekly_review ORDER BY id") suspend fun getAllReviews(): List<WeeklyReview>
    @Query("DELETE FROM weekly_review") suspend fun deleteAllReviews()

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertRollup(rollup: DailyRollup)
    @Query("SELECT * FROM daily_rollup ORDER BY date") suspend fun getAllRollups(): List<DailyRollup>
    @Query("DELETE FROM daily_rollup") suspend fun deleteAllRollups()
}
