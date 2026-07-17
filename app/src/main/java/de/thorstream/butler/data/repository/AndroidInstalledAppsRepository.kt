package de.thorstream.butler.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import de.thorstream.butler.core.common.AppError
import de.thorstream.butler.core.common.AppResult
import de.thorstream.butler.domain.model.InstalledApp
import de.thorstream.butler.domain.repository.InstalledAppsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidInstalledAppsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : InstalledAppsRepository {
    private val packageManager: PackageManager get() = context.packageManager

    override suspend fun getLaunchableApps(): AppResult<List<InstalledApp>> = withContext(Dispatchers.IO) {
        try {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val resolved = packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
            AppResult.Success(
                resolved.asSequence()
                    .filter { it.activityInfo.packageName != context.packageName }
                    .map { info ->
                        InstalledApp(
                            label = info.loadLabel(packageManager).toString(),
                            packageName = info.activityInfo.packageName,
                        )
                    }
                    .distinctBy { it.packageName }
                    .sortedBy { it.label.lowercase() }
                    .toList(),
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: SecurityException) {
            AppResult.Failure(AppError.Unavailable("Die installierten Apps dürfen auf diesem Gerät nicht vollständig gelesen werden."))
        } catch (_: RuntimeException) {
            AppResult.Failure(AppError.Technical("Die Liste der installierten Apps konnte nicht geladen werden."))
        }
    }

    override fun canLaunch(packageName: String): Boolean = packageManager.getLaunchIntentForPackage(packageName) != null

    override fun launch(packageName: String): AppResult<Unit> {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            ?: return AppResult.Failure(AppError.Unavailable("Die App ist nicht installiert oder besitzt keinen startbaren Bildschirm."))
        return try {
            context.startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            AppResult.Success(Unit)
        } catch (_: SecurityException) {
            AppResult.Failure(AppError.Unavailable("Android hat den Start dieser App blockiert."))
        } catch (_: RuntimeException) {
            AppResult.Failure(AppError.Technical("Die App konnte nicht gestartet werden."))
        }
    }
}

