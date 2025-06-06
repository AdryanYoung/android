package mega.privacy.android.data.mapper.transfer.completed

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.cryptography.DecryptData
import mega.privacy.android.data.database.entity.CompletedTransferEntityLegacy
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CompletedTransferLegacyModelMapperTest {
    private lateinit var underTest: CompletedTransferLegacyModelMapper

    private val decryptData: DecryptData = mock()


    @BeforeAll
    fun setup() {
        underTest = CompletedTransferLegacyModelMapper(decryptData)
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            decryptData,
        )
    }

    @Test
    fun `test that mapper returns model correctly when invoke function`() = runTest {
        val entity = CompletedTransferEntityLegacy(
            id = 0,
            fileName = "2023-03-24 00.13.20_1.jpg",
            type = "1",
            state = "6",
            size = "3.57 MB",
            handle = "27169983390750",
            path = "Cloud drive/Camera uploads",
            isOffline = "false",
            timestamp = "1684228012974",
            error = "No error",
            originalPath = "/data/user/0/mega.privacy.android.app/cache/cu/53132573053997.2023-03-24 00.13.20_1.jpg",
            parentHandle = "11622336899311",
            appData = "appData",
        )
        val expected = CompletedTransfer(
            id = 0,
            fileName = "2023-03-24 00.13.20_1.jpg",
            type = 1,
            state = 6,
            size = "3.57 MB",
            handle = 27169983390750L,
            path = "Cloud drive/Camera uploads",
            isOffline = false,
            timestamp = 1684228012974L,
            error = "No error",
            originalPath = "/data/user/0/mega.privacy.android.app/cache/cu/53132573053997.2023-03-24 00.13.20_1.jpg",
            parentHandle = 11622336899311L,
            appData = "appData",
            displayPath = null,
            errorCode = null,
        )
        whenever(decryptData(entity.fileName)).thenReturn(entity.fileName)
        whenever(decryptData(entity.type)).thenReturn(entity.type)
        whenever(decryptData(entity.state)).thenReturn(entity.state)
        whenever(decryptData(entity.size)).thenReturn(entity.size)
        whenever(decryptData(entity.handle)).thenReturn(entity.handle)
        whenever(decryptData(entity.path)).thenReturn(entity.path)
        whenever(decryptData(entity.isOffline)).thenReturn(entity.isOffline)
        whenever(decryptData(entity.timestamp)).thenReturn(entity.timestamp)
        whenever(decryptData(entity.error)).thenReturn(entity.error)
        whenever(decryptData(entity.originalPath)).thenReturn(entity.originalPath)
        whenever(decryptData(entity.parentHandle)).thenReturn(entity.parentHandle)
        whenever(decryptData(entity.appData)).thenReturn(entity.appData)

        Truth.assertThat(underTest(entity)).isEqualTo(expected)
    }

    @Test
    fun `test that mapper returns default value when decrypt fails`() = runTest {
        val entity = CompletedTransferEntityLegacy(
            id = 0,
            fileName = "2023-03-24 00.13.20_1.jpg",
            type = "1",
            state = "6",
            size = "3.57 MB",
            handle = "27169983390750",
            path = "Cloud drive/Camera uploads",
            isOffline = "false",
            timestamp = "1684228012974",
            error = "No error",
            originalPath = "/data/user/0/mega.privacy.android.app/cache/cu/53132573053997.2023-03-24 00.13.20_1.jpg",
            parentHandle = "11622336899311",
            appData = "appData",
        )
        val expected = CompletedTransfer(
            id = 0,
            fileName = "",
            type = -1,
            state = -1,
            size = "",
            handle = -1L,
            path = "",
            isOffline = null,
            timestamp = -1L,
            error = null,
            originalPath = "",
            parentHandle = -1L,
            appData = null,
            displayPath = null,
            errorCode = null,
        )
        whenever(decryptData(entity.fileName)).thenReturn(null)
        whenever(decryptData(entity.type)).thenReturn(null)
        whenever(decryptData(entity.state)).thenReturn(null)
        whenever(decryptData(entity.size)).thenReturn(null)
        whenever(decryptData(entity.handle)).thenReturn(null)
        whenever(decryptData(entity.path)).thenReturn(null)
        whenever(decryptData(entity.isOffline)).thenReturn(null)
        whenever(decryptData(entity.timestamp)).thenReturn(null)
        whenever(decryptData(entity.error)).thenReturn(null)
        whenever(decryptData(entity.originalPath)).thenReturn(null)
        whenever(decryptData(entity.parentHandle)).thenReturn(null)
        whenever(decryptData(entity.appData)).thenReturn(null)

        Truth.assertThat(underTest(entity)).isEqualTo(expected)
    }

    @Test
    fun `test that mapper returns default value when parsing the string to specific types fails`() =
        runTest {
            val entity = CompletedTransferEntityLegacy(
                id = 0,
                fileName = "2023-03-24 00.13.20_1.jpg",
                type = "nonIntValue",
                state = "nonIntValue",
                size = "3.57 MB",
                handle = "nonLongValue",
                path = "Cloud drive/Camera uploads",
                isOffline = "false",
                timestamp = "nonLongValue",
                error = "No error",
                originalPath = "/data/user/0/mega.privacy.android.app/cache/cu/53132573053997.2023-03-24 00.13.20_1.jpg",
                parentHandle = "nonLongValue",
                appData = "appData",
            )
            val expected = CompletedTransfer(
                id = 0,
                fileName = "2023-03-24 00.13.20_1.jpg",
                type = -1,
                state = -1,
                size = "3.57 MB",
                handle = -1L,
                path = "Cloud drive/Camera uploads",
                isOffline = false,
                timestamp = -1L,
                error = "No error",
                originalPath = "/data/user/0/mega.privacy.android.app/cache/cu/53132573053997.2023-03-24 00.13.20_1.jpg",
                parentHandle = -1L,
                appData = "appData",
                displayPath = null,
                errorCode = null,
            )
            whenever(decryptData(entity.fileName)).thenReturn(entity.fileName)
            whenever(decryptData(entity.type)).thenReturn(entity.type)
            whenever(decryptData(entity.state)).thenReturn(entity.state)
            whenever(decryptData(entity.size)).thenReturn(entity.size)
            whenever(decryptData(entity.handle)).thenReturn(entity.handle)
            whenever(decryptData(entity.path)).thenReturn(entity.path)
            whenever(decryptData(entity.isOffline)).thenReturn(entity.isOffline)
            whenever(decryptData(entity.timestamp)).thenReturn(entity.timestamp)
            whenever(decryptData(entity.error)).thenReturn(entity.error)
            whenever(decryptData(entity.originalPath)).thenReturn(entity.originalPath)
            whenever(decryptData(entity.parentHandle)).thenReturn(entity.parentHandle)
            whenever(decryptData(entity.appData)).thenReturn(entity.appData)

            Truth.assertThat(underTest(entity)).isEqualTo(expected)
        }
}
