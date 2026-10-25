package com.example.bingelist.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.bingelist.data.database.BingeListDatabaseHelper
import com.example.bingelist.data.model.FeaturedMovies
import com.example.bingelist.data.repository.MovieRepository
import com.example.bingelist.ui.watchlist.WatchlistActivity
import com.google.firebase.auth.FirebaseAuth

// Weekly: what's new this week, plus a nudge if the user has movies waiting in the watchlist. */
class WeeklyDigestWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        if (!NotificationHelper.isEnabled(ctx)) return Result.success()

        val movies = MovieRepository().getMovieDetailsByIds(FeaturedMovies.NEW_THIS_WEEK_IDS)
        if (movies.isNotEmpty()) {
            NotificationHelper.show(
                ctx,
                "${movies.size} new this week",
                movies.joinToString(" · ") { it.title },
                imdbId = null,
                notificationId = NotificationHelper.ID_DIGEST
            )
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val waiting = BingeListDatabaseHelper(ctx).getWatchlistIds(uid).size
            if (waiting > 0) {
                NotificationHelper.show(
                    ctx,
                    "Your watchlist is waiting",
                    if (waiting == 1) "You have 1 movie saved to watch."
                    else "You have $waiting movies saved to watch.",
                    imdbId = null,
                    target = WatchlistActivity::class.java,
                    notificationId = NotificationHelper.ID_REMINDER
                )
            }
        }
        return Result.success()
    }
}