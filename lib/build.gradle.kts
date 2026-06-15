plugins {
    kotlin("jvm")
    id("com.vanniktech.maven.publish")
    signing
}

group = "com.rishabhships"
version = "0.3.0"

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

signing {
    useGpgCmd()
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates("com.rishabhships", "renew-kt", "0.3.0")

    pom {
        name.set("renew-kt")
        description.set("A deterministic Kotlin state machine for Google Play Billing subscriptions.")
        inceptionYear.set("2026")
        url.set("https://github.com/rishabhships/renew-kt")

        licenses {
            license {
                name.set("The Apache Software License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }

        developers {
            developer {
                id.set("rishabhships")
                name.set("Rishabh Gupta")
                email.set("g.rishabh2010@gmail.com")
                url.set("https://rishabhships.com")
            }
        }

        scm {
            url.set("https://github.com/rishabhships/renew-kt")
            connection.set("scm:git:git://github.com/rishabhships/renew-kt.git")
            developerConnection.set("scm:git:ssh://git@github.com/rishabhships/renew-kt.git")
        }
    }
}
