# Reglas específicas para OrbBlaze

# REGLA DE ORO PARA EVITAR IncompatibleClassChangeError en Compose (Android 10-13)
-optimizations !method/marking/static
-optimizations !method/propagation/argument
-optimizations !method/propagation/returnvalue
-optimizations !class/merging/*
-optimizations !interface/merge

# Mantener todas las interfaces de Compose intactas
-keep interface androidx.compose.** { *; }
-keep class androidx.compose.ui.modifier.** { *; }
-keep interface androidx.compose.ui.modifier.ModifierLocalProvider { *; }
-keep class androidx.compose.ui.modifier.ModifierLocalProvider** { *; }
-keep interface androidx.compose.runtime.** { *; }
-keep class androidx.compose.runtime.** { *; }

# GOOGLE AUTH / SIGN-IN: Reglas reforzadas para evitar Error 10 en Release
-keep class com.google.android.gms.auth.api.signin.** { *; }
-keep class com.google.android.gms.auth.api.signin.internal.** { *; }
-keep class com.google.android.gms.common.api.ApiException { *; }
-keep class com.google.android.gms.common.api.Scope { *; }
-keep class com.google.android.gms.common.api.Status { *; }
-keepclassmembers class * extends com.google.android.gms.common.api.ApiException {
    public <init>(...);
}

# HILT: Inyección de dependencias
-keep class com.google.dagger.** { *; }
-keep class dagger.** { *; }
-keep public class * extends android.app.Application
-keep public class * extends android.app.Activity
-keep public class * extends androidx.fragment.app.Fragment
-keep public class * extends androidx.viewmodel.ViewModel
-keep class * extends androidx.lifecycle.ViewModel
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# FIREBASE: Auth y Firestore
-keep class com.google.firebase.** { *; }
-keep @com.google.firebase.firestore.IgnoreExtraProperties class * { *; }
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
}

# ADMOB: Google Play Services Ads
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }

# MODELOS DE DATOS: Importante para Firestore
-keepclassmembers class com.example.orbblaze.domain.model.** { *; }
-keep class com.example.orbblaze.domain.model.** { *; }

# NAVEGACIÓN:
-keep class androidx.navigation.** { *; }

# WORKMANAGER: Crash Fix
-keep class androidx.work.impl.WorkDatabase_Impl { *; }
-keep class androidx.work.impl.background.systemalarm.RescheduleReceiver { *; }
-keep class androidx.work.impl.background.systemalarm.ConstraintProxy$BatteryChargingProxy { *; }
-keep class androidx.work.impl.background.systemalarm.ConstraintProxy$BatteryNotLowProxy { *; }
-keep class androidx.work.impl.background.systemalarm.ConstraintProxy$StorageNotLowProxy { *; }
-keep class androidx.work.impl.background.systemalarm.ConstraintProxy$NetworkStateProxy { *; }
-keep class androidx.work.impl.background.systemjob.SystemJobService { *; }
-keep class androidx.work.impl.foreground.SystemForegroundService { *; }
-keep class androidx.work.impl.diagnostics.DiagnosticsReceiver { *; }

# REFUERZO PARA GOOGLE PLAY SERVICES (Vital para comunicación GMS)
-keep class com.google.android.gms.common.** { *; }
-keep class com.google.android.gms.internal.** { *; }
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.tasks.** { *; }
-keep interface com.google.android.gms.** { *; }
-keep class com.google.android.gms.common.api.internal.LifecycleCallback { *; }
-keep public class com.google.android.gms.common.internal.ReflectedParcelable
-keep public interface com.google.android.gms.common.internal.safeparcel.SafeParcelable {
    public static final android.os.Parcelable$Creator *;
}

# Mantener Parcelable
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# ROOM:
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase

# APP STARTUP:
-keep class * extends androidx.startup.Initializer

# ATRIBUTOS:
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,Exceptions,AnnotationDefault
