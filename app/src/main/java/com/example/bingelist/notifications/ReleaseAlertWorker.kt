package com.example.bingelist.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.bingelist.data.database.BingeListDatabaseHelper
import com.example.bingelist.data.repository.MovieRepository
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Locale

// Daily check: tells the user when a watchlist movie was released in the last 7 days. */
class ReleaseAlertWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        if (!NotificationHelper.isEnabled(ctx)) return Result.success()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()

        val db = BingeListDatabaseHelper(ctx)
        val repository = MovieRepository()
        val prefs = ctx.getSharedPreferences("release_alerts", Context.MODE_PRIVATE)
        val notified = (prefs.getStringSet("notified_$uid", emptySet()) ?: emptySet()).toMutableSet()

        val format = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH) // OMDb: "15 Mar 2024"
        val now = System.currentTimeMillis()
        val week = 7L * 24 * 60 * 60 * 1000

        for (id in db.getWatchlistIds(uid)) {
            if (id in notified) continue // never alert twice for the same movie
            val movie = repository.getMovieDetails(id).getOrNull() ?: continue
            val releasedAt = runCatching { format.parse(movie.released ?: "")?.time }.getOrNull()
                ?: continue

            if (releasedAt in (now - week)..now) {
                NotificationHelper.show(
                    ctx,
                    "Now showing: ${movie.title}",
                    "${movie.title} from your watchlist was released on ${movie.released}.",
                    id
                )
                notified.add(id)
            }
        }
        prefs.edit().putStringSet("notified_$uid", notified).apply()
        return Result.success()
    }
}