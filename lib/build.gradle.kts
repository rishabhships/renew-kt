plugins {
    kotlin("jvm")
}

group = "com.rishabhships"
version = "0.2.0-SNAPSHOT"

kotlin {
    jvmToolchain(17)
    explicitApi()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
