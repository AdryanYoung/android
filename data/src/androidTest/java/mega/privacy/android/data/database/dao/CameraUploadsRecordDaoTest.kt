package mega.privacy.android.data.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.MegaDatabase
import mega.privacy.android.data.database.entity.CameraUploadsRecordEntity
import mega.privacy.android.domain.entity.CameraUploadsRecordType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecordUploadStatus
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CameraUploadsRecordDaoTest {
    private lateinit var cameraUploadsRecordDao: CameraUploadsRecordDao
    private lateinit var db: MegaDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, MegaDatabase::class.java
        ).build()
        cameraUploadsRecordDao = db.cameraUploadsRecordDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    private fun generateEntities() = (1..10).map {
        val entity = CameraUploadsRecordEntity(
            mediaId = it.toLong(),
            timestamp = it.toLong(),
            folderType = if (it % 2 == 0) CameraUploadFolderType.Primary else CameraUploadFolderType.Secondary,
            fileName = "encryptedFileName$it",
            filePath = "encryptedFilePath$it",
            fileType = if (it < 6) CameraUploadsRecordType.TYPE_PHOTO else CameraUploadsRecordType.TYPE_VIDEO,
            uploadStatus = when {
                it < 3 -> CameraUploadsRecordUploadStatus.UPLOADED
                it < 7 -> CameraUploadsRecordUploadStatus.PENDING
                else -> CameraUploadsRecordUploadStatus.FAILED
            },
            originalFingerprint = "encryptedOriginalFingerprint$it",
            generatedFingerprint = "encryptedGeneratedFingerprint$it",
            tempFilePath = "encryptedTempFilePath$it",
            fileSize = it.toLong()
        )
        entity
    }

    private suspend fun insertEntities(entities: List<CameraUploadsRecordEntity>) {
        cameraUploadsRecordDao.insertOrUpdateCameraUploadsRecords(entities)
    }


    @Test
    fun test_that_insertOrUpdateBackup_insert_the_corresponding_items() = runTest {
        val entities = generateEntities()
        val expected = entities.size

        insertEntities(entities)

        assertThat(cameraUploadsRecordDao.getAllCameraUploadsRecords().size)
            .isEqualTo(expected)
    }

    @Test
    fun test_that_insertOrUpdateBackup_update_the_corresponding_items() = runTest {
        val entities = generateEntities()
        val expected = entities.size

        insertEntities(entities)
        insertEntities(entities)

        assertThat(cameraUploadsRecordDao.getAllCameraUploadsRecords().size)
            .isEqualTo(expected)
    }

    @Test
    fun test_that_getCameraUploadsRecordByUploadStatusAndTypes_returns_the_corresponding_items() =
        runTest {
            val entities = generateEntities()
            val status = listOf(
                CameraUploadsRecordUploadStatus.PENDING,
                CameraUploadsRecordUploadStatus.FAILED,
            )
            val types = listOf(CameraUploadsRecordType.TYPE_PHOTO)
            val folderTypes = listOf(CameraUploadFolderType.Primary)
            val expected =
                entities.filter {
                    status.contains(it.uploadStatus)
                            && types.contains(it.fileType)
                            && folderTypes.contains(it.folderType)
                }.size

            insertEntities(entities)

            assertThat(
                cameraUploadsRecordDao
                    .getCameraUploadsRecordsBy(status, types, folderTypes).size,
            ).isEqualTo(expected)
        }

    @Test
    fun test_that_updateCameraUploadsRecordUploadStatus_update_the_status_of_the_corresponding_item() =
        runTest {
            val entities = generateEntities()
            val expected = CameraUploadsRecordUploadStatus.LOCAL_FILE_NOT_EXIST

            insertEntities(entities)

            val recordToUpdate = entities[0]
            cameraUploadsRecordDao.updateCameraUploadsRecordUploadStatus(
                recordToUpdate.mediaId,
                recordToUpdate.timestamp,
                recordToUpdate.folderType,
                expected,
            )

            assertThat(
                cameraUploadsRecordDao.getAllCameraUploadsRecords().single {
                    it.mediaId == recordToUpdate.mediaId
                            && it.timestamp == recordToUpdate.timestamp
                            && it.folderType == recordToUpdate.folderType
                }.uploadStatus
            ).isEqualTo(expected)
        }

    @Test
    fun test_that_updateCameraUploadsRecordGeneratedFingerprint_update_the_generated_fingerprint_of_the_corresponding_item() =
        runTest {
            val entities = generateEntities()
            val expected = "generatedFingerprint"

            insertEntities(entities)

            val recordToUpdate = entities[0]
            cameraUploadsRecordDao.updateCameraUploadsRecordGeneratedFingerprint(
                recordToUpdate.mediaId,
                recordToUpdate.timestamp,
                recordToUpdate.folderType,
                expected,
            )

            assertThat(
                cameraUploadsRecordDao.getAllCameraUploadsRecords().single {
                    it.mediaId == recordToUpdate.mediaId
                            && it.timestamp == recordToUpdate.timestamp
                            && it.folderType == recordToUpdate.folderType
                }.generatedFingerprint
            ).isEqualTo(expected)
        }

    @Test
    fun test_that_deleteCameraUploadsRecordsByFolderType_delete_all_the_corresponding_items() =
        runTest {
            val entities = generateEntities()
            val folderTypes = listOf(
                CameraUploadFolderType.Primary,
            )
            val expected = entities.filter { !folderTypes.contains(it.folderType) }.size

            insertEntities(entities)

            cameraUploadsRecordDao.deleteCameraUploadsRecordsByFolderType(folderTypes)

            assertThat(cameraUploadsRecordDao.getAllCameraUploadsRecords().size).isEqualTo(expected)
        }

    @Test
    fun test_that_getAllCameraUploadsRecords_returns_all_the_corresponding_items() =
        runTest {
            val expected = generateEntities()
            insertEntities(expected)
            assertThat(cameraUploadsRecordDao.getAllCameraUploadsRecords()).isEqualTo(expected)
        }

    @Test
    fun test_that_getAllCameraUploadsRecords_returns_all_the_corresponding_file_sizes() =
        runTest {
            val expected = generateEntities()
            insertEntities(expected)
            val result = cameraUploadsRecordDao.getAllCameraUploadsRecords()
            result.forEachIndexed { index, entity ->
                assertThat(entity.fileSize).isEqualTo(expected[index].fileSize)
            }
        }
}
