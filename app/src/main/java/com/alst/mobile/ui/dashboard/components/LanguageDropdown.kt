package com.alst.mobile.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alst.mobile.R
import com.alst.mobile.domain.model.SupportedLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageDropdown(
    selectedLanguageCode: String,
    downloadedLanguages: List<SupportedLanguage>,
    isOnlineMode: Boolean,
    onLanguageSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val selectedLanguage = SupportedLanguage.fromCode(selectedLanguageCode)
    val focusManager = LocalFocusManager.current

    val baseLanguages = if (isOnlineMode) SupportedLanguage.entries else downloadedLanguages

    val displayLanguages = if (searchQuery.isNotBlank()) {
        baseLanguages.filter {
            it.displayName.contains(searchQuery, ignoreCase = true) ||
            it.nativeName.contains(searchQuery, ignoreCase = true)
        }
    } else {
        baseLanguages
    }

    val selectedText = if (selectedLanguage != null && (isOnlineMode || downloadedLanguages.contains(selectedLanguage))) {
        "${selectedLanguage.nativeName} (${selectedLanguage.displayName})"
    } else if (!isOnlineMode && downloadedLanguages.isEmpty()) {
        stringResource(R.string.no_languages_downloaded)
    } else {
        stringResource(R.string.target_language_label)
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            if (baseLanguages.isNotEmpty()) {
                expanded = it
                if (!it) focusManager.clearFocus()
            }
        },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = if (expanded) searchQuery else selectedText,
            onValueChange = {
                searchQuery = it
                expanded = true
            },
            readOnly = false,
            label = {
                Text(
                    text = if (expanded) "Search language..." else stringResource(R.string.target_language_label),
                    color = if (expanded) Color(0xFF00E5FF) else Color(0xAAFFFFFF),
                    fontSize = 12.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Translate,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0x20FFFFFF),
                unfocusedContainerColor = Color(0x12FFFFFF),
                focusedBorderColor = Color(0xFF00E5FF),
                unfocusedBorderColor = Color(0x25FFFFFF),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color(0xFF00E5FF),
            ),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryEditable)
                .fillMaxWidth(),
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
                focusManager.clearFocus()
            },
            modifier = Modifier
                .background(Color(0xF0181A26), RoundedCornerShape(16.dp))
        ) {
            if (displayLanguages.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No results found", color = Color(0x80FFFFFF)) },
                    onClick = { },
                    enabled = false
                )
            } else {
                displayLanguages.forEach { language ->
                    val isCurrent = language.code == selectedLanguageCode
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "${language.nativeName} (${language.displayName})",
                                color = if (isCurrent) Color(0xFF00E5FF) else Color.White,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        },
                        trailingIcon = {
                            if (isCurrent) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        onClick = {
                            onLanguageSelected(language.code)
                            expanded = false
                            searchQuery = ""
                            focusManager.clearFocus()
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .background(
                                if (isCurrent) Color(0x2500E5FF) else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                    )
                }
            }
        }
    }
}
