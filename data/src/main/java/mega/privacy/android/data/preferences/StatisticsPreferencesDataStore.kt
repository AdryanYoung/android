package mega.privacy.android.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.preferences.ChatPreferencesGateway
import mega.privacy.android.data.gateway.preferences.StatisticsPreferencesGateway
import mega.privacy.android.domain.qualifier.IoDispatcher
import java.io.IOException
import javax.inject.Inject

private const val mdClickPreferenceName = "MEDIA_DISCOVERY_CLICK"
private val Context.mediaDiscoveryStatisticsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = mdClickPreferenceName,
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
    produceMigrations = {
        listOf(
            SharedPreferencesMigration(it, mdClickPreferenceName)
        )
    }
)

/**
 * Chat preferences data store implementation of the [ChatPreferencesGateway]
 *
 * @property context
 * @property ioDispatcher
 * @constructor Create empty chat preferences data store.
 **/
internal class StatisticsPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : StatisticsPreferencesGateway {

    companion object {
        private const val CLICK_COUNT = "ClickCount"
        private const val CLICK_COUNT_FOLDER = "ClickCountFolder"
    }

    private val clickCountPreferenceKey = intPreferencesKey(CLICK_COUNT)

    private val clickCountFolderPreferenceKey = intPreferencesKey(CLICK_COUNT_FOLDER)

    override fun getClickCount(): Flow<Int> =
        context.mediaDiscoveryStatisticsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences ->
                preferences[clickCountPreferenceKey] ?: 0
            }


    override suspend fun setClickCount(count: Int) {
        withContext(ioDispatcher) {
            context.mediaDiscoveryStatisticsDataStore.edit {
                it[clickCountPreferenceKey] = count
            }
        }
    }

    override fun getClickCountFolder(mediaHandle: Long): Flow<Int> =
        context.mediaDiscoveryStatisticsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences ->
                val clickCountFolderPreferenceKey =
                    intPreferencesKey(CLICK_COUNT_FOLDER + mediaHandle)
                preferences[clickCountFolderPreferenceKey] ?: 0
            }


    override suspend fun setClickCountFolder(count: Int, mediaHandle: Long) {
        val clickCountFolderPreferenceKey = intPreferencesKey(CLICK_COUNT_FOLDER + mediaHandle)
        withContext(ioDispatcher) {
            context.mediaDiscoveryStatisticsDataStore.edit {
                it[clickCountFolderPreferenceKey] = count
            }
        }
    }
}