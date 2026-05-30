/*
 * ╭────────────────────────────────────────────╮
 * │             M3Play UI System               │
 * │--------------------------------------------│
 * │  Crafted for expressive music experience   │
 * │                                            │
 * │  Signature: M3PLAY::UI::EXPRESSIVE::V2     │
 * ╰────────────────────────────────────────────╯
 */

package com.j.m3play.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.R
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.utils.backToMain

// Data class updated to hold raw markdown lines
data class ChangeLog(
    val version: String,
    val isLatest: Boolean = false,
    val lines: List<String>,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    
    // Automatically reads and parses the CHANGELOG.md file from assets
    val changelog = remember {
        val parsedList = mutableListOf<ChangeLog>()
        try {
            // Reading the file from assets folder
            val text = context.assets.open("CHANGELOG.md").bufferedReader().use { it.readText() }
            var currentVersion = ""
            var currentLines = mutableListOf<String>()
            var isFirst = true

            text.lines().forEach { line ->
                if (line.startsWith("---v")) {
                    if (currentVersion.isNotEmpty()) {
                        parsedList.add(ChangeLog(currentVersion, isFirst, currentLines))
                        isFirst = false
                    }
                    currentVersion = line.removePrefix("---")
                    currentLines = mutableListOf()
                } else if (currentVersion.isNotEmpty() && line.isNotBlank()) {
                    currentLines.add(line)
                }
            }
            if (currentVersion.isNotEmpty()) {
                parsedList.add(ChangeLog(currentVersion, isFirst, currentLines))
            }
        } catch (e: Exception) {
            // Fallback error message if file is not found
            parsedList.add(
                ChangeLog(
                    version = "Error",
                    isLatest = true,
                    lines = listOf("Could not load CHANGELOG.md from assets folder.")
                )
            )
        }
        parsedList
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Changelog") },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                    ),
                )
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            item {
                GlassCard {
                    Text(
                        text = "What's New",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Reading directly from CHANGELOG.md",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            items(changelog) { item ->
                GlassCard(highlight = item.isLatest) {
                    androidx.compose.foundation.layout.Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = item.version,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (item.isLatest) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )

                        if (item.isLatest) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            ) {
                                Text(
                                    text = "NEW",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Premium Markdown Renderer
                    item.lines.forEach { line ->
                        when {
                            line.startsWith("# ") -> {
                                Text(
                                    text = line.removePrefix("# "),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                )
                            }
                            line.startsWith("## ") -> {
                                Text(
                                    text = line.removePrefix("## "),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                                )
                            }
                            line.startsWith("- ") -> {
                                Text(
                                    text = "• ${line.removePrefix("- ")}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                                )
                            }
                            line.startsWith("> ") -> {
                                // Red color formatting for Warning messages
                                val warningText = line.removePrefix("> ").removePrefix("[!WARNING]").trim()
                                if (warningText.isNotEmpty()) {
                                    Text(
                                        text = warningText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }
                            }
                            else -> {
                                Text(
                                    text = line,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
            }

            item {
                Text(
                    text = "This changelog is bundled inside app and loads offline.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 12.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun GlassCard(
    highlight: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val baseContainer = if (highlight) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier.background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        baseContainer,
                        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.72f),
                    ),
                ),
                shape = RoundedCornerShape(32.dp),
            ),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = baseContainer,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    content = content,
                )
            }
        }
    }
}
