# Renew Android sample

A small Compose app that drives `renew-kt` directly.

<p align="center">
  <img src="docs/screenshot.png" alt="Renew sample app — InGracePeriod state with allowed and rejected events" width="320" />
</p>

The current state is shown at the top. Every event is rendered as a button — **filled** if the state machine accepts it from the current state, **outlined** if it would be rejected. Tapping a rejected event surfaces the exact reason `renew-kt` returned, so you can see the rules apply in real time.

A history strip at the bottom shows the last 12 transitions (and rejections).

## Run

```bash
./gradlew :samples:android:installDebug
adb shell am start -n com.rishabhships.renew.sample/.MainActivity
```

Or open the project in Android Studio (Hedgehog or newer) and run the `:samples:android` configuration on an emulator / device with API 24+.

## What this demonstrates

- `SubscriptionStateMachine` as the single source of truth for what's allowed
- Wiring `renew-kt` into a Compose `ViewModel` with a `StateFlow<SubscriptionState>`
- Pre-evaluating every event against the current state to drive UI affordances (filled vs. outlined buttons)
- Capturing `TransitionResult.Invalid` so rejections surface to the user rather than vanishing into a log

## No Android dependency in the library itself

The library at `:lib` is plain Kotlin/JVM. This sample applies the AGP plugin and depends on `:lib` via Gradle:

```kotlin
// samples/android/build.gradle.kts
dependencies {
    implementation(project(":lib"))
    // … Compose, lifecycle-viewmodel-compose, activity-compose
}
```

You can copy the same pattern in your own Android project once `renew-kt` ships to Maven Central (planned for v0.3).
