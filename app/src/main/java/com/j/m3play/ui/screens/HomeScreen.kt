@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2), // Rhythm jaisa 2-column grid
        modifier = modifier.fillMaxSize().padding(8.dp)
    ) {
        items(10) {
            SongCard(title = "Song Title", artist = "Artist Name")
        }
    }
}

@Composable
fun SongCard(title: String, artist: String) {
    Column(modifier = Modifier.padding(8.dp)) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.aspectRatio(1f)
        ) {
            // Image yahan aayegi
        }
        Text(text = title, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
        Text(text = artist, style = MaterialTheme.typography.bodySmall, maxLines = 1)
    }
}
