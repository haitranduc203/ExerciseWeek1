plugins { id("com.android.application") }
android {
    namespace = "vn.training.books.server"
    compileSdk = 36
    defaultConfig { applicationId = "vn.training.books.server"; minSdk = 31; targetSdk = 36; versionCode = 1; versionName = "1.0" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    buildFeatures { aidl = true; viewBinding = true }
    testOptions { unitTests.isReturnDefaultValues = true }
}
dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.activity:activity-ktx:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("io.coil-kt:coil:2.7.0")
    testImplementation("junit:junit:4.13.2")
}

android.sourceSets.getByName("main") { java.srcDir("../contract/java"); aidl.srcDir("../contract/aidl") }
android.sourceSets.getByName("main").kotlin.directories.add("../contract/java")
