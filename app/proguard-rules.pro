# Kotlin metadata (required for reflection-free Kotlin features)
-keep class kotlin.Metadata { *; }

# Preserve enum names and members (Gospel enum is persisted by ordinal,
# but keeping names prevents issues with Kotlin's EnumEntries API)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    **[] $VALUES;
    public *;
}

# Coroutines (debug info not needed in release)
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Keep application entry points
-keep class njs.listentogospel.ListenToGospelApp { *; }
-keep class njs.listentogospel.MainActivity { *; }
-keep class njs.listentogospel.service.PlaybackService { *; }
