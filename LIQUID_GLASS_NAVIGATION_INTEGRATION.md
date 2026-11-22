# Liquid Glass Navigation

The app keeps its original Classic Floating Navigation Toolbar and adds a selectable Liquid Glass style.

The Liquid Glass toolbar now follows an Apple Music-inspired floating tab-bar layout:
- one continuous translucent capsule with blur, vibrancy, lens distortion, highlights, and inner depth;
- four equal-width destinations/actions (the app's three primary destinations plus More);
- a smoothly animated glass selection capsule;
- the existing Identify Music, Mood & Genres, and Shuffle actions remain under More;
- the existing navigation destinations and callbacks are reused.

Choose the style in Appearance Settings. The preference is persisted using the existing DataStore preference key.

Build verification: not completed in this environment because the Gradle distribution host (`services.gradle.org`) could not be reached. Please run `./gradlew :app:compileDebugKotlin` in Android Studio or an environment with Gradle network access.


## White-theme and interaction refinement
- Increased the light-surface milk-glass opacity and added a subtle outer rim for contrast on white artwork/backgrounds.
- Kept the dark-theme smoky glass treatment.
- Added spring-based press deformation to each tab and the More segment.
- Increased the selection spring responsiveness and refined the selected capsule rim.
- Liquid lens, vibrancy, blur, highlight and shadow effects continue to use Kyant0 Backdrop APIs.
