package mega.privacy.android.domain.usecase.camerauploads.mapper

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.CameraUploadsRecordType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsMedia
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecord
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecordUploadStatus
import mega.privacy.android.domain.usecase.GetDeviceCurrentNanoTimeUseCase
import mega.privacy.android.domain.usecase.file.GetFileByPathUseCase
import mega.privacy.android.domain.usecase.file.GetFingerprintUseCase
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.io.File

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CameraUploadsRecordMapperTest {
    lateinit var underTest: CameraUploadsRecordMapper

    private val getFingerprintUseCase = mock<GetFingerprintUseCase>()
    private val getDeviceCurrentNanoTimeUseCase = mock<GetDeviceCurrentNanoTimeUseCase>()
    private val getFileByPathUseCase = mock<GetFileByPathUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = CameraUploadsRecordMapper(
            getFingerprintUseCase = getFingerprintUseCase,
            getDeviceCurrentNanoTimeUseCase = getDeviceCurrentNanoTimeUseCase,
            getFileByPathUseCase = getFileByPathUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getFingerprintUseCase,
            getDeviceCurrentNanoTimeUseCase,
        )
    }

    @Test
    fun `test that the camera uploads media is mapped correctly to a camera uploads media record`() =
        runTest {
            val media = CameraUploadsMedia(
                mediaId = 1234L,
                displayName = "displayName.jpeg",
                filePath = "filePath",
                timestamp = 1L
            )
            val cameraUploadFolderType = mock<CameraUploadFolderType>()
            val type = mock<CameraUploadsRecordType>()
            val tempRoot = "tempRoot"

            val fingerprint = "fingerprint"
            whenever(getFingerprintUseCase(media.filePath)).thenReturn(fingerprint)
            val currentNanoTime = 1111L
            whenever(getDeviceCurrentNanoTimeUseCase()).thenReturn(currentNanoTime)
            val fileSize = 12345L
            val mockFile = mock<File> {
                on { length() }.thenReturn(fileSize)
            }
            whenever(getFileByPathUseCase(any())).thenReturn(mockFile)

            val actual = underTest(
                media = media,
                folderType = cameraUploadFolderType,
                fileType = type,
                tempRoot = tempRoot
            )

            val expected = CameraUploadsRecord(
                mediaId = media.mediaId,
                fileName = media.displayName,
                filePath = media.filePath,
                timestamp = media.timestamp,
                folderType = cameraUploadFolderType,
                type = type,
                uploadStatus = CameraUploadsRecordUploadStatus.PENDING,
                originalFingerprint = fingerprint,
                generatedFingerprint = null,
                tempFilePath = "$tempRoot$currentNanoTime.jpeg",
                fileSize = fileSize
            )
            assertThat(actual).isEqualTo(expected)
        }

    @Test
    fun `test that if fingerprint is null then the camera uploads record returned is null`() =
        runTest {
            val media = CameraUploadsMedia(
                mediaId = 1234L,
                displayName = "displayName.jpeg",
                filePath = "filePath",
                timestamp = 1L
            )
            val cameraUploadFolderType = mock<CameraUploadFolderType>()
            val type = mock<CameraUploadsRecordType>()
            val tempRoot = "tempRoot"

            whenever(getFingerprintUseCase(media.filePath)).thenReturn(null)

            val actual = underTest(
                media = media,
                folderType = cameraUploadFolderType,
                fileType = type,
                tempRoot = tempRoot
            )

            assertThat(actual).isNull()
        }

}
