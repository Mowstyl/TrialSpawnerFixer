plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version ("1.0.0")
}

gradle.extra["projectName"] = "TrialSpawnerLootFix"
rootProject.name = gradle.extra["projectName"].toString().lowercase()
