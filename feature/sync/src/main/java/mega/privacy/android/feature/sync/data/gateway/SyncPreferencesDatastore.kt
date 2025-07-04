package mega.privacy.android.feature.sync.data.gateway

import kotlinx.coroutines.flow.Flow

internal interface SyncPreferencesDatastore {

    suspend fun setOnboardingShown(shown: Boolean)

    suspend fun getOnboardingShown(): Boolean?

    suspend fun setSyncOnlyByWiFi(checked: Boolean)

    fun monitorSyncOnlyByWiFi(): Flow<Boolean?>

    suspend fun setSyncOnlyByCharging(checked: Boolean)

    fun monitorSyncOnlyByCharging(): Flow<Boolean?>

    suspend fun setSyncFrequencyInMinutes(frequencyInMinutes: Int)

    suspend fun getSyncFrequencyMinutes(): Int?
}
