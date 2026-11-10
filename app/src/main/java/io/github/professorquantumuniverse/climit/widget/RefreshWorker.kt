package io.github.professorquantumuniverse.climit.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.professorquantumuniverse.climit.data.ApiException
import io.github.professorquantumuniverse.climit.data.SessionRepository
import java.util.concurrent.TimeUnit

/** Fetches usage in the background and redraws the widgets. */
class RefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = SessionRepository(applicationContext)
        if (repository.isSignedIn()) {
            try {
                repository.refresh()
            } catch (_: ApiException) {
                // Keep showing the cached values; the next run tries again.
            }
        } else {
            repository.load()
        }
        UsageWidget().updateAll(applicationContext)
        return Result.success()
    }

    companion object {
        private const val PERIODIC = "usage-refresh"
        private const val ONCE = "usage-refresh-now"

        private val online = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        /** Refresh every 15 minutes, the shortest interval Android allows for periodic work. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<RefreshWorker>(15, TimeUnit.MINUTES)
                .setConstraints(online)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC)
        }

        /** One refresh as soon as there is a connection; repeated taps collapse into one run. */
        fun runOnce(context: Context) {
            val request = OneTimeWorkRequestBuilder<RefreshWorker>().setConstraints(online).build()
            WorkManager.getInstance(context).enqueueUniqueWork(ONCE, ExistingWorkPolicy.KEEP, request)
        }
    }
}
