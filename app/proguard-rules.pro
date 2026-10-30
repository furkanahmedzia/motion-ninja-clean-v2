# Motion Ninja Clean - ProGuard Rules

# Keep native library JNI methods
-keepclassmembers class com.motionninja.util.NativeLibs {
    native <methods>;
}

# Keep Room entities and DAOs
-keep class com.motionninja.data.** { *; }
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-keep @androidx.room.Database class *

# Keep Kotlin serialization
-keep class kotlinx.serialization.** { *; }
-keep @kotlinx.serialization.Serializable class *
-keep @kotlinx.serialization.Transient class *

# Keep Compose
-keep class androidx.compose.** { *; }
-keep class org.jetbrains.compose.** { *; }

# Keep ViewModels
-keep class androidx.lifecycle.ViewModel { *; }
-keep class androidx.lifecycle.ViewModelProvider { *; }

# Keep Exported activities
-keep public class com.motionninja.MainActivity
-keep public class com.motionninja.editor.EditorActivity

# Keep native library loading
-keep class com.motionninja.MotionApp { *; }

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Optimize
-optimizationpasses 5
-allowaccessmodification
-mergeinterfacesaggressively
-overloadaggressively