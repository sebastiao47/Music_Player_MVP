plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.playerseba"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.playerseba"
        minSdk = 24
        targetSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation("androidx.core:core:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.activity:activity:1.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.9.4")

    implementation("androidx.room:room-runtime:2.8.2")
    annotationProcessor("androidx.room:room-compiler:2.8.2")

    implementation("androidx.media:media:1.7.1")

    implementation(files("libs/acrcloud-universal-sdk-1.3.30.jar"))

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
