package mega.privacy.android.shared.original.core.ui.controls.pager.indicator

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.shared.original.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * The indicator [Composable] for a [HorizontalPager].
 *
 * @param pageSize The pager size.
 * @param currentPage The current selected page.
 * @param modifier A modifier instance to be applied to this Pager's indicator outer layout.
 * @param horizontalArrangement The horizontal arrangement of the indicators.
 */
@Composable
fun HorizontalPagerIndicator(
    pageSize: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Center,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
) {
    Row(
        modifier = modifier.testTag(HORIZONTAL_PAGER_INDICATOR_TAG),
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = verticalAlignment
    ) {
        repeat(pageSize) { iteration ->
            val color = if (currentPage == iteration) {
                DSTokens.colors.icon.accent
            } else {
                DSTokens.colors.icon.onColorDisabled
            }
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(color = color)
                    .size(8.dp)
            )
        }
    }
}

@CombinedTextAndThemePreviews
@Composable
private fun HorizontalPagerIndicatorPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        HorizontalPagerIndicator(
            pageSize = 10,
            currentPage = 5
        )
    }
}

internal const val HORIZONTAL_PAGER_INDICATOR_TAG = "horizontal_pager_indicator:indicators"
