plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

kotlin {
    // Nouvelle DSL requise par Kotlin 2.3.0 (kotlinOptions{jvmTarget=...} est
    // désormais une erreur de compilation, pas juste un warning de dépréciation).
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

android {
    namespace = "dj.medscriberural.standalone"
    compileSdk = 34

    defaultConfig {
        applicationId = "dj.medscriberural.standalone"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        // litertlm-android (0.10.0+) est compilé avec Java 21 (class file 65.0) —
        // Java 17 provoque une erreur "bad class file" à la compilation.
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        viewBinding = true
    }

    packaging {
        // Évite les conflits de licences/méta-fichiers entre libs natives
        resources.excludes.add("META-INF/*")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-ktx:1.9.1")
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Moteur d'inférence on-device — remplace la dépendance à Gallery.
    implementation("com.google.ai.edge.litertlm:litertlm-android:latest.release")

    // Stockage local des fiches, identique au pipeline SQLite existant.
    // Room 2.8.4 (2.6.1 déclenche un bug connu de KSP2 "unexpected jvm
    // signature V" sur les fonctions DAO suspend qui retournent Unit).
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    implementation("org.json:json:20240303")
}
