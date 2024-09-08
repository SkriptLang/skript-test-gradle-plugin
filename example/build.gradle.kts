plugins {
    java
    id("org.skriptlang.gradle.test.plugin")
}

tasks.skriptTest {
    extraPluginsDirectory = File("build.gradle.kts")
    testScriptDirectory = File("build.gradle.kts")
}
