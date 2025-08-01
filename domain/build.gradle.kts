plugins {
    id("java-library")
    alias(libs.plugins.jetbrains.kotlin.jvm)
}
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}

dependencies {
    implementation("javax.inject:javax.inject:1")
    implementation(libs.androidx.annotation.jvm)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}
