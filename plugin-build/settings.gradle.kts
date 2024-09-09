rootProject.name = ("org.skriptlang.gradle.test.plugin")

dependencyResolutionManagement {

    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }

}
