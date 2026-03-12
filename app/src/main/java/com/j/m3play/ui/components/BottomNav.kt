@Composable
fun M3BottomNav(navController: NavController) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp // Rhythm jaisa flat look
    ) {
        val items = listOf("Home", "Search", "Library")
        val icons = listOf(Icons.Default.Home, Icons.Default.Search, Icons.Default.List)

        items.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = { Icon(icons[index], contentDescription = item) },
                label = { Text(item) },
                selected = false, // Baad mein logic add karenge
                onClick = { /* Navigation logic */ }
            )
        }
    }
}
