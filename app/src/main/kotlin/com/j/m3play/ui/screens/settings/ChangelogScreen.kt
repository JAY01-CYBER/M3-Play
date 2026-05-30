/*
 * ╭────────────────────────────────────────────╮
 * │             M3Play UI System               │
 * │--------------------------------------------│
 * │  Crafted for expressive music experience   │
 * │                                            │
 * │  Signature: M3PLAY::UI::EXPRESSIVE::V3     │
 * ╰────────────────────────────────────────────╯
 */

package com.j.m3play.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.R
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.utils.backToMain

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

    val changelog = remember {
        val parsedList = mutableListOf<ChangeLog>()
        try {
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
                title = { 
                    Text(
                        text = "Changelog", 
                        fontWeight = FontWeight.Bold 
                    ) 
                },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = "Back",
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
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
            verticalArrangement = Arrangement.spacedBy(20.dp), // MD3 Expressive spacing
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Expressive Hero Card for Header
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = "What's New",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Loaded directly from CHANGELOG.md",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        )
                    }
                }
            }

            items(changelog) { item ->
                ExpressiveVersionCard(item = item)
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
                Text(
                    text = "M3-Play • Offline Changelog System",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ExpressiveVersionCard(item: ChangeLog) {
    // MD3 Elevated Card for beautiful depth
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp), // Large corner radius for expressive look
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (item.isLatest) 6.dp else 2.dp
        ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (item.isLatest) 
                MaterialTheme.colorScheme.surfaceVariant 
            else 
                MaterialTheme.colorScheme.surface,
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = item.version,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isLatest) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )

                if (item.isLatest) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Text(
                            text = "LATEST",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // State to group warning lines together
            val warningBlock = mutableListOf<String>()

            item.lines.forEach { line ->
                if (line.startsWith(">")) {
                    warningBlock.add(line.removePrefix(">").trim())
                } else {
                    if (warningBlock.isNotEmpty()) {
                        WarningBox(warningBlock)
                        warningBlock.clear()
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    when {
                        line.startsWith("# ") -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = line.removePrefix("# "),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        line.startsWith("## ") -> {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = line.removePrefix("## "),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                        line.startsWith("- ") -> {
                            FormattedText(
                                text = "•  ${line.removePrefix("- ")}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                            )
                        }
                        else -> {
                            FormattedText(
                                text = line,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                }
            }
            
            // Flush any remaining warnings at the end
            if (warningBlock.isNotEmpty()) {
                WarningBox(warningBlock)
                warningBlock.clear()
            }
        }
    }
}

@Composable
private fun WarningBox(lines: List<String>) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            lines.forEach { line ->
                val cleanLine = line.removePrefix("[!WARNING]").trim()
                if (cleanLine.isNotEmpty()) {
                    FormattedText(
                        text = cleanLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}

// Helper to convert Markdown **bold** tags into actual Compose Bold text
@Composable
private fun FormattedText(
    text: String, 
    style: androidx.compose.ui.text.TextStyle, 
    color: androidx.compose.ui.graphics.Color, 
    modifier: Modifier = Modifier
) {
    val annotatedString = buildAnnotatedString {
        val parts = text.split("**")
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) { 
                // Odd index means it was wrapped in **
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(part)
                }
            } else {
                append(part)
            }
        }
    }
    Text(
        text = annotatedString,
        style = style,
        color = color,
        modifier = modifier
    )
}
