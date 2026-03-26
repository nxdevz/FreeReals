# Optimasi umum
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-dontpreverify
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# ==================== KOMPRESI & OBFUSCATION ====================
# Aktifkan obfuscation dan compression
-obfuscationdictionary proguard-dictionary.txt
-classobfuscationdictionary proguard-dictionary.txt
-packageobfuscationdictionary proguard-dictionary.txt

# Hapus atribut yang tidak perlu
-dontwarn com.google.errorprone.annotations**
-dontwarn com.google.auto.value.**

# Hapus log untuk release build
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    public static *** wtf(...);
}

-assumenosideeffects class kotlin.io.ConsoleKt {
    public static *** println(...);
    public static *** print(...);
}

# ==================== KEEP KOTLIN ====================
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public *;
}

# ==================== KEEP COMPOSE ====================
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.animation.** { *; }

# Keep Composable functions
-keep @androidx.compose.runtime.Composable public class * { *; }
-keep @androidx.compose.runtime.Composable public interface * { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# Keep Preview for development (opsional, bisa dihapus jika tidak butuh preview di release)
-keep @androidx.compose.ui.tooling.preview.Preview public class * { *; }
-keepclassmembers class * {
    @androidx.compose.ui.tooling.preview.Preview <methods>;
}

# ==================== KEEP ACTIVITY & VIEWMODEL ====================
-keep public class * extends android.app.Activity
-keep public class * extends androidx.lifecycle.ViewModel
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# Keep ViewModel constructors
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# ==================== KEEP ROOM ====================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity public class * { *; }
-keep @androidx.room.Dao public interface * { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

-keepclassmembers class ** {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ==================== KEEP RETROFIT & GSON ====================
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**

-keep class com.google.gson.** { *; }
-keep class com.example.myapplication.data.model.** { *; }
-keep class com.example.myapplication.data.network.** { *; }

# Keep Retrofit interfaces
-keep interface com.example.myapplication.data.network.MeloloApi { *; }

# ==================== KEEP MEDIA3 ====================
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
-keep class com.google.android.exoplayer2.** { *; }
-dontwarn com.google.android.exoplayer2.**

# ==================== KEEP COIL ====================
-keep class coil.** { *; }
-dontwarn coil.**

# ==================== KEEP COROUTINES ====================
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keepclassmembers class kotlinx.coroutines.internal.MainDispatcherFactory {
    <init>(...);
}
-keepclassmembers class kotlinx.coroutines.CoroutineExceptionHandler {
    <init>(...);
}
-keepclassmembers class kotlinx.coroutines.android.AndroidExceptionPreHandler {
    <init>(...);
}
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler

# ==================== REMOVE UNUSED RESOURCES ====================
# Hapus resource yang tidak digunakan
-dontwarn com.android.resources.Resource**
-assumenosideeffects class android.content.res.Resources {
    public *** getString(...);
    public *** getQuantityString(...);
    public *** getText(...);
    public *** getDrawable(...);
    public *** getColor(...);
}

# ==================== OPTIMIZE STRINGS ====================
# Gabungkan string yang sama
-optimizations !code/allocation/variable
-mergeinterfacesaggressively
-allowaccessmodification
-repackageclasses ''