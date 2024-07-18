plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "edu.gatech.ce.allgather"
    compileSdk = 34

    defaultConfig {
        applicationId = "edu.gatech.ce.allgather"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                    getDefaultProguardFile("proguard-android.txt"),
                    "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

val kotlin_version: String by rootProject.extra

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    implementation("androidx.core:core-ktx:1.3.2")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")

    // App compat and UI things
    implementation("androidx.multidex:multidex:2.0.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.preference:preference-ktx:1.1.1")
    implementation("androidx.recyclerview:recyclerview:1.1.0")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.lifecycle:lifecycle-runtime:2.2.0")
    implementation("androidx.constraintlayout:constraintlayout:1.1.3")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.material:material:1.8.0")
    implementation("com.github.leandroborgesferreira:loading-button-android:2.3.0")

    implementation("com.google.android.gms:play-services:12.0.1")

    // CameraX
    val camerax_version = "1.0.0-beta05"
    implementation("androidx.camera:camera-core:$camerax_version")
    implementation("androidx.camera:camera-camera2:$camerax_version")
    implementation("androidx.camera:camera-lifecycle:$camerax_version")
    implementation("androidx.camera:camera-view:1.0.0-alpha12")
    implementation("androidx.camera:camera-extensions:1.0.0-alpha12")

    // Navigation library
    val nav_version = "2.5.3"
    implementation("androidx.navigation:navigation-fragment:$nav_version")
    implementation("androidx.navigation:navigation-ui:$nav_version")

    // EasyPermissions
    implementation("pub.devrel:easypermissions:3.0.0")

    // Rxjava2
    implementation("io.reactivex.rxjava2:rxandroid:2.0.1")
    implementation("io.reactivex.rxjava2:rxjava:2.1.5")

    // API Library
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Zip functionality
    implementation("org.apache.commons:commons-compress:1.26.0")
    implementation("org.apache.commons:commons-csv:1.8")

    // Math Library
    implementation("org.ejml:ejml-simple:0.41")
    implementation("org.apache.commons:commons-math3:3.6.1")

    // Test Related Libraries
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1")
    testImplementation("org.mockito:mockito-core:3.3.3")
    testImplementation("org.mockito:mockito-android:3.3.3")
    testImplementation("org.powermock:powermock-api-mockito2:2.0.9")
    testImplementation("org.powermock:powermock-module-junit4:2.0.9")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.1")

    androidTestImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("org.mockito:mockito-core:3.3.3")
    androidTestImplementation("org.mockito:mockito-android:3.3.3")
}