package com.j.m3play.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.j.m3play.LocalPlayerAwareWindowInsets
import com.j.m3play.R
import com.j.m3play.ui.component.IconButton
import com.j.m3play.ui.utils.backToMain

data class ChangeLog(
    val version: String,
    val isLatest: Boolean,
    val lines: List<String>
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
            parsedList.add(ChangeLog(version = "Error", isLatest = true, lines = listOf("Could not load CHANGELOG.md from assets folder.")))
        }
        parsedList
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(text = "Changelog", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        val bottomInsets = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal)),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomInsets + 40.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("What's New", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Loaded directly from CHANGELOG.md", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    }
                }
            }

            items(changelog) { item ->
                ExpressiveVersionCard(item = item)
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Text(
                    text = "M3Play • Offline Changelog System",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ExpressiveVersionCard(item: ChangeLog) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(painter = painterResource(R.drawable.update), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Text(text = item.version, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                }
                if (item.isLatest) {
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Text(text = "Latest", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item.lines.forEach { line ->
                    if (line.startsWith("!warning")) {
                        WarningBox(text = line.removePrefix("!warning").trim())
                    } else {
                        FormattedText(text = line)
                    }
                }
            }
        }
    }
}

@Composable
private fun WarningBox(text: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painter = painterResource(R.drawable.info), contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(text = text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

@Composable
private fun FormattedText(text: String) {
    val annotatedString = buildAnnotatedString {
        if (text.startsWith("- ")) {
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
                append("• ")
            }
            append(text.removePrefix("- ").trim())
        } else {
            append(text)
        }
    }
    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = if (text.startsWith("- ")) 4.dp else 0.dp)
    )
}
