package mega.privacy.android.domain.usecase.createaccount

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.AccountBlockedEvent
import mega.privacy.android.domain.entity.AccountConfirmationEvent
import mega.privacy.android.domain.entity.Event
import mega.privacy.android.domain.entity.StorageStateEvent
import mega.privacy.android.domain.entity.UnknownEvent
import mega.privacy.android.domain.repository.NotificationsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorAccountConfirmationUseCaseTest {

    private lateinit var underTest: MonitorAccountConfirmationUseCase

    private lateinit var notificationsRepository: NotificationsRepository

    @BeforeAll
    fun setup() {
        notificationsRepository = mock()
        underTest = MonitorAccountConfirmationUseCase(notificationsRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(notificationsRepository)
    }

    @ParameterizedTest(
        name = " {2} when monitor event returns event with type {1}"
    )
    @MethodSource("provideParameters")
    fun `test that monitor account confirmation returns `(
        event: Event,
        eventType: String,
        expectedResult: Boolean?,
    ) = runTest {
        whenever(notificationsRepository.monitorEvent()).thenReturn(flowOf(event))
        underTest().test {
            expectedResult?.let {
                val result = awaitItem()
                awaitComplete()
                assertThat(result).isEqualTo(it)
            } ?: awaitComplete()
        }
    }

    private fun provideParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(mock<AccountBlockedEvent>(), "AccountBlockedEvent", null),
        Arguments.of(mock<AccountConfirmationEvent>(), "AccountConfirmationEvent", true),
        Arguments.of(mock<UnknownEvent>(), "UnknownEvent", null),
        Arguments.of(mock<StorageStateEvent>(), "StorageStateEvent", null)
    )
}
