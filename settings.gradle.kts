plugins {
    id("de.fayard.refreshVersions") version "0.51.0"
}

rootProject.name = "loadstone"

include(":toolbox")
include(":toolbox:asm")

include(":server")
include(":server:cache")