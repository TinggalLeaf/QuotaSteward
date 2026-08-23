# =============================================================================
# ProGuard / R8 rules for QuotaSteward release builds.
# R8 is enabled in release { isMinifyEnabled = true }.
# =============================================================================

# Keep file/line info for stack traces; keep annotations for reflection.
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature, Exceptions
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile

# -----------------------------------------------------------------------------
# Kotlin runtime + reflection
# -----------------------------------------------------------------------------
-keep class kotlin.Metadata { *; }
-keep class kotlin.coroutines.Continuation { *; }
-keep class kotlin.reflect.** { *; }
-dontwarn kotlin.reflect.jvm.internal.**

# kotlinx.serialization — KEEP the @Serializable companion serializer
# classes and the synthesized $serializer methods, otherwise JSON encoding
# will fail at runtime with SerializationException.
-keepattributes RuntimeVisibleAnnotations, AnnotationDefault, RuntimeVisibleParameterAnnotations
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    static **$Companion Companion;
    static <1>$Companion Companion;
    public static **$$serializer INSTANCE;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class **$$serializer { *; }
# Keep the @Serializable annotation itself so reflection still finds it
-keep @kotlinx.serialization.Serializable class * { *; }

# -----------------------------------------------------------------------------
# Compose — keep runtime + composition machinery
# Compose uses heavy reflection / generated lambdas; R8 default rules cover
# most of it but we add a few belt-and-braces entries.
# -----------------------------------------------------------------------------
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.ui.tooling.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.animation.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.material.icons.** { *; }
-keep class androidx.compose.runtime.snapshots.** { *; }
-keep class androidx.compose.runtime.saveable.** { *; }
-dontwarn androidx.compose.**

# Keep all @Composable functions — they are invoked via generated lambdas
# whose names are constructed at compose-time.
-keep,allowobfuscation @androidx.compose.runtime.Composable class *

# -----------------------------------------------------------------------------
# Miuix — keep its classes; the library uses string-based class lookups in
# places (theme system, dialog host). Safer to keep it whole.
# -----------------------------------------------------------------------------
-keep class top.yukonga.miuix.kmp.** { *; }
-keep class io.github.kyant0.** { *; }   # G2 continuity shapes (transitive)
-dontwarn top.yukonga.miuix.kmp.**

# -----------------------------------------------------------------------------
# App data classes — keep with their declared fields so kotlinx-serialization
# can read/write them.
# -----------------------------------------------------------------------------
-keepclassmembers class com.github.tinggalleaf.ai_quota_dashboard.data.model.** {
    <init>(...);
    <fields>;
}
-keep class com.github.tinggalleaf.ai_quota_dashboard.data.model.** { *; }

# Keep our top-level composables (referenced by name in some places)
-keep class com.github.tinggalleaf.ai_quota_dashboard.ui.** { *; }

# -----------------------------------------------------------------------------
# OkHttp / Okio / Coil
# -----------------------------------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn coil.**

# AndroidX core/lifecycle/activity/navigation/dataStore
-dontwarn androidx.lifecycle.**
-dontwarn androidx.navigation.**
-dontwarn androidx.datastore.**

# -----------------------------------------------------------------------------
# Optimization: keep public API surface tight but don't repackage — Compose
# tooling and Kotlin reflection rely on stable class names.
# -----------------------------------------------------------------------------
-optimizationpasses 5
-mergeinterfacesaggressively
# DO NOT use -allowaccessmodification or -repackageclasses '' with Compose +
# Miuix: the libraries do private-access reflection that breaks when members
# are renamed or moved.
