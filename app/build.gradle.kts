plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp") version "2.0.21-1.0.27"
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.mcldev.comprainteligente"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mcldev.comprainteligente"
        minSdk = 29
        //noinspection EditedTargetSdkVersion
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }

        debug {
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        mlModelBinding = true
    }
}



dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.compose)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    //MaterialTheme3
    implementation(libs.material3)

    //Room
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    //Navigation
    implementation(libs.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    //Tesseeract
    implementation(libs.tesseract4android.openmp)

    //Scanner utility ML
    implementation(libs.play.services.mlkit.document.scanner)

    //Koin
    // Koin for Android
    implementation(libs.koin.android)
    implementation(libs.koin.test)

    //Work Manager
    implementation(libs.androidx.work.runtime.ktx)

    //Suport for links inside the credits section
    implementation(platform(libs.androidx.compose.bom.v20250101))
    implementation(libs.androidx.compose.material3.material3)

    //Memory leak check
    //debugImplementation ("com.squareup.leakcanary:leakcanary-android:2.14")
    /*
    Tested on 3/03/25 with release flag and using R8 and with debug build without R8. Both tests circle through
    all the implemented screens at the time and waited for aobut a minute on each.
    No leaks found.
     */
}
ksp {
    arg("room.schemaLocation", "$projectDir/schemas") // Add schema export location for Room
}