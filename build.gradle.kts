plugins {
    id("base")
    id("org.gradle.wrapper-upgrade") version "0.11.4"
}

wrapperUpgrade {
    gradle {
        register("foojayToolchains") {
            repo.set("gradle/foojay-toolchains")
        }
    }
}