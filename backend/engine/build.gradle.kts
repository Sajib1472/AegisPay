plugins {
    java
    application
}

application {
    mainClass.set("com.aegispay.engine.concierge.ConciergeMain")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
