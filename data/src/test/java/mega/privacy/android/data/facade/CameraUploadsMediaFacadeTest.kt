package mega.privacy.android.data.facade

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.CameraUploadsMediaGateway
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsMedia
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever


@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CameraUploadsMediaFacadeTest {

    private lateinit var underTest: CameraUploadsMediaGateway
    private val context = mock<Context>()

    @BeforeAll
    fun setUp() {
        underTest = CameraUploadsMediaFacade(
            context = context,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            context,
        )
    }

    @Test
    fun test_that_getMediaQueue_returns_a_list_of_camera_uploads_media_fetch_from_the_media_store() =
        runTest {
            val uri = mock<Uri>()
            val selectionQuery = null

            val id = 1234L
            val displayName = "display name"
            val filePath = "file path"
            val addedDate = 0L
            val modifiedDate = 1L
            val fileSize = 1000L

            val id2 = 5678L
            val displayName2 = "display name 2"
            val filePath2 = "file path 2"
            val addedDate2 = 2L
            val modifiedDate2 = 3L
            val fileSize2 = 2000L

            val projection = arrayOf(
                "_id",
                "_display_name",
                "_data",
                "date_added",
                "date_modified",
                "_size"
            )
            val mockCursor = mock<Cursor> {
                on { getColumnIndexOrThrow(projection[0]) }.thenReturn(0)
                on { getColumnIndexOrThrow(projection[1]) }.thenReturn(1)
                on { getColumnIndexOrThrow(projection[2]) }.thenReturn(2)
                on { getColumnIndexOrThrow(projection[3]) }.thenReturn(3)
                on { getColumnIndexOrThrow(projection[4]) }.thenReturn(4)
                on { getColumnIndexOrThrow(projection[5]) }.thenReturn(5)

                on { getLong(0) }.thenReturn(id, id2)
                on { getString(1) }.thenReturn(displayName, displayName2)
                on { getString(2) }.thenReturn(filePath, filePath2)
                on { getLong(3) }.thenReturn(addedDate, addedDate2)
                on { getLong(4) }.thenReturn(modifiedDate, modifiedDate2)
                on { getLong(5) }.thenReturn(fileSize, fileSize2)

                on { moveToFirst() }.thenReturn(true)
                on { moveToNext() }.thenReturn(true, false)
            }

            val contentResolver = mock<ContentResolver> {
                on {
                    query(
                        anyOrNull(),
                        anyOrNull(),
                        anyOrNull(),
                        anyOrNull(),
                        anyOrNull()
                    )
                }.thenReturn(mockCursor)
            }
            whenever(context.contentResolver).thenReturn(contentResolver)

            val expected = listOf(
                CameraUploadsMedia(
                    mediaId = id,
                    displayName = displayName,
                    filePath = filePath,
                    timestamp = modifiedDate * 1000,
                    fileSize = fileSize,
                ),
                CameraUploadsMedia(
                    mediaId = id2,
                    displayName = displayName2,
                    filePath = filePath2,
                    timestamp = modifiedDate2 * 1000,
                    fileSize = fileSize2,
                )
            )
            assertThat(underTest.getMediaList(uri, selectionQuery)).isEqualTo(expected)
        }

    @Test
    fun test_that_getMediaSelectionQuery_returns_the_correct_selection_query() =
        runTest {
            val parentPath = "parentPath"
            val expected = "${MediaStore.MediaColumns.DATA} LIKE '$parentPath%'"

            assertThat(underTest.getMediaSelectionQuery(parentPath)).isEqualTo(expected)
        }


}
