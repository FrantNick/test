# Focus Flight Android App

This repository contains the offline Kotlin/Jetpack Compose implementation of **Focus Flight**, a productivity timer that turns deep work sessions into immersive flights.

## Build Requirements

- Android Studio Giraffe (or newer) / IntelliJ with Android support
- Android SDK 34
- JDK 17+

## Building & Running

1. Open the project in Android Studio and let Gradle sync.
2. Build an APK with **Build ▸ Build Bundle(s) / APK(s) ▸ Build APK(s)** or run on a device/emulator with **Run ▸ Run 'app'**.
3. From the command line you can assemble a debug APK with the Gradle wrapper (generate it via Android Studio or `gradle wrapper` if it is not already present):
   ```bash
   ./gradlew assembleDebug
   ```

The resulting APK will be located at `app/build/outputs/apk/debug/app-debug.apk`.

All data (flight catalog, ambient audio synthesis, progress history) is stored locally so the app works completely offline.
