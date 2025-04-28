plugins {
    java
    id("skript-test")
}

tasks.skriptTest {
    extraPluginsDirectory = File("build.gradle.kts")
    testScriptDirectory = File("build.gradle.kts")
}
