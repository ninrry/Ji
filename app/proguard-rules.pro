# WorkManager loads workers by class name after process restart.
-keep class luzzr.ji.core.payment.PaymentRecognitionWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Shizuku starts the user service by the name passed through Binder after process restart.
-keep class luzzr.ji.core.shizuku.ShizukuScreenshotUserService { *; }
-keep class luzzr.ji.core.shizuku.IShizukuScreenshotService { *; }
-keep class luzzr.ji.core.shizuku.IShizukuScreenshotService$* { *; }

# Tink's Error Prone annotations are compile-time metadata only.
-dontwarn com.google.errorprone.annotations.**
