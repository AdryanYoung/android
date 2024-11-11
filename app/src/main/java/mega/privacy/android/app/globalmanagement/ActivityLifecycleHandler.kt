package mega.privacy.android.app.globalmanagement

import android.app.Activity
import android.app.Application
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.worker.StartSyncWorkerUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.worker.StopSyncWorkerUseCase
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Activity lifecycle handler
 */
@Singleton
class ActivityLifecycleHandler @Inject constructor(
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    private val startSyncWorkerUseCase: StartSyncWorkerUseCase,
    private val stopSyncWorkerUseCase: StopSyncWorkerUseCase,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : Application.ActivityLifecycleCallbacks {
    // The current App Activity
    private var currentActivity: Activity? = null

    // Attributes to detect if app changes between background and foreground
    // Keep the count of number of Activities in the started state
    private var activityReferences = 0

    // Flag to indicate if the current Activity is going through configuration change like orientation switch
    private var isActivityChangingConfigurations = false

    /**
     * On activity created
     *
     * @param activity
     * @param savedInstanceState
     */
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    /**
     * On activity started
     *
     * @param activity
     */
    override fun onActivityStarted(activity: Activity) {
        Timber.d("onActivityStarted: %s", activity.javaClass.simpleName)
        if (activityReferences == 0) {
            Timber.i("Stopping SyncWorker")
            applicationScope.launch {
                stopSyncWorkerUseCase()
            }
        }

        if (++activityReferences == 1 && !isActivityChangingConfigurations) {
            Timber.i("App enters foreground")
            if (monitorStorageStateEventUseCase.getState() == StorageState.PayWall) {
                showOverDiskQuotaPaywallWarning(activity, false)
            }
        }
    }

    /**
     * On activity resumed
     *
     * @param activity
     */
    override fun onActivityResumed(activity: Activity) {
        Timber.d("onActivityResumed: %s", activity.javaClass.simpleName)
        currentActivity = activity
    }

    /**
     * On activity paused
     *
     * @param activity
     */
    override fun onActivityPaused(activity: Activity) {
        Timber.d("onActivityPaused: %s", activity.javaClass.simpleName)
        currentActivity = null
    }

    /**
     * On activity stopped
     *
     * @param activity
     */
    override fun onActivityStopped(activity: Activity) {
        Timber.d("onActivityStopped: %s", activity.javaClass.simpleName)
        isActivityChangingConfigurations = activity.isChangingConfigurations
        if (--activityReferences == 0 && !isActivityChangingConfigurations) {
            Timber.i("App enters background")
        }

        if (activityReferences == 0) {
            Timber.i("Starting SyncWorker")
            applicationScope.launch {
                startSyncWorkerUseCase()
            }
        }
    }

    /**
     * On activity save instance state
     *
     * @param activity
     * @param outState
     */
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    /**
     * On activity destroyed
     *
     * @param activity
     */
    override fun onActivityDestroyed(activity: Activity) {}

    /**
     * Get current activity
     *
     * @return current visible activity
     */
    fun getCurrentActivity(): Activity? = currentActivity

    /**
     * Is activity visible
     */
    @Deprecated(
        message = "This method is deprecated because it seems to don't work properly and there's a better way to check if the app is active or not",
        replaceWith = ReplaceWith("ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)")
    )
    val isActivityVisible: Boolean
        get() = currentActivity != null
}