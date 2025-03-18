package mega.privacy.android.domain.usecase.network

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.ConnectivityState
import mega.privacy.android.domain.repository.NetworkRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@OptIn(ExperimentalCoroutinesApi::class)
class MonitorConnectivityUseCaseTest {
    private lateinit var underTest: MonitorConnectivityUseCase

    private val networkRepository = mock<NetworkRepository>()

    @Before
    fun setUp() {
        underTest = MonitorConnectivityUseCase(
            networkRepository = networkRepository,
        )
    }

    @Test
    fun `test that subsequent states have their connected property returned`() = runTest {
        val connectivityFlow = MutableStateFlow(ConnectivityState.Disconnected)
        networkRepository.stub {
            onBlocking { getCurrentConnectivityState() }.thenReturn(
                ConnectivityState.Connected(true)
            )
            on { monitorConnectivityChanges() }.thenReturn(connectivityFlow)
        }

        underTest().test {
            assertThat(awaitItem()).isTrue()
            assertThat(awaitItem()).isFalse()
        }
    }

    @Test
    fun `test that connectivity state is updated if it changes`() = runTest {
        val connectivityFlow = MutableStateFlow<ConnectivityState>(ConnectivityState.Disconnected)
        networkRepository.stub {
            onBlocking { getCurrentConnectivityState() }.thenReturn(
                ConnectivityState.Connected(
                    false
                )
            )
            on { monitorConnectivityChanges() }.thenReturn(connectivityFlow)
        }

        underTest().test {
            assertThat(awaitItem()).isTrue()
            assertThat(awaitItem()).isFalse()
            connectivityFlow.emit(ConnectivityState.Connected(true))
            assertThat(awaitItem()).isTrue()
        }
    }
}
