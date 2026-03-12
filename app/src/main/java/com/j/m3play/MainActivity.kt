class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            M3PlayTheme { // Dynamic Colors Enable
                Scaffold(
                    bottomBar = {
                        Column {
                            RhythmMiniPlayer(onPlayPause = { /* Play logic */ })
                            // Bottom Navigation (Home, Search, Library)
                            M3BottomNav() 
                        }
                    }
                ) { padding ->
                    // Main Content Screen
                    HomeScreen(modifier = Modifier.padding(padding))
                }
            }
        }
    }
}
