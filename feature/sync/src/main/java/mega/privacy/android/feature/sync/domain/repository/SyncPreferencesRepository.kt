package mega.privacy.android.feature.sync.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repository for storing user preference regarding Sync
 */
interface SyncPreferencesRepository {

    /**
     * Sets if sync should be done only when connected to WiFi
     */
    suspend fun setSyncByWiFi(checked: Boolean)

    /**
     * Gets if sync should be done only when connected to WiFi
     */
    fun monitorSyncByWiFi(): Flow<Boolean?>

    /**
     * Sets if sync should be done only when charging
     */
    suspend fun setSyncByCharging(checked: Boolean)

    /**
     * Gets if sync should be done only when charging
     */
    fun monitorSyncByCharging(): Flow<Boolean?>

    suspend fun setOnboardingShown(shown: Boolean)

    suspend fun getOnboardingShown(): Boolean?

    suspend fun setUserPausedSync(syncId: Long)

    suspend fun deleteUserPausedSync(syncId: Long)

    suspend fun isSyncPausedByTheUser(syncId: Long): Boolean

    suspend fun setSyncFrequencyInMinutes(frequencyInMinutes: Int)

    suspend fun getSyncFrequencyMinutes(): Int
}
