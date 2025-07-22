package mega.privacy.android.app.presentation.node.view

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.NavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import mega.privacy.android.app.presentation.node.NodeActionHandler
import mega.privacy.android.app.presentation.node.NodeOptionsBottomSheetViewModel
import mega.privacy.android.app.presentation.node.model.NodeBottomSheetState
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFolderNode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class NodeOptionsBottomSheetTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val viewModel = mock<NodeOptionsBottomSheetViewModel>()
    private val nodeActionHandler = mock<NodeActionHandler>()
    private val navHostController = mock<NavHostController>()
    private val fileTypeIconMapper = FileTypeIconMapper()

    private val node = mock<TypedFolderNode> {
        on { name }.thenReturn("name")
        on { childFileCount }.thenReturn(1)
        on { childFolderCount }.thenReturn(1)
    }

    @Test
    fun `test that a single group has no dividers`() {
        viewModel.stub {
            on { state }.thenReturn(MutableStateFlow(
                NodeBottomSheetState(
                    name = "",
                    isOnline = true,
                    node = node,
                    actions = persistentListOf(persistentListOf(
                        BottomSheetMenuItem(
                            group = 1,
                            orderInGroup = 1,
                            control = { _, _, _, _ -> }
                        )
                    )),
                ),
            ))
        }

        composeTestRule.setContent {
            NodeOptionsBottomSheetContent(
                handler = nodeActionHandler,
                viewModel = viewModel,
                navHostController = navHostController,
                nodeId = 1L,
                onDismiss = {},
                nodeSourceType = NodeSourceType.CLOUD_DRIVE,
                fileTypeIconMapper = fileTypeIconMapper
            )
        }

        composeTestRule.onNodeWithText("name").assertExists()
        composeTestRule.onNodeWithTag(DIVIDER_TAG + 0).assertDoesNotExist()
    }

    @Test
    fun `test that two groups have a divider`() {
        viewModel.stub {
            on { state }.thenReturn(MutableStateFlow(
                NodeBottomSheetState(
                    name = "",
                    isOnline = true,
                    node = node,
                    actions = persistentListOf(
                        persistentListOf(
                            BottomSheetMenuItem(
                                group = 1,
                                orderInGroup = 1,
                                control = { _, _, _, _ -> }
                            )),
                        persistentListOf(
                            BottomSheetMenuItem(
                                group = 2,
                                orderInGroup = 1,
                                control = { _, _, _, _ -> }
                            )
                        )
                    ),
                ),
            ))
        }

        composeTestRule.setContent {
            NodeOptionsBottomSheetContent(
                handler = nodeActionHandler,
                viewModel = viewModel,
                navHostController = navHostController,
                nodeId = 1L,
                onDismiss = {},
                nodeSourceType = NodeSourceType.CLOUD_DRIVE,
                fileTypeIconMapper = fileTypeIconMapper
            )
        }

        composeTestRule.onNodeWithText("name").assertExists()
        composeTestRule.onNodeWithTag(DIVIDER_TAG + 0).assertExists()
    }
}