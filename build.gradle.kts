plugins {
    // Declare plugins centrally; modules apply them as needed.
    kotlin("jvm") version "2.0.21" apply false
    kotlin("android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("com.android.application") version "8.5.2" apply false
    id("com.vanniktech.maven.publish") version "0.34.0" apply false
}
