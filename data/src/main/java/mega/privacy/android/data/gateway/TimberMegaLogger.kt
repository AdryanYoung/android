package mega.privacy.android.data.gateway

import android.util.Log
import androidx.annotation.Keep
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaLoggerInterface
import timber.log.Timber
import javax.inject.Inject

/**
 * Timber mega logger
 *
 * Class responsible for writing SDK log output to the Timber log, adding custom tags.
 *
 * See logback.xml for configuration.
 */
@Keep
internal class TimberMegaLogger @Inject constructor() : MegaLoggerInterface {
    @Synchronized
    override fun log(time: String, logLevel: Int, source: String, message: String) {
        Timber.tag(TAG)
        Timber.log(
            priority = getPriority(logLevel),
            message = "$message ${getSource(source)}",
        )
    }

    private fun getPriority(logLevel: Int): Int {
        return when (logLevel) {
            MegaApiAndroid.LOG_LEVEL_DEBUG -> Log.DEBUG
            MegaApiAndroid.LOG_LEVEL_ERROR -> Log.ERROR
            MegaApiAndroid.LOG_LEVEL_FATAL -> Log.ERROR
            MegaApiAndroid.LOG_LEVEL_INFO -> Log.INFO
            MegaApiAndroid.LOG_LEVEL_MAX -> Log.VERBOSE
            MegaApiAndroid.LOG_LEVEL_WARNING -> Log.WARN
            else -> Log.INFO
        }
    }

    private fun getSource(source: String?) =
        source?.split("jni/mega")?.getOrNull(1) ?: source

    companion object {
        const val TAG = "[sdk]"
    }
}