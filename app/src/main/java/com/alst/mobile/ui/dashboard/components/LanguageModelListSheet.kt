package com.alst.mobile.ui.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alst.mobile.R
import com.alst.mobile.domain.model.LanguageModelInfo
import com.alst.mobile.domain.model.SupportedLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageModelListSheet(
    allModels: List<LanguageModelInfo>,
    onDownload: (SupportedLanguage) -> Unit,
    onDelete: (SupportedLanguage) -> Unit,
    onRefresh: () -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xF5111320),
        scrimColor = Color.Black.copy(alpha = 0.65f),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.ocr_models_title),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                androidx.compose.material3.IconButton(onClick = onRefresh) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh & Check Updates",
                        tint = Color(0xFF00E5FF)
                    )
                }
            }
            Text(
                text = "All downloaded models are synced and up-to-date with Google ML Kit",
                color = Color(0x99FFFFFF),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 16.dp),
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                items(
                    items = allModels,
                    key = { it.language.code },
                ) { modelInfo ->
                    LanguageModelItem(
                        modelInfo = modelInfo,
                        onDownload = { onDownload(modelInfo.language) },
                        onDelete = { onDelete(modelInfo.language) },
                    )
                }
            }
        }
    }
}
