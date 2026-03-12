@Composable
fun SearchScreen() {
    var searchQuery by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Rhythm style Search Bar
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search songs, artists...") },
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            colors = TextFieldDefaults.textFieldColors(focusedIndicatorColor = Color.Transparent)
        )
        
        // Results yahan list mein aayenge
        LazyColumn {
            items(5) {
                SongListItem(title = "Searching for $searchQuery...")
            }
        }
    }
}
