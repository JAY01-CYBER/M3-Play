@Composable
fun BlurredBackground(imageUrl: String) {
    AsyncImage(
        model = imageUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxSize()
            .blur(radius = 30.dp) // Rhythm jaisa heavy blur
            .alpha(0.4f) // Background ko thoda halka rakhne ke liye
    )
}
