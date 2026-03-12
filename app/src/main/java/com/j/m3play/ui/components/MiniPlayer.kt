// Rhythm Style Mini Player Concept
@Composable
fun RhythmMiniPlayer(
    songName: String = "Abhi Koi Song Nahi Hai",
    artist: String = "M3-Play",
    onPlayPause: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(24.dp), // Rhythm jaisa extra round corner
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Art (YouTube Thumbnail)
            AsyncImage(
                model = "https://via.placeholder.com/50",
                contentDescription = null,
                modifier = Modifier.size(50.dp).clip(RoundedCornerShape(12.dp))
            )
            
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(text = songName, style = MaterialTheme.typography.titleMedium)
                Text(text = artist, style = MaterialTheme.typography.bodySmall)
            }
            
            // Play/Pause Button
            IconButton(onClick = onPlayPause) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play")
            }
        }
    }
}
