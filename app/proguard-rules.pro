# ProGuard / R8 rules for «Хроники Бабочки»

# Preserve Firestore data models, enums, and @Keep-annotated members for reflection deserialization
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}
-keep class com.example.data.model.** { *; }

# Preserve Kotlin metadata & attributes needed by Firestore and Compose
-keepattributes Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations,AnnotationDefault
-keep class kotlin.Metadata { *; }

# Keep Firebase / Firestore / Credential Manager generated & reflective entry points
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
-keep class com.google.firebase.appcheck.** { *; }
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
