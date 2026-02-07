# Reglas específicas para OrbBlaze

# AdMob: Evitar que R8 elimine clases necesarias de Google Play Services
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }

# Compose: Reglas generales para que Compose funcione correctamente tras la ofuscación
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# DataStore y Serialización (si usas modelos que se guardan)
-keepclassmembers class com.example.orbblaze.domain.model.** { *; }

# Mantener atributos necesarios para debugging de errores (opcional pero recomendado)
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod

# Si usas bibliotecas que usan reflexión (Gson, Moshi, etc), se añadirían aquí.
