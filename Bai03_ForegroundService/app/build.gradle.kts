plugins {
    id("com.android.application")
    alias(libs.plugins.compose.compiler)
}
android {
    namespace = "vn.training.bai03"
    compileSdk = 36
    defaultConfig { applicationId = "vn.training.bai03"; minSdk = 31; targetSdk = 36; versionCode = 1; versionName = "1.0" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    buildFeatures { compose = true }
    testOptions { unitTests.isReturnDefaultValues = true }
}
dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    testImplementation("junit:junit:4.13.2")
}

// Keep inactive CLI template tests outside the active source sets.
android.sourceSets.getByName("test").java.setSrcDirs(listOf("src/testSolution/java"))
android.sourceSets.getByName("androidTest").java.setSrcDirs(listOf("src/androidTestSolution/java"))
