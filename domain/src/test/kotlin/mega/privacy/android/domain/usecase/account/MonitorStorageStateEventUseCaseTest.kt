package mega.privacy.android.domain.usecase.account

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.CommitDbEvent
import mega.privacy.android.domain.entity.DisconnectEvent
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.StorageStateEvent
import mega.privacy.android.domain.repository.NotificationsRepository
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class MonitorStorageStateEventUseCaseTest {

    private lateinit var underTest: MonitorStorageStateEventUseCase

    private val notificationsRepository = mock<NotificationsRepository>()

    private val exampleStorageStateEvent = StorageStateEvent(
        handle = 1L,
        storageState = StorageState.Unknown
    )

    private val exampleNormalEvent = CommitDbEvent(
        handle = 1L,
    )

    @Test
    fun `test that initial storage state is Unknown`() = runTest {
        whenever(notificationsRepository.monitorEvent()).thenReturn(flowOf())
        underTest = MonitorStorageStateEventUseCase(
            notificationsRepository = notificationsRepository,
            scope = this
        )

        underTest().test {
            assertThat(awaitItem().storageState).isEqualTo(StorageState.Unknown)
        }
    }

    @Test
    fun `test that storage state event is passed`() = runTest {
        whenever(notificationsRepository.monitorEvent()).thenReturn(
            flowOf(
                exampleStorageStateEvent.copy(storageState = StorageState.PayWall)
            )
        )
        underTest = MonitorStorageStateEventUseCase(
            notificationsRepository = notificationsRepository,
            scope = this
        )

        underTest().test {
            assertThat(awaitItem().storageState).isEqualTo(StorageState.Unknown)
            assertThat(awaitItem().storageState).isEqualTo(StorageState.PayWall)
        }
    }

    @Test
    fun `test that only storage state events are received if there are multiple types of events`() =
        runTest {
            whenever(notificationsRepository.monitorEvent()).thenReturn(
                flowOf(
                    mock<CommitDbEvent>(),
                    exampleStorageStateEvent.copy(
                        storageState = StorageState.Red
                    ),
                    mock<DisconnectEvent>(),
                    exampleStorageStateEvent.copy(
                        storageState = StorageState.Green
                    ),
                    exampleStorageStateEvent.copy(
                        storageState = StorageState.Orange
                    ),
                )
            )
            underTest = MonitorStorageStateEventUseCase(
                notificationsRepository = notificationsRepository,
                scope = this
            )

            underTest().test {
                assertThat(awaitItem().storageState).isEqualTo(StorageState.Unknown)
                assertThat(awaitItem().storageState).isEqualTo(StorageState.Red)
                assertThat(awaitItem().storageState).isEqualTo(StorageState.Green)
                assertThat(awaitItem().storageState).isEqualTo(StorageState.Orange)
            }
            assertThat(underTest().value.storageState).isEqualTo(StorageState.Orange)
        }
}