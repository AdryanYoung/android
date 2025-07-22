package mega.privacy.android.app.presentation.view

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.domain.entity.node.ExportedData
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.legacy.core.ui.controls.lists.EXPORTED_TEST_TAG
import mega.privacy.android.legacy.core.ui.controls.lists.FAVORITE_TEST_TAG
import mega.privacy.android.legacy.core.ui.controls.lists.INFO_TEXT_TEST_TAG
import mega.privacy.android.legacy.core.ui.controls.lists.SELECTED_TEST_TAG
import mega.privacy.android.legacy.core.ui.controls.lists.TAKEN_TEST_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class NodesViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    private val fileTypeIconMapper: FileTypeIconMapper = mock()

    private val exportedData = ExportedData("link", 123L)

    @Test
    fun `test when list item is selected then is shows selected image`() = runTest {
        val node: TypedFolderNode = mock()
        whenever(node.name).thenReturn("Some name")
        whenever(node.childFileCount).thenReturn(1)
        whenever(node.childFolderCount).thenReturn(2)

        composeTestRule.setContent {
            NodesView(
                modifier = Modifier,
                nodeUIItems = listOf(
                    NodeUIItem<TypedNode>(
                        node = node,
                        isSelected = true,
                        isInvisible = false
                    )
                ),
                onItemClicked = {},
                onMenuClick = {},
                onLongClick = {},
                onSortOrderClick = {},
                onChangeViewTypeClick = {},
                isListView = false,
                sortOrder = "Any Name",
                onLinkClicked = {},
                onDisputeTakeDownClicked = {},
                fileTypeIconMapper = fileTypeIconMapper,
            )
        }
        composeTestRule.run {
            onAllNodes(hasTestTag(SELECTED_TEST_TAG))
        }
    }

    @Test
    fun `test when list item is favorite and exported then it shows favorite icon`() = runTest {
        val node: TypedFolderNode = mock()
        whenever(node.name).thenReturn("Some name")
        whenever(node.childFileCount).thenReturn(1)
        whenever(node.childFolderCount).thenReturn(2)
        whenever(node.isFavourite).thenReturn(true)
        whenever(node.exportedData).thenReturn(exportedData)

        composeTestRule.setContent {
            NodesView(
                modifier = Modifier,
                nodeUIItems = listOf(
                    NodeUIItem<TypedNode>(
                        node = node,
                        isSelected = true,
                        isInvisible = false
                    )
                ),
                onItemClicked = {},
                onMenuClick = {},
                onLongClick = {},
                onSortOrderClick = {},
                onChangeViewTypeClick = {},
                isListView = false,
                sortOrder = "Any Name",
                onLinkClicked = {},
                onDisputeTakeDownClicked = {},
                fileTypeIconMapper = fileTypeIconMapper,
            )
        }
        composeTestRule.run {
            onAllNodes(hasTestTag(FAVORITE_TEST_TAG))
            onAllNodes(hasTestTag(EXPORTED_TEST_TAG).not())
            onAllNodes(hasTestTag(TAKEN_TEST_TAG).not())
        }
    }

    @Test
    fun `test when list item is exported then it shows folder exported icon`() = runTest {
        val node: TypedFolderNode = mock()
        whenever(node.name).thenReturn("Some name")
        whenever(node.childFileCount).thenReturn(1)
        whenever(node.childFolderCount).thenReturn(2)
        whenever(node.exportedData).thenReturn(exportedData)

        composeTestRule.setContent {
            NodesView(
                modifier = Modifier,
                nodeUIItems = listOf(
                    NodeUIItem<TypedNode>(
                        node = node,
                        isSelected = true,
                        isInvisible = false
                    )
                ),
                onItemClicked = {},
                onMenuClick = {},
                onLongClick = {},
                onSortOrderClick = {},
                onChangeViewTypeClick = {},
                isListView = false,
                sortOrder = "Any Name",
                onLinkClicked = {},
                onDisputeTakeDownClicked = {},
                fileTypeIconMapper = fileTypeIconMapper,
            )
        }
        composeTestRule.run {
            onAllNodes(hasTestTag(EXPORTED_TEST_TAG))
            onAllNodes(hasTestTag(FAVORITE_TEST_TAG).not())
            onAllNodes(hasTestTag(TAKEN_TEST_TAG).not())
        }
    }

    @Test
    fun `test when list item is taken down favorite and exported then it shows favorite, exported and taken down icon`() =
        runTest {
            val node: TypedFolderNode = mock()
            whenever(node.name).thenReturn("Some name")
            whenever(node.childFileCount).thenReturn(1)
            whenever(node.childFolderCount).thenReturn(2)
            whenever(node.isTakenDown).thenReturn(true)
            whenever(node.exportedData).thenReturn(exportedData)
            whenever(node.isFavourite).thenReturn(true)

            composeTestRule.setContent {
                NodesView(
                    modifier = Modifier,
                    nodeUIItems = listOf(
                        NodeUIItem<TypedNode>(
                            node = node,
                            isSelected = false,
                            isInvisible = false
                        )
                    ),
                    onItemClicked = {},
                    onMenuClick = {},
                    onLongClick = {},
                    onSortOrderClick = {},
                    onChangeViewTypeClick = {},
                    isListView = false,
                    sortOrder = "Any Name",
                    onLinkClicked = {},
                    onDisputeTakeDownClicked = {},
                    fileTypeIconMapper = fileTypeIconMapper,
                )
            }
            composeTestRule.run {
                onAllNodes(hasTestTag(EXPORTED_TEST_TAG))
                onAllNodes(hasTestTag(FAVORITE_TEST_TAG))
                onAllNodes(hasTestTag(TAKEN_TEST_TAG))
            }
        }

    @Test
    fun `test when grid item is folder info then it does not show info text`() = runTest {
        val node: TypedFolderNode = mock()
        whenever(node.name).thenReturn("Some name")
        whenever(node.childFileCount).thenReturn(1)
        whenever(node.childFolderCount).thenReturn(2)

        composeTestRule.setContent {
            NodesView(
                modifier = Modifier,
                nodeUIItems = listOf(
                    NodeUIItem<TypedNode>(
                        node = node,
                        isSelected = true,
                        isInvisible = false
                    )
                ),
                onItemClicked = {},
                onMenuClick = {},
                onLongClick = {},
                onSortOrderClick = {},
                onChangeViewTypeClick = {},
                isListView = true,
                sortOrder = "Any Name",
                onLinkClicked = {},
                onDisputeTakeDownClicked = {},
                fileTypeIconMapper = fileTypeIconMapper,
            )
        }
        composeTestRule.run {
            onAllNodes(hasTestTag(INFO_TEXT_TEST_TAG).not())
        }
    }
}