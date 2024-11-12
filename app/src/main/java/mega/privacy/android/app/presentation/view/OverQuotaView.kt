package mega.privacy.android.app.presentation.view

import mega.privacy.android.shared.resources.R as sharedR
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.shared.original.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.extensions.textColorPrimary
import java.util.concurrent.TimeUnit

/**
 * View to display over quota text
 */
@Composable
fun OverQuotaView(
    modifier: Modifier = Modifier,
    bannerTime: Long,
    shouldShowBannerVisibility: Boolean,
    onUpgradeClicked: () -> Unit,
    onDismissClicked: () -> Unit,
) {
    var time by remember {
        mutableStateOf(TimeUtils.getHumanizedTimeMs(bannerTime))
    }
    var isTimerRunning by remember {
        mutableStateOf(shouldShowBannerVisibility)
    }

    LaunchedEffect(bannerTime) {
        if (isTimerRunning) {
            for (timer in bannerTime downTo 0L step TimeUnit.SECONDS.toMillis(1)) {
                delay(TimeUnit.SECONDS.toMillis(1))
                time = TimeUtils.getHumanizedTimeMs(timer)
            }
            time = ""
            isTimerRunning = false
        }
    }

    OverQuotaBannerText(
        time = time,
        isTimerRunning = isTimerRunning,
        onUpgradeClicked = onUpgradeClicked,
        onDismissClicked = onDismissClicked,
        modifier = modifier
    )
}

@Composable
private fun OverQuotaBannerText(
    modifier: Modifier = Modifier,
    time: String,
    isTimerRunning: Boolean,
    onUpgradeClicked: () -> Unit,
    onDismissClicked: () -> Unit,
) {
    if (isTimerRunning) {
        Column(
            modifier = modifier
                .padding(top = 22.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = stringResource(
                    id = R.string.current_text_depleted_transfer_overquota,
                    time,
                ),
                style = MaterialTheme.typography.subtitle1.copy(color = MaterialTheme.colors.textColorPrimary),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextMegaButton(
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                    text = stringResource(id = sharedR.string.general_upgrade_button),
                    onClick = { onUpgradeClicked() }
                )
                TextMegaButton(
                    modifier = Modifier
                        .padding(end = 16.dp, top = 8.dp, bottom = 8.dp),
                    text = stringResource(id = R.string.general_dismiss),
                    onClick = { onDismissClicked() }
                )
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun OverQuotaViewPreview() {
    OverQuotaView(
        bannerTime = TimeUnit.MINUTES.toMillis(10),
        shouldShowBannerVisibility = true,
        onUpgradeClicked = { },
        onDismissClicked = { })
}