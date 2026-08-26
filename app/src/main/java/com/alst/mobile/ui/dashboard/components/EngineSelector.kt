package com.alst.mobile.ui.dashboard.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alst.mobile.domain.model.EngineType

@Composable
fun EngineSelector(
    selectedEngine: EngineType,
    onEngineSelected: (EngineType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val engines = EngineType.entries

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        backgroundColor = Color(0x15FFFFFF),
        borderColor = Color(0x25FFFFFF),
        borderWidth = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            engines.forEach { engine ->
                val isSelected = selectedEngine == engine
                val targetBg = if (isSelected) Color(0x3500E5FF) else Color.Transparent
                val targetBorder = if (isSelected) Color(0x9000E5FF) else Color.Transparent
                val targetTextColor = if (isSelected) Color.White else Color(0x99FFFFFF)
                val targetIconColor = if (isSelected) Color(0xFF00E5FF) else Color(0x70FFFFFF)

                val animBg by animateColorAsState(targetValue = targetBg, animationSpec = tween(250), label = "bg")
                val animBorder by animateColorAsState(targetValue = targetBorder, animationSpec = tween(250), label = "border")

                val shape = RoundedCornerShape(16.dp)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(shape)
                        .background(animBg)
                        .border(
                            width = 1.dp,
                            brush = if (isSelected) {
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF00E5FF).copy(alpha = 0.8f),
                                        Color(0xFF00B0FF).copy(alpha = 0.4f)
                                    )
                                )
                            } else {
                                Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                            },
                            shape = shape
                        )
                        .clickable { onEngineSelected(engine) }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = when (engine) {
                                EngineType.OFFLINE -> Icons.Default.PhoneAndroid
                                EngineType.ONLINE_GEMINI -> Icons.Default.AutoAwesome
                            },
                            contentDescription = null,
                            tint = targetIconColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(engine.displayNameResId),
                            color = targetTextColor,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
