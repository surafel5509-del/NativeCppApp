# AndroidForge Studio ProGuard/R8 rules.

# ---- Gson (plugin manifests, AI payloads) ----
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.androidforge.studio.data.remote.dto.** { *; }
-keep class com.androidforge.studio.data.plugin.** { *; }
-keep class com.androidforge.studio.data.template.** { *; }
-dontwarn com.google.gson.**

# ---- Retrofit / OkHttp ----
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# ---- JGit ----
-keep class org.eclipse.jgit.** { *; }
-dontwarn org.eclipse.jgit.**
-dontwarn java.awt.**
-dontwarn javax.swing.**
-dontwarn org.slf4j.**

# ---- Coroutines ----
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# ---- Keep BuildConfig ----
-keepclassmembers class com.androidforge.studio.BuildConfig { *; }
