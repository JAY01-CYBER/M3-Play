@Composable
fun FullPlayerScreen(song: Song) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Background Blur
        BlurredBackground(imageUrl = song.thumbnailUrl)

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Big Album Art
            Card(
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.size(320.dp).shadow(20.dp)
            ) {
                AsyncImage(model = song.thumbnailUrl, contentDescription = null)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Song Info
            Text(text = song.title, style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Text(text = song.artist, style = MaterialTheme.typography.titleMedium, color = Color.White.copy(0.7f))

            Spacer(modifier = Modifier.height(40.dp))

            // Playback Controls (Rhythm Style)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SkipPrevious, "Prev", tint = Color.White, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.width(24.dp))
                FloatingActionButton(
                    onClick = { /* Play/Pause */ },
                    shape = CircleShape,
                    containerColor = Color.White
                ) {
                    Icon(Icons.Default.PlayArrow, "Play", tint = Color.Black)
                }
                Spacer(modifier = Modifier.width(24.dp))
                Icon(Icons.Default.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(40.dp))
            }
        }
    }
}
