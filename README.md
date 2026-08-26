# JetForge KMP runtime

Kotlin Multiplatform library (`:jetforge`) plus a sample app (`:sample`) that mix **ordinary Compose screens** with **server-driven JetForge screens**.

This repository is **only** the native runtime. The visual designer is a separate project: [Compose Studio / jet-forge](https://github.com/bhargavcode/jet-forge).

Android and iOS share one renderer. Publish a document in Compose Studio, then draw it with native Compose Multiplatform.

**`JetForgeScreen` and `JetForgeComponent` never use a WebView.** They map the published JSON tree onto Material 3 composables and fire the same HTTP request the designer configured (headers, query, JSON / form / multipart body).

```kotlin
JetForge.configure(JetForgeConfig(baseUrl = "https://your-studio.example.com"))

// Full screen — shimmer skeleton until the document arrives
JetForgeScreen(endpoint = "us-briefing")

// Embedded region inside a screen you already own
JetForgeComponent(
    endpoint = "us-briefing",
    modifier = Modifier.fillMaxWidth().height(240.dp),
)
```

`endpoint` can be a screen id (`us-briefing`), a path (`/api/screens/us-briefing`), or an absolute URL.

## Add it to an existing KMP app

1. Copy the `jetforge/` module into your project, or `includeBuild("../jetforge-kmp")`.
2. In `settings.gradle.kts`: `include(":jetforge")`
3. In your app module:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":jetforge"))
        }
    }
}
```

4. Call `JetForge.configure` once at startup. On the Android emulator use `http://10.0.2.2:43145`; on iOS simulator / desktop use `http://127.0.0.1:43145`. Cleartext HTTP must be allowed for local studios (`android:usesCleartextTraffic="true"`).

### SwiftUI

The iOS framework exposes `JetForgeViewController(endpoint:baseUrl:)`. Wrap it:

```swift
JetForgeScreenView(endpoint: "us-briefing", baseUrl: "http://127.0.0.1:43145")
    .ignoresSafeArea(.keyboard)
```

## Sample app

The sample is a shop-style shell:

| Tab | What it is |
| --- | --- |
| **Home** | Traditional Compose (copy, button, local state) plus an embedded `JetForgeComponent` |
| **Briefing** | A full `JetForgeScreen` for the published US Briefing document |
| **Settings** | Traditional form to change studio URL and endpoint |

### Desktop

```bash
./gradlew :sample:run
```

### Android Studio

Open this repository. Set `sdk.dir` in `local.properties`. Run the `sample` Android configuration.

### iOS

Open `iosApp` after Android Studio / Fleet has generated the `Sample` framework, or run the iOS app from a Compose Multiplatform run configuration. `MainViewController()` hosts the same `App()`.

## How loading works

1. `GET {baseUrl}/api/screens/{id}` — published JSON document (schema v3).
2. Shimmer skeleton while that request is in flight.
3. `POST {baseUrl}/api/bind` (falls back to calling each data source URL directly with the configured method, headers, query, and body) — same bindings the web runtime uses.
4. Placeholder images (`/api/assets/...` or a static URL) show until the bound API field (for example `item.image`) resolves.
5. Touch events from the document (`tap`, `doubleTap`, `longPress`, `swipeLeft` / `Right` / `Up` / `Down`) run inside the interpreter. Your app keeps its own back stack for traditional screens.

Do not generate Kotlin per screen. Keep this module in the app and treat published JSON as configuration.

## Tests

```bash
./gradlew :jetforge:jvmTest
```
