# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.beolddeok.alarm.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.beolddeok.alarm.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
