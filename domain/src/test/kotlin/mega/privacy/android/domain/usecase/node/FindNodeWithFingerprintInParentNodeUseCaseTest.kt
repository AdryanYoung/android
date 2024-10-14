package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
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

/**
 * Test class for [FindNodeWithFingerprintInParentNodeUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FindNodeWithFingerprintInParentNodeUseCaseTest {

    private lateinit var underTest: FindNodeWithFingerprintInParentNodeUseCase

    private val getNodeFromCloudDriveUseCase = mock<GetNodeFromCloudDriveUseCase>()
    private val isNodeInRubbishBinUseCase = mock<IsNodeInRubbishBinUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = FindNodeWithFingerprintInParentNodeUseCase(
            getNodeFromCloudDriveUseCase = getNodeFromCloudDriveUseCase,
            isNodeInRubbishBinUseCase = isNodeInRubbishBinUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getNodeFromCloudDriveUseCase,
            isNodeInRubbishBinUseCase,
        )
    }

    @ParameterizedTest(name = "when isNodeInParentNode is {0} and isNodeInOtherNode is {1} and isNodeInRubbishBin is {2}, a pair with {3} and the NodeId is returned")
    @MethodSource("provideParameters")
    fun `test that the node exists in the parent folder`(
        isNodeInParentNode: Boolean,
        isNodeInOtherNode: Boolean,
        isNodeInRubbishBin: Boolean,
        expectedIsNodeExistingInParentFolder: Boolean?,
    ) = runTest {
        val fingerprint = "fingerprint"
        val generatedFingerprint = "generatedFingerprint"

        val parentNodeId = mock<NodeId> {
            on { longValue }.thenReturn(1111L)
        }

        val expectedNode =
            when {
                isNodeInParentNode -> {
                    mock<TypedFileNode> {
                        on { id }.thenReturn(NodeId(1234L))
                        on { parentId }.thenReturn(NodeId(1111L))
                    }
                }

                isNodeInOtherNode || isNodeInRubbishBin -> {
                    mock {
                        on { id }.thenReturn(NodeId(1234L))
                        on { parentId }.thenReturn(NodeId(2222L))
                    }
                }

                else -> null
            }

        whenever(getNodeFromCloudDriveUseCase(fingerprint, generatedFingerprint, parentNodeId))
            .thenReturn(expectedNode)
        expectedNode?.let {
            whenever(isNodeInRubbishBinUseCase(it.id)).thenReturn(isNodeInRubbishBin)
        }

        assertThat(underTest(fingerprint, generatedFingerprint, parentNodeId))
            .isEqualTo(Pair(expectedIsNodeExistingInParentFolder, expectedNode?.id))
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(true, false, false, true),
        Arguments.of(false, true, false, false),
        Arguments.of(false, false, true, null),
        Arguments.of(false, false, false, false),
    )
}
