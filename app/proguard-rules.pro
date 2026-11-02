# kotlinx.serialization ships its own rules; keep our backup/AI models explicitly for safety.
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-keep,includedescriptorclasses class com.aio.tracker.**$$serializer { *; }
-keepclassmembers class com.aio.tracker.** {
    *** Companion;
}
-keepclasseswithmembers class com.aio.tracker.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep @kotlinx.serialization.Serializable class com.aio.tracker.** { *; }
