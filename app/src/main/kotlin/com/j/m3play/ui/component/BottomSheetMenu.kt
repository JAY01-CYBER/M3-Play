/*
 * M3Play Component Module
 *
 * Reusable UI building block
 * Signature: M3PLAY::COMPONENT::V1
 */

package com.j.m3play.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp

val LocalMenuState = compositionLocalOf { MenuState() }

@Stable
class MenuState(
    isVisible: Boolean = false,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    var isVisible by mutableStateOf(isVisible)
    var content by mutableStateOf(content)

    fun show(content: @Composable ColumnScope.() -> Unit) {
        isVisible = true
        this.content = content
    }

    fun dismiss() {
        isVisible = false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetMenu(
    modifier: Modifier = Modifier,
    state: MenuState,
    background: Color = MaterialTheme.colorScheme.surfaceContainerHigh, // Premium Pixel background
) {
    val focusManager = LocalFocusManager.current
    
    // skipPartiallyExpanded = true se sheet seedha apne content ke hisaab se khulegi
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (state.isVisible) {
        ModalBottomSheet(
            onDismissRequest = {
                focusManager.clearFocus()
                state.isVisible = false
            },
            sheetState = sheetState,
            containerColor = background,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp), // Premium Large Radius
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .size(width = 36.dp, height = 5.dp)
                        .clip(RoundedCornerShape(2.5.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                )
            },
            modifier = modifier
        ) {
            // Yahan se navigation bar ki actual height (pixels/dp) nikali ja rahi hai
            val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                state.content(this)
                
                // Actual nav bar height + 24dp extra breathing room taaki aakhri list item bilkul na kate
                Spacer(modifier = Modifier.height(bottomPadding + 24.dp))
            }
        }
    }
}
