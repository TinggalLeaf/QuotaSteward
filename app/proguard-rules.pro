# ProGuard rules for QuotaSteward release builds.
# Kept conservative because Miuix + Compose reflection paths can be fragile.

-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature, Exceptions
-keepattributes SourceFile, LineNumberTable

# Compose / Kotlin reflection
-keep class kotlin.Metadata { *; }
-keep class kotlin.coroutines.Continuation { *; }

# kotlinx-serialization — keep @Serializable companions
-keepclassmembers class **$$serializer { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <methods>;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp / Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**

# Miuix / Compose runtime
-keep class top.yukonga.miuix.kmp.** { *; }

# App data classes
-keep class com.github.tinggalleaf.ai_quota_dashboard.data.model.** { *; }
