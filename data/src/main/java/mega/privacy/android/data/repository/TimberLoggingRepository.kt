package mega.privacy.android.data.repository

import android.content.Context
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.FileCompressionGateway
import mega.privacy.android.data.gateway.LogWriterGateway
import mega.privacy.android.data.gateway.LogbackLogConfigurationGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.logging.LineNumberDebugTree
import mega.privacy.android.data.logging.LogFlowTree
import mega.privacy.android.domain.entity.logging.LogEntry
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.ChatLogger
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.qualifier.LogFileDirectory
import mega.privacy.android.domain.qualifier.LogZipFileDirectory
import mega.privacy.android.domain.qualifier.SdkLogger
import mega.privacy.android.domain.repository.LoggingRepository
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatLoggerInterface
import nz.mega.sdk.MegaLoggerInterface
import timber.log.Timber
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Timber logging repository
 *
 * @property megaSdkLogger
 * @property megaChatLogger
 * @property sdkLogFlowTree
 * @property chatLogFlowTree
 * @property loggingConfig
 * @property sdkLogger
 * @property chatLogger
 * @property context
 * @property fileCompressionGateway
 * @property megaApiGateway
 * @property ioDispatcher
 * @property appScope
 */
internal class TimberLoggingRepository @Inject constructor(
    private val megaSdkLogger: MegaLoggerInterface,
    private val megaChatLogger: MegaChatLoggerInterface,
    @SdkLogger private val sdkLogFlowTree: LogFlowTree,
    @ChatLogger private val chatLogFlowTree: LogFlowTree,
    private val loggingConfig: LogbackLogConfigurationGateway,
    @SdkLogger private val sdkLogger: LogWriterGateway,
    @ChatLogger private val chatLogger: LogWriterGateway,
    @ApplicationContext private val context: Context,
    private val fileCompressionGateway: FileCompressionGateway,
    private val megaApiGateway: MegaApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val appScope: CoroutineScope,
    @LogFileDirectory private val logFileDirectory: Lazy<File>,
    @LogZipFileDirectory private val logZipFileDirectory: Lazy<File>,
) : LoggingRepository {

    init {
        if (!Timber.forest().contains(sdkLogFlowTree)) {
            Timber.plant(sdkLogFlowTree)
        }
        if (!Timber.forest().contains(chatLogFlowTree)) {
            Timber.plant(chatLogFlowTree)
        }
    }

    override fun enableLogAllToConsole(isDebugBuild: Boolean) {
        MegaApiAndroid.addLoggerObject(megaSdkLogger)
        MegaApiAndroid.setLogLevel(MegaApiAndroid.LOG_LEVEL_MAX)
        MegaChatApiAndroid.setLoggerObject(megaChatLogger)
        MegaChatApiAndroid.setLogLevel(MegaChatApiAndroid.LOG_LEVEL_MAX)
        MegaChatApiAndroid.setInternalMaxLogLevel(MegaChatApiAndroid.LOG_LEVEL_DEBUG)
        if (isDebugBuild) {
            Timber.plant(LineNumberDebugTree())
        }
    }

    override fun resetSdkLogging() {
        MegaApiAndroid.removeLoggerObject(megaSdkLogger)
        MegaApiAndroid.addLoggerObject(megaSdkLogger)
    }

    override fun getSdkLoggingFlow(): Flow<LogEntry> = sdkLogFlowTree
        .logFlow
        .onSubscription {
            withContext(ioDispatcher) {
                MegaApiAndroid.setLogLevel(MegaApiAndroid.LOG_LEVEL_MAX)
                loggingConfig.resetLoggingConfiguration()
            }
        }.onCompletion {
            MegaApiAndroid.setLogLevel(MegaApiAndroid.LOG_LEVEL_FATAL)
        }

    override fun getChatLoggingFlow(): Flow<LogEntry> =
        chatLogFlowTree
            .logFlow
            .onSubscription {
                withContext(ioDispatcher) {
                    loggingConfig.resetLoggingConfiguration()
                }
            }

    override suspend fun logToSdkFile(logMessage: LogEntry) =
        withContext(ioDispatcher) { sdkLogger.writeLogEntry(logMessage) }

    override suspend fun logToChatFile(logMessage: LogEntry) =
        withContext(ioDispatcher) { chatLogger.writeLogEntry(logMessage) }

    override suspend fun compressLogs(): File = withContext(ioDispatcher) {
        Timber.d("LoggingRepository: compressLogs called")
        val loggingDirectoryPath = logFileDirectory.get().absolutePath
        val sourceFolder = File(loggingDirectoryPath).takeIf { it.exists() }
            ?: throw IllegalStateException("Logging directory not found")
        createEmptyFile().apply {
            fileCompressionGateway.zipFolder(
                sourceFolder,
                this
            )
        }
    }

    private fun createEmptyFile() =
        File("${logZipFileDirectory.get().path}/${getLogFileName()}").apply {
            if (exists()) delete()
            createNewFile()
        }

    private fun getLogFileName() =
        "${getFormattedDate()}_Android_${megaApiGateway.accountEmail}.zip"

    private fun getFormattedDate() =
        DateTimeFormatter.ofPattern("dd_MM_yyyy__HH_mm_ss")
            .withZone(ZoneId.from(ZoneOffset.UTC))
            .format(Instant.now())
}