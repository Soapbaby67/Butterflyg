package com.example.core

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import java.util.concurrent.ConcurrentHashMap

object FirestoreClientProvider {
    private const val TAG = "FirestoreClientProvider"
    private const val CACHE_SIZE_BYTES = 100L * 1024L * 1024L // 100 MB offline cache
    private val configuredDatabases = ConcurrentHashMap.newKeySet<String>()

    fun getDatabase(context: Context): FirebaseFirestore {
        val databaseId = context.applicationContext.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(databaseId)
        if (configuredDatabases.add(databaseId)) {
            runCatching {
                val settings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(
                        PersistentCacheSettings.newBuilder()
                            .setSizeBytes(CACHE_SIZE_BYTES)
                            .build()
                    )
                    .build()
                db.firestoreSettings = settings
            }.onFailure { err ->
                Log.w(TAG, "Firestore cache settings already initialized: ${err.message}")
            }
        }
        return db
    }
}
