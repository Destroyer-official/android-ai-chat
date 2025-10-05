# AI Assistant - Production ProGuard Rules

# Keep application classes
-keep class com.example.androidimageapp.** { *; }

# Keep API models and data classes
-keep class com.example.androidimageapp.** implements java.io.Serializable { *; }
-keep class com.example.androidimageapp.**$** { *; }

# Keep fragment constructors
-keepclassmembers class * extends androidx.fragment.app.Fragment {
    public <init>(...);
}

# Keep activity constructors  
-keepclassmembers class * extends androidx.appcompat.app.AppCompatActivity {
    public <init>(...);
}

# Retrofit & OkHttp
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Data classes
-keep class com.example.androidimageapp.fragments.MessageItem { *; }
-keep class com.example.androidimageapp.MainActivity$CustomModel { *; }
-keep class com.example.androidimageapp.ChatSession { *; }

# Markwon
-keep class io.noties.markwon.** { *; }
-dontwarn io.noties.markwon.**

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}
