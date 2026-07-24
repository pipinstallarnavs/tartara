package com.tatara

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tatara.data.db.Seeder
import com.tatara.data.db.TataraDatabase
import com.tatara.data.tdee.TdeeService
import com.tatara.ui.TataraApp
import java.time.ZonedDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val db = TataraDatabase.build(applicationContext)
        CoroutineScope(Dispatchers.IO).launch {
            Seeder.seedIfEmpty(applicationContext, db)
            // §3.5 — process every Sunday 23:00 that has passed since the last run.
            TdeeService(db).catchUp(ZonedDateTime.now())
        }
        setContent {
            TataraApp(db)
        }
    }
}
