plugins {
    java
    id("com.diffplug.spotless") version "6.25.0" apply false
}

allprojects {
    group = "com.aegispay"
    version = "0.1.0"
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "com.diffplug.spotless")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    repositories {
        mavenCentral()
    }

    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        java {
            target("src/**/*.java")
            googleJavaFormat()
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
