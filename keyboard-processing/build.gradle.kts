plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.vernacularguardian.keyboardprocessing"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // Required for java.time.LocalDate/ZoneId (Sprint 4's sessionEpochDay
        // calculation) to work on API 24-25, which lack java.time natively.
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.coroutines.core)
    // Pinned to the exact version VGA's :app already uses (frozen dependency;
    // this module only needs long-stable WorkManager 2.x APIs - CoroutineWorker,
    // PeriodicWorkRequestBuilder, Constraints, enqueueUniquePeriodicWork,
    // ExistingPeriodicWorkPolicy.KEEP, getWorkInfosForUniqueWork - none of which
    // require 2.11.2), rather than introducing a second WorkManager version.
    implementation("androidx.work:work-runtime-ktx:2.10.1")
    // NotificationCompat/NotificationManagerCompat for the Sprint 5 owner-confirmation prompt.
    implementation(libs.androidx.core.ktx)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.work:work-testing:2.10.1")
}
