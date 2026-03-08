package com.androhunter.app.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.androhunter.app.ui.theme.*

@Composable
fun HunterTopBar(title: String, onBack: (() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = HunterGreen)
            }
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text       = "[ $title ]",
            color      = HunterGreen,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            style      = MaterialTheme.typography.titleMedium
        )
    }
}
