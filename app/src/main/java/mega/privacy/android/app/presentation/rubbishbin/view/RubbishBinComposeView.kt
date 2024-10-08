package mega.privacy.android.app.presentation.rubbishbin.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.rubbishbin.model.RubbishBinState
import mega.privacy.android.app.presentation.view.NODES_EMPTY_VIEW_VISIBLE
import mega.privacy.android.app.presentation.view.NodesView
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.legacy.core.ui.controls.LegacyMegaEmptyView

/**
 * View for RubbishBinComposeFragment
 * @param uiState [RubbishBinState]
 * @param onMenuClick
 * @param onItemClicked
 * @param onLongClick
 * @param onSortOrderClick
 * @param onChangeViewTypeClick
 * @param sortOrder
 * @param emptyState
 */
@Composable
fun RubbishBinComposeView(
    uiState: RubbishBinState,
    onMenuClick: (NodeUIItem<TypedNode>) -> Unit,
    onItemClicked: (NodeUIItem<TypedNode>) -> Unit,
    onLongClick: (NodeUIItem<TypedNode>) -> Unit,
    onSortOrderClick: () -> Unit,
    onChangeViewTypeClick: () -> Unit,
    sortOrder: String,
    emptyState: Pair<Int, Int>,
    onLinkClicked: (String) -> Unit,
    onDisputeTakeDownClicked: (String) -> Unit,
    fileTypeIconMapper: FileTypeIconMapper,
) {
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    if (uiState.nodeList.isNotEmpty()) {
        NodesView(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            nodeUIItems = uiState.nodeList,
            onMenuClick = onMenuClick,
            onItemClicked = onItemClicked,
            onLongClick = onLongClick,
            sortOrder = sortOrder,
            isListView = uiState.currentViewType == ViewType.LIST,
            onSortOrderClick = onSortOrderClick,
            onChangeViewTypeClick = onChangeViewTypeClick,
            listState = listState,
            gridState = gridState,
            onLinkClicked = onLinkClicked,
            onDisputeTakeDownClicked = onDisputeTakeDownClicked,
            fileTypeIconMapper = fileTypeIconMapper,
            inSelectionMode = uiState.isInSelection,
            shouldApplySensitiveMode = uiState.accountType?.isPaid == true
                    && !uiState.isBusinessAccountExpired,
        )
    } else {
        LegacyMegaEmptyView(
            modifier = Modifier.testTag(NODES_EMPTY_VIEW_VISIBLE),
            imagePainter = painterResource(id = emptyState.first),
            text = stringResource(id = emptyState.second)
        )
    }
}