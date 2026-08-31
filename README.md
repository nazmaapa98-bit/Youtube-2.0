# 🎵 YT Player (Hybrid Android YouTube Player with Background Audio & Auto-Update)

An Android application built with Jetpack Compose, WebView, Media3 (ExoPlayer), and NewPipe Extractor.

---

## 🌟 Key Features

1. **100% Authentic YouTube Web Interface:** Full YouTube features including Google Sign-in and recommendations.
2. **Audio Mode / Data Saver (Magic Button):** One-tap switch to audio-only stream via NewPipe Extractor (~98% data savings).
3. **Background & Lock Screen Playback:** Media3 `MediaSessionService` with lock screen controls, notification actions, and Bluetooth headset button support.
4. **Multi-Layer Ad Blocking:** Network domain interception + cosmetic element hiding + automated video ad fast-forward & skip.
5. **Picture-in-Picture (PiP):** Auto-enters PiP on swipe-to-home gesture with playback controls.
6. **🔄 Auto In-App Updates:** When you push updates to GitHub, GitHub Actions automatically builds a signed APK and creates a release. Opening the app shows an in-app update popup with a one-tap update download and install.

---

## 🚀 Setup & Push to GitHub

### Step 1: Set your GitHub Username & Repository Name

Open `app/src/main/java/com/tonmoy/ytplayer/util/Constants.kt` and change:
```kotlin
const val GITHUB_OWNER = "YOUR_GITHUB_USERNAME"  // e.g. "tonmoy"
const val GITHUB_REPO = "YT-Player"               // your repository name
```

### Step 2: Push to GitHub

In your terminal / PowerShell:
```bash
git add .
git commit -m "Initial commit: YT Player with CI/CD and In-App Update"
git remote add origin https://github.com/YOUR_GITHUB_USERNAME/YT-Player.git
git push -u origin main
```

---

## ⚙️ GitHub Actions & Continuous Deployment (CI/CD)

Every time you push to the `main` branch, `.github/workflows/build-release.yml` will automatically:
1. Build a signed Release APK using JDK 17 & Gradle.
2. Create a new GitHub Release with the tag `v<versionName>`.
3. Upload `YTPlayer-v<versionName>.apk`.

### How to Release an Update in Future:
When you want to update the app on your phone:
1. Make your code changes.
2. Bump the version in `app/build.gradle.kts`:
   ```kotlin
   versionCode = 2        // Increment by 1
   versionName = "1.0.1"  // Increment version
   ```
3. Commit and push to GitHub:
   ```bash
   git add .
   git commit -m "Update feature X"
   git push origin main
   ```
4. GitHub Actions will build and publish the release.
5. **Open the app on your phone** ➔ An update popup will appear ➔ Tap **Download** ➔ Tap **Install**!

---

## 🔑 Signing Keystore Information

A persistent release keystore has already been generated at `app/release.keystore`.

If you keep your GitHub repository **Private**, the keystore is used directly.
If you use a **Public** repository, add the following GitHub Actions Secret (`Settings -> Secrets and variables -> Actions`):
- `KEYSTORE_BASE64`: The Base64 representation of your keystore
- `KEYSTORE_PASSWORD`: `ytplayer123`
- `KEY_ALIAS`: `ytplayer`
- `KEY_PASSWORD`: `ytplayer123`
