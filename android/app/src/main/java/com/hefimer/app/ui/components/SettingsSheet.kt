package com.hefimer.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hefimer.app.BuildConfig
import com.hefimer.app.data.network.UploadProviders
import com.hefimer.app.ui.theme.AppleSegmentedControl
import com.hefimer.app.ui.theme.HefimerColors
import com.hefimer.app.ui.theme.JetBrainsMonoFamily
import com.hefimer.app.ui.theme.applePressable

@Composable
fun SettingsSheetContent(
    isLightMode: Boolean,
    defaultProviderId: String,
    historyCount: Int,
    onSetLightMode: (Boolean) -> Unit,
    onOpenProviderPicker: () -> Unit,
    onClearHistory: () -> Unit,
    onClose: () -> Unit,
) {
    val defaultCfg = UploadProviders.getConfig(defaultProviderId)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .background(HefimerColors.SurfaceSheet)
            .border(1.dp, HefimerColors.White12, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Grabber pill
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(38.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(HefimerColors.White20),
        )

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "PREFERENCES",
                    color = HefimerColors.Emerald400,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.2.sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Settings",
                    color = HefimerColors.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(HefimerColors.White06)
                    .border(1.dp, HefimerColors.White10, CircleShape)
                    .applePressable(onClick = onClose),
            ) {
                Icon(Icons.Rounded.Close, "Close", tint = HefimerColors.White60, modifier = Modifier.size(16.dp))
            }
        }

        // Appearance Mode (OLED Dark / Apple Light)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "APPEARANCE",
                color = HefimerColors.White40,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            )
            AppleSegmentedControl(
                options = listOf(false, true),
                selected = isLightMode,
                label = { if (it) "Light" else "Dark" },
                onSelect = onSetLightMode,
            )
        }

        // Default Upload Provider
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "DEFAULT UPLOAD PROVIDER",
                color = HefimerColors.White40,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(HefimerColors.White04)
                    .border(1.dp, HefimerColors.White10, RoundedCornerShape(20.dp))
                    .applePressable(pressedScale = 0.98f, onClick = onOpenProviderPicker)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(HefimerColors.EmeraldBg)
                        .border(1.dp, HefimerColors.EmeraldBorder, RoundedCornerShape(12.dp)),
                ) {
                    Icon(Icons.Rounded.Inventory2, null, tint = HefimerColors.Emerald400, modifier = Modifier.size(18.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = defaultCfg.name,
                        color = HefimerColors.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${defaultCfg.maxSizeLabel} • Auto-routes larger files",
                        color = HefimerColors.White40,
                        fontSize = 11.sp,
                    )
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = HefimerColors.White40, modifier = Modifier.size(18.dp))
            }
        }

        // Clear local history
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(HefimerColors.White04)
                .border(1.dp, HefimerColors.White10, RoundedCornerShape(20.dp))
                .applePressable(enabled = historyCount > 0, pressedScale = 0.98f, onClick = onClearHistory)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(HefimerColors.RedBg)
                    .border(1.dp, HefimerColors.RedBorder, RoundedCornerShape(12.dp)),
            ) {
                Icon(Icons.Rounded.DeleteOutline, null, tint = HefimerColors.Red400, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Clear Recent History",
                    color = if (historyCount > 0) HefimerColors.White else HefimerColors.White40,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (historyCount > 0) "$historyCount saved item${if (historyCount == 1) "" else "s"}" else "History is empty",
                    color = HefimerColors.White40,
                    fontSize = 11.sp,
                )
            }
        }

        // Version footer
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Hefimer Native",
                color = HefimerColors.White30,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                color = HefimerColors.White30,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 11.sp,
            )
        }
    }
}
