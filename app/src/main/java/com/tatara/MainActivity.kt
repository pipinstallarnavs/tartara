package com.tatara

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import com.tatara.data.dashboard.DayCloseService
import com.tatara.data.dashboard.ReviewService
import com.tatara.data.db.Seeder
import com.tatara.data.db.TataraDatabase
import androidx.glance.appwidget.updateAll
import com.tatara.data.tdee.TdeeService
import com.tatara.ui.TataraApp
import com.tatara.ui.widget.TataraWidget
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Every app palette is dark, independent of the device's light/dark setting.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        val db = TataraDatabase.build(applicationContext)
        CoroutineScope(Dispatchers.IO).launch {
            Seeder.seedIfEmpty(applicationContext, db)
            // §3.5 — process every Sunday 23:00 that has passed since the last run.
            TdeeService(db).catchUp(ZonedDateTime.now())
            // §5.4/§7.2 — close days that have left the edit window: habits, XP, rollups.
            DayCloseService(db).closeOpenDays(LocalDate.now())
            // §7.5 — generate any weekly reviews that have come due.
            ReviewService(db).generateDueReviews(ZonedDateTime.now())
        }
        setContent {
            TataraApp(db)
        }
    }

    override fun onStop() {
        super.onStop()
        // §8 — the widget reflects data changed in the app.
        CoroutineScope(Dispatchers.IO).launch {
            TataraWidget().updateAll(applicationContext)
        }
    }
}
