package com.androhunter.app.ui.common

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.androhunter.app.ui.theme.*

@Composable
fun HunterCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color  = HunterCard,
        shape  = RoundedCornerShape(4.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, HunterBorder, RoundedCornerShape(4.dp))
    ) {
        Column(Modifier.padding(12.dp), content = content)
    }
}
