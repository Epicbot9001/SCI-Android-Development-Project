plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 21
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.navigation:navigation-fragment:2.5.3")
    implementation("androidx.navigation:navigation-ui:2.5.3")
    implementation(files("libs\\jts-core-1.19.0.jar"))
    implementation(files("libs\\jts-io-common-1.19.0.jar"))
    implementation(files("libs\\json-simple-1.1.1.jar"))
    implementation(files("libs\\java-rt-jar-stubs-1.5.0.jar"))
    implementation(files("libs\\proj4j-1.3.0.jar"))
    implementation(files("libs\\gt-main-30.0.jar"))
    implementation(files("libs\\gt-shapefile-30.0.jar"))
    implementation(files("libs\\gt-swing-30.0.jar"))
    implementation(files("libs\\gt-api-30.0.jar"))
    implementation(files("libs\\gt-referencing-30.0.jar"))
    implementation(files("libs\\gt-metadata-30.0.jar"))
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}