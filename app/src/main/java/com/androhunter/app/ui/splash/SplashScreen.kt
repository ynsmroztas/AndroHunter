package com.androhunter.app.ui.splash

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.androhunter.app.core.AppLanguage
import com.androhunter.app.core.LanguageManager
import com.androhunter.app.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashLanguageScreen(onLanguageSelected: () -> Unit) {
    var phase         by remember { mutableStateOf(0) } // 0=logo, 1=lang select
    var logoAlpha     by remember { mutableStateOf(0f) }
    var selectedLang  by remember { mutableStateOf<AppLanguage?>(null) }

    val animAlpha by animateFloatAsState(
        targetValue = logoAlpha,
        animationSpec = tween(1200),
        label = "alpha"
    )

    // Pulse animation for crosshair
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )

    LaunchedEffect(Unit) {
        logoAlpha = 1f
        delay(2000)
        phase = 1
    }

    Box(
        Modifier.fillMaxSize().background(HunterBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            // ASCII Logo + Crosshair
            Box(
                Modifier
                    .size(120.dp)
                    .scale(if (phase == 0) pulseScale else 1f)
                    .alpha(animAlpha)
                    .border(2.dp, HunterGreen, RoundedCornerShape(60.dp))
                    .background(HunterCard, RoundedCornerShape(60.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⊕", color = HunterGreen, fontSize = 48.sp)
                }
            }

            Text(
                "ANDROHUNTER",
                color      = HunterGreen,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize   = 28.sp,
                modifier   = Modifier.alpha(animAlpha)
            )
            Text(
                "Android Security Toolkit v2.0",
                color      = HunterTextDim,
                fontFamily = FontFamily.Monospace,
                fontSize   = 13.sp,
                modifier   = Modifier.alpha(animAlpha)
            )

            // Language selection — appears after logo phase
            AnimatedVisibility(
                visible = phase == 1,
                enter   = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 2 }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Select Language / Dil Seçin",
                        color      = HunterTextDim,
                        fontFamily = FontFamily.Monospace,
                        fontSize   = 14.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        AppLanguage.values().forEach { lang ->
                            val isSelected = selectedLang == lang
                            Box(
                                Modifier
                                    .weight(1f)
                                    .clickable { selectedLang = lang }
                                    .background(
                                        if (isSelected) HunterGreen.copy(alpha = 0.15f) else HunterCard,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) HunterGreen else HunterBorder,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(vertical = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(lang.flag,   fontSize = 32.sp)
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        lang.displayName,
                                        color      = if (isSelected) HunterGreen else HunterTextDim,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize   = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = selectedLang != null) {
                        Button(
                            onClick = {
                                selectedLang?.let {
                                    LanguageManager.setLanguage(it)
                                    LanguageManager.setFirstLaunchDone()
                                    onLanguageSelected()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = HunterGreen),
                            shape    = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "[ CONTINUE / DEVAM ET ]",
                                color      = HunterBg,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize   = 15.sp
                            )
                        }
                    }
                }
            }
        }

        // Scanline effect
        repeat(20) { i ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .offset(y = (i * 40).dp)
                    .alpha(0.03f)
                    .background(HunterGreen)
            )
        }
    }
}
