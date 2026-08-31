# ProGuard / R8 rules for YT Player

# --- NewPipe Extractor ---
# NewPipe Extractor uses Mozilla Rhino for JavaScript evaluation
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.** { *; }

# Keep NewPipe Extractor classes
-keep class org.schabi.newpipe.extractor.** { *; }
-dontwarn org.schabi.newpipe.extractor.**

# --- OkHttp ---
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# --- Media3 / ExoPlayer ---
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# --- Hilt ---
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# --- WebView JavaScript Interface ---
-keepclassmembers class com.tonmoy.ytplayer.webview.JavaScriptBridge {
    @android.webkit.JavascriptInterface <methods>;
}

# --- Kotlin Coroutines ---
-dontwarn kotlinx.coroutines.**

# --- General ---
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
