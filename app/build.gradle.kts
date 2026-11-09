plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "me.theoria.barlinex"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "me.theoria.barlinex"
        minSdk = 26
        targetSdk = 37
        versionCode = 8
        versionName = "1.8"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // AndroidX
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)

    // Material Design
    implementation(libs.material)

    // CameraX
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)

    // Room Database
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    // ViewModel
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)

    // Barcode Library
    implementation(libs.zxing.core)

    // ML Kit Text Recognition
    implementation(libs.mlkit.text.recognition)

    // Splash Screen
    implementation(libs.core.splashscreen)

    // Unit Testing
    testImplementation(libs.junit)

    // Instrumentation Testing
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}