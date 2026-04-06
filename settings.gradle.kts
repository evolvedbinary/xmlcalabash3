plugins {
    // Apply the foojay-resolver plugin to allow automatic download of JDKs
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "xmlcalabash"

include("xmlcalabash")
include("app")
include("test-driver")
include("documentation")
include("template:java")
include("template:kotlin")
include("ext:polyglot")
include("ext:basex")
include("ext:existdb")
include("ext:elemental")

