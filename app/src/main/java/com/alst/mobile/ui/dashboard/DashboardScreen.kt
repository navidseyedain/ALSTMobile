package com.alst.mobile.ui.dashboard

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alst.mobile.R
import com.alst.mobile.domain.model.EngineType
import com.alst.mobile.service.ScreenTranslatorService
import com.alst.mobile.ui.dashboard.components.EngineSelector
import com.alst.mobile.ui.dashboard.components.GlassCard
import com.alst.mobile.ui.dashboard.components.LanguageDropdown
import com.alst.mobile.ui.dashboard.components.LanguageModelListSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var passwordVisible by remember { mutableStateOf(false) }

    // Actively check permissions and model statuses whenever app is resumed
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermissions()
                viewModel.refreshModels()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Ambient Cyber-Dark Gradient with glowing blur orbs
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF090D16),
                        Color(0xFF0F1422),
                        Color(0xFF0B0D17)
                    )
                )
            )
    ) {
        // Glowing Ambient Light Spheres (gives real depth to frosted glass)
        Box(
            modifier = Modifier
                .size(260.dp)
                .offset(x = (-40).dp, y = 80.dp)
                .blur(90.dp)
                .background(Color(0x2200E5FF), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.CenterEnd)
                .offset(x = 60.dp, y = 60.dp)
                .blur(90.dp)
                .background(Color(0x187C4DFF), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-30).dp, y = 30.dp)
                .blur(80.dp)
                .background(Color(0x1500B0FF), CircleShape)
        )

        Scaffold(
            containerColor = Color.Transparent,
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // -- Header Title & Branding --
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "ALST",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "AI Live Screen Translation",
                            color = Color(0xFF00E5FF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.3.sp
                        )
                    }

                    // Status Pill Badge
                    GlassCard(
                        shape = RoundedCornerShape(20.dp),
                        backgroundColor = if (state.isServiceEnabled) Color(0x2500E676) else Color(0x18FFFFFF),
                        borderColor = if (state.isServiceEnabled) Color(0x6000E676) else Color(0x30FFFFFF),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (state.isServiceEnabled) Color(0xFF00E676) else Color(0x80FFFFFF))
                            )
                            Text(
                                text = if (state.isServiceEnabled) "Active" else "Standby",
                                color = if (state.isServiceEnabled) Color(0xFF00E676) else Color(0x99FFFFFF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // -- Master Power Card (Hero) --
                GlassCard(
                    shape = RoundedCornerShape(24.dp),
                    backgroundColor = if (state.isServiceEnabled) Color(0x2000E5FF) else Color(0x12FFFFFF),
                    borderColor = if (state.isServiceEnabled) Color(0x5000E5FF) else Color(0x25FFFFFF),
                    borderWidth = 1.2.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(if (state.isServiceEnabled) Color(0x3300E5FF) else Color(0x15FFFFFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PowerSettingsNew,
                                    contentDescription = null,
                                    tint = if (state.isServiceEnabled) Color(0xFF00E5FF) else Color(0x88FFFFFF),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.master_toggle_label),
                                    style = androidx.compose.ui.text.TextStyle(
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = if (state.isServiceEnabled) "Floating button is visible" else "Tap to activate floating button",
                                    fontSize = 12.sp,
                                    color = if (state.isServiceEnabled) Color(0xCC00E5FF) else Color(0x88FFFFFF)
                                )
                            }
                        }

                        Switch(
                            checked = state.isServiceEnabled,
                            onCheckedChange = { isChecked ->
                                viewModel.toggleService(isChecked)
                                val intent = Intent(context, ScreenTranslatorService::class.java)
                                if (isChecked) {
                                    intent.action = ScreenTranslatorService.ACTION_INIT
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        context.startForegroundService(intent)
                                    } else {
                                        context.startService(intent)
                                    }
                                } else {
                                    intent.action = ScreenTranslatorService.ACTION_STOP_SERVICE
                                    context.startService(intent)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF00B0FF),
                                uncheckedThumbColor = Color(0x88FFFFFF),
                                uncheckedTrackColor = Color(0x22FFFFFF),
                                uncheckedBorderColor = Color(0x30FFFFFF)
                            )
                        )
                    }
                }

                // -- Engine Selector Section --
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.engine_selector_title),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF),
                        letterSpacing = 0.5.sp
                    )
                    EngineSelector(
                        selectedEngine = state.engineType,
                        onEngineSelected = { viewModel.setEngineType(it) },
                    )
                }

                // -- Gemini API Key Glass Card --
                AnimatedVisibility(
                    visible = state.engineType == EngineType.ONLINE_GEMINI,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    GlassCard(
                        shape = RoundedCornerShape(22.dp),
                        backgroundColor = Color(0x14FFFFFF),
                        borderColor = Color(0x25FFFFFF)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Google Gemini API Key",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            OutlinedTextField(
                                value = state.geminiApiKey,
                                onValueChange = { viewModel.setGeminiApiKey(it) },
                                placeholder = {
                                    Text(
                                        stringResource(R.string.api_key_hint),
                                        color = Color(0x60FFFFFF),
                                        fontSize = 13.sp
                                    )
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (passwordVisible) "Hide" else "Show",
                                            tint = Color(0xAAFFFFFF),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0x20FFFFFF),
                                    unfocusedContainerColor = Color(0x10FFFFFF),
                                    unfocusedBorderColor = Color(0x25FFFFFF),
                                    focusedBorderColor = Color(0xFF00E5FF),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = Color(0xFF00E5FF)
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            val intent = Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse("https://aistudio.google.com/app/apikey")
                                            )
                                            context.startActivity(intent)
                                        }
                                        .padding(vertical = 6.dp, horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = "Get API Key here",
                                        color = Color(0xFF00E5FF),
                                        fontSize = 12.sp,
                                        textDecoration = TextDecoration.Underline,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Icon(
                                        imageVector = Icons.Default.OpenInNew,
                                        contentDescription = null,
                                        tint = Color(0xFF00E5FF),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }

                                Button(
                                    onClick = {
                                        focusManager.clearFocus()
                                        Toast.makeText(context, "API Key saved securely", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF00B0FF),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Save,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text("Save", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // -- Target Language Section --
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.target_language_label),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF),
                        letterSpacing = 0.5.sp
                    )
                    LanguageDropdown(
                        selectedLanguageCode = state.targetLanguage,
                        downloadedLanguages = state.downloadedLanguages,
                        isOnlineMode = state.engineType == EngineType.ONLINE_GEMINI,
                        onLanguageSelected = { viewModel.setTargetLanguage(it) },
                    )
                }

                // -- Manage Offline Models Button (Visible only in Offline Mode) --
                AnimatedVisibility(
                    visible = state.engineType == EngineType.OFFLINE,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleLanguageManager(true) },
                        shape = RoundedCornerShape(18.dp),
                        backgroundColor = Color(0x12FFFFFF),
                        borderColor = Color(0x25FFFFFF)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x2200E5FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.ocr_models_title),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Download & manage offline language packages",
                                    color = Color(0x88FFFFFF),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // -- Overlay Permission Card --
                GlassCard(
                    shape = RoundedCornerShape(20.dp),
                    backgroundColor = if (state.hasOverlayPermission) Color(0x1800E676) else Color(0x18FF5252),
                    borderColor = if (state.hasOverlayPermission) Color(0x4500E676) else Color(0x40FF5252),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Icon(
                            imageVector = if (state.hasOverlayPermission)
                                Icons.Default.CheckCircle
                            else
                                Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (state.hasOverlayPermission)
                                Color(0xFF00E676)
                            else
                                Color(0xFFFF5252),
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.overlay_permission_title),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (state.hasOverlayPermission)
                                    stringResource(R.string.permission_granted)
                                else
                                    stringResource(R.string.overlay_permission_desc),
                                fontSize = 11.sp,
                                color = Color(0xAAFFFFFF)
                            )
                        }
                        if (!state.hasOverlayPermission) {
                            Button(
                                onClick = {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}"),
                                    )
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF5252),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    stringResource(R.string.grant_permission),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            if (state.showLanguageManager) {
                LanguageModelListSheet(
                    allModels = state.allLanguageModels,
                    onDownload = { viewModel.downloadLanguageModel(it) },
                    onDelete = { viewModel.deleteLanguageModel(it) },
                    onRefresh = { viewModel.refreshModels() },
                    onDismiss = { viewModel.toggleLanguageManager(false) },
                )
            }
        }
    }
}
