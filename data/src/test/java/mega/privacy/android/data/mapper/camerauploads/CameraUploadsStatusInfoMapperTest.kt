package mega.privacy.android.data.mapper.camerauploads

import androidx.work.Data
import androidx.work.WorkInfo
import androidx.work.workDataOf
import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.ARE_UPLOADS_PAUSED
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.CHECK_FILE_UPLOAD
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.COMPRESSION_ERROR
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.COMPRESSION_PROGRESS
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.CURRENT_FILE_INDEX
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.CURRENT_PROGRESS
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.FINISHED
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.FINISHED_REASON
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.FOLDER_TYPE
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.FOLDER_UNAVAILABLE
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.NOT_ENOUGH_STORAGE
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.NO_WIFI_CONNECTION
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.OUT_OF_SPACE
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.PROGRESS
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.STATUS_INFO
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.STORAGE_OVER_QUOTA
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.TOTAL_COUNT
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.TOTAL_TO_UPLOAD
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.TOTAL_UPLOADED
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.TOTAL_UPLOADED_BYTES
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.TOTAL_UPLOAD_BYTES
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsFinishedReason
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsStatusInfo
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CameraUploadsStatusInfoMapperTest {
    private lateinit var underTest: CameraUploadsStatusInfoMapper

    private val totalUploaded = 10
    private val totalToUpload = 100
    private val totalUploadedBytes = 1024L
    private val totalUploadBytes = 2048L
    private val progress = 0.5f
    private val areUploadsPaused = false
    private val currentFileIndex = 11
    private val totalCount = 20


    private val progressData = workDataOf(
        STATUS_INFO to PROGRESS,
        TOTAL_UPLOADED to totalUploaded,
        TOTAL_TO_UPLOAD to totalToUpload,
        TOTAL_UPLOADED_BYTES to totalUploadedBytes,
        TOTAL_UPLOAD_BYTES to totalUploadBytes,
        CURRENT_PROGRESS to progress,
        ARE_UPLOADS_PAUSED to areUploadsPaused
    )

    private val cameraUploadProgressInfo = CameraUploadsStatusInfo.UploadProgress(
        totalUploaded = totalUploaded,
        totalToUpload = totalToUpload,
        totalUploadedBytes = totalUploadedBytes,
        totalUploadBytes = totalUploadBytes,
        progress = Progress(progress),
        areUploadsPaused = areUploadsPaused
    )

    private val videoProgressData = workDataOf(
        STATUS_INFO to COMPRESSION_PROGRESS,
        CURRENT_PROGRESS to progress,
        CURRENT_FILE_INDEX to currentFileIndex,
        TOTAL_COUNT to totalCount
    )

    private val videoProgressInfo = CameraUploadsStatusInfo.VideoCompressionProgress(
        currentFileIndex = currentFileIndex,
        totalCount = totalCount,
        progress = Progress(progress),
    )

    @BeforeAll
    fun setUp() {
        underTest = CameraUploadsStatusInfoMapper()
    }

    @ParameterizedTest(name = "when invoked with {0} and returned {1}")
    @MethodSource("provideParameters")
    fun `test that mapper returns model correctly when invoke function`(
        progress: Data,
        state: WorkInfo.State,
        stopReason: Int,
        cameraUploadsStatusInfo: CameraUploadsStatusInfo?,
    ) = runTest {
        val actual = underTest(progress, state, stopReason)
        Truth.assertThat(actual).isEqualTo(cameraUploadsStatusInfo)
    }

    private fun provideParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(
            progressData,
            WorkInfo.State.RUNNING,
            WorkInfo.STOP_REASON_NOT_STOPPED,
            cameraUploadProgressInfo,
        ),
        Arguments.of(
            videoProgressData,
            WorkInfo.State.RUNNING,
            WorkInfo.STOP_REASON_NOT_STOPPED,
            videoProgressInfo
        ),
        Arguments.of(
            workDataOf(STATUS_INFO to CHECK_FILE_UPLOAD),
            WorkInfo.State.RUNNING,
            WorkInfo.STOP_REASON_NOT_STOPPED,
            CameraUploadsStatusInfo.CheckFilesForUpload
        ),
        Arguments.of(
            workDataOf(STATUS_INFO to STORAGE_OVER_QUOTA),
            WorkInfo.State.RUNNING,
            WorkInfo.STOP_REASON_NOT_STOPPED,
            CameraUploadsStatusInfo.StorageOverQuota
        ),
        Arguments.of(
            workDataOf(STATUS_INFO to OUT_OF_SPACE),
            WorkInfo.State.RUNNING,
            WorkInfo.STOP_REASON_NOT_STOPPED,
            CameraUploadsStatusInfo.VideoCompressionOutOfSpace
        ),
        Arguments.of(
            workDataOf(STATUS_INFO to NOT_ENOUGH_STORAGE),
            WorkInfo.State.RUNNING,
            WorkInfo.STOP_REASON_NOT_STOPPED,
            CameraUploadsStatusInfo.NotEnoughStorage
        ),
        Arguments.of(
            workDataOf(STATUS_INFO to COMPRESSION_ERROR),
            WorkInfo.State.RUNNING,
            WorkInfo.STOP_REASON_NOT_STOPPED,
            CameraUploadsStatusInfo.VideoCompressionError
        ),
        Arguments.of(
            workDataOf(
                STATUS_INFO to FOLDER_UNAVAILABLE,
                FOLDER_TYPE to CameraUploadFolderType.Primary.ordinal
            ),
            WorkInfo.State.RUNNING,
            WorkInfo.STOP_REASON_NOT_STOPPED,
            CameraUploadsStatusInfo.FolderUnavailable(CameraUploadFolderType.Primary)
        ),
        Arguments.of(
            workDataOf(
                STATUS_INFO to FOLDER_UNAVAILABLE,
                FOLDER_TYPE to CameraUploadFolderType.Secondary.ordinal
            ),
            WorkInfo.State.RUNNING,
            WorkInfo.STOP_REASON_NOT_STOPPED,
            CameraUploadsStatusInfo.FolderUnavailable(CameraUploadFolderType.Secondary)
        ),
        Arguments.of(
            workDataOf(
                STATUS_INFO to FINISHED,
                FINISHED_REASON to CameraUploadsFinishedReason.COMPLETED.name,
            ),
            WorkInfo.State.RUNNING,
            WorkInfo.STOP_REASON_NOT_STOPPED,
            CameraUploadsStatusInfo.Finished(reason = CameraUploadsFinishedReason.COMPLETED)
        ),
        Arguments.of(
            workDataOf(
                STATUS_INFO to FINISHED,
                FINISHED_REASON to CameraUploadsFinishedReason.ACCOUNT_STORAGE_OVER_QUOTA.name,
            ),
            WorkInfo.State.RUNNING,
            WorkInfo.STOP_REASON_NOT_STOPPED,
            CameraUploadsStatusInfo.Finished(reason = CameraUploadsFinishedReason.ACCOUNT_STORAGE_OVER_QUOTA)
        ),
        Arguments.of(
            workDataOf(),
            WorkInfo.State.SUCCEEDED,
            WorkInfo.STOP_REASON_NOT_STOPPED,
            null,
        ),
        Arguments.of(
            workDataOf(),
            WorkInfo.State.FAILED,
            WorkInfo.STOP_REASON_NOT_STOPPED,
            null,
        ),
        Arguments.of(
            workDataOf(),
            WorkInfo.State.FAILED,
            WorkInfo.STOP_REASON_NOT_STOPPED,
            null,
        ),
        Arguments.of(
            workDataOf(),
            WorkInfo.State.FAILED,
            WorkInfo.STOP_REASON_CANCELLED_BY_APP,
            CameraUploadsStatusInfo.Finished(reason = CameraUploadsFinishedReason.SYSTEM_REASON_CANCELLED_BY_APP)
        ),
        Arguments.of(
            workDataOf(STATUS_INFO to NO_WIFI_CONNECTION),
            WorkInfo.State.RUNNING,
            WorkInfo.STOP_REASON_NOT_STOPPED,
            CameraUploadsStatusInfo.NoWifiConnection
        ),
    )
}
