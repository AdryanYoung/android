package mega.privacy.android.app.presentation.node.model.menuaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.original.core.ui.model.MenuActionWithIcon
import javax.inject.Inject

/**
 * Dispute take down menu action
 */
class DisputeTakeDownMenuAction @Inject constructor() : MenuActionWithIcon {
    @Composable
    override fun getDescription() = stringResource(id = R.string.dispute_takendown_file)

    @Composable
    override fun getIconPainter() = rememberVectorPainter(IconPack.Medium.Regular.Outline.AlertTriangle)

    override val orderInCategory = 100

    override val testTag: String = "menu_action:Dispute_take_down"
}