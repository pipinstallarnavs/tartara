package com.tatara

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tatara.data.db.Seeder
import com.tatara.data.db.TataraDatabase
import com.tatara.ui.TataraApp
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
        }
        setContent {
            TataraApp(db)
        }
    }
}
