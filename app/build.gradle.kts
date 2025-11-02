import com.xmlcalabash.build.ExternalDependencies
import com.xmlcalabash.build.XmlCalabashBuildExtension
import org.jetbrains.dokka.DokkaConfiguration.Visibility
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.DokkaTaskPartial
import java.net.URI
import java.net.URL

buildscript {
  dependencies {
    classpath("org.jetbrains.dokka:dokka-base:1.9.20")
  }
}

plugins {
  id("buildlogic.kotlin-application-conventions")
  id("com.xmlcalabash.build.xmlcalabash-build")
  id("org.jetbrains.dokka") version "1.9.20"
  id("maven-publish")
  application
}

val xmlbuild = the<XmlCalabashBuildExtension>()

val dep_activation = project.findProperty("activation").toString()
val dep_drewnoakesExtractor = project.findProperty("drewnoakesExtractor").toString()
val dep_jaxbapi = project.findProperty("jaxbapi").toString()
val dep_nineml = project.findProperty("nineml").toString()
val dep_pdfbox = project.findProperty("pdfbox").toString()
val dep_slf4j = project.findProperty("slf4j").toString()

val xmlcalabashRelease by configurations.dependencyScope("xmlcalabashRelease")

val stageJars by configurations.creating {
  extendsFrom(configurations["distributionClasspath"])
}

dependencies {
  xmlcalabashRelease(project(mapOf("path" to ":xmlcalabash",
                                   "configuration" to "releaseArtifacts")))

  implementation(project(":xmlcalabash"))
  //implementation(project(":ext:existdb")) // No, it requires eXist-db
  //implementation(project(":ext:basex")) // No, it requires BaseX
  //implementation(project(":ext:polyglot")) // No, it requires Java 17

  ExternalDependencies.of(ExternalDependencies.distributionSteps).forEach {
    stageJars(it) {
      exclude(group="net.sf.saxon", module="Saxon-HE")
    }
  }
}

val xmlcalabashJar = configurations.resolvable("xmlcalabashJar") {
  extendsFrom(xmlcalabashRelease)
}

application {
  // Define the main class for the application.
  mainClass = "com.xmlcalabash.app.Main"
}

tasks.withType<DokkaTaskPartial>().configureEach {
  pluginConfiguration<DokkaBase, DokkaBaseConfiguration> {
    customStyleSheets = listOf(file("../documentation/src/dokka/resources/css/xmlcalabash.css"))
    //templatesDir = file("../documentation/src/dokka/resources/templates")
    footerMessage = "© 2024-2025 Norm Tovey-Walsh"
    separateInheritedMembers = false
    mergeImplicitExpectActualDeclarations = false
  }

  dokkaSourceSets {
    named("main") {
      documentedVisibilities.set(setOf(Visibility.PUBLIC))
      moduleName.set("XML Calabash Application")
      includes.from("Module.md")
      sourceLink {
        localDirectory.set(file("src/main/kotlin"))
        remoteUrl.set(URI("https://github.com/xmlcalabash/xmlcalabash3").toURL())
        remoteLineSuffix.set("#L")
      }
    }
  }
}

fun distClasspath(): List<File> {
  val libdir = "${layout.projectDirectory.dir("../xmlcalabash/lib")}/"
  val libs = mutableListOf<File>()
  configurations["stageJars"].forEach {
    // Test is !isDirectory rather than isFile() because
    // the xmlcalabash.jar file may not exist yet...but it will!
    if (!it.startsWith(libdir) && !it.isDirectory() && !it.getName().startsWith("Saxon-EE")) {
      libs.add(it)
    }
  }
  return libs
}

tasks.jar {
  val libs = mutableListOf<String>()
  for (jar in distClasspath()) {
    if (jar.getName().startsWith("Saxon-HE")) {
      libs.add("lib/Saxon-EE-${project.findProperty("saxonVersion")}.jar")
      libs.add("lib/Saxon-PE-${project.findProperty("saxonVersion")}.jar")
    }
    libs.add("lib/${jar.getName()}")
  }

  archiveFileName.set("xmlcalabash-${xmlbuild.jarArchiveFilename()}")
  manifest {
    attributes("Project-Name" to "XML Calabash",
               "Main-Class" to "com.xmlcalabash.app.Main",
               "Class-Path" to libs.joinToString(" "))
  }
}

val copyScripts = tasks.register<Copy>("copyScripts") {
  inputs.file(layout.projectDirectory.file("src/main/scripts/xmlcalabash.sh"))
  inputs.file(layout.projectDirectory.file("src/main/scripts/xmlcalabash.ps1"))
  outputs.file(layout.buildDirectory.file("stage/xmlcalabash.sh"))
  outputs.file(layout.buildDirectory.file("stage/xmlcalabash.ps1"))

  doFirst {
    // Never let different versions get co-staged
    delete(layout.buildDirectory.dir("stage"))
  }

  from(layout.projectDirectory.dir("src/main/scripts"))
  into(layout.buildDirectory.dir("stage"))
  include("xmlcalabash.sh")
  include("xmlcalabash.ps1")
  filter { line ->
    line.replace("@@VERSION@@", xmlbuild.version.get())
  }
}

val copyLib = tasks.register<Copy>("copyLib") {
  dependsOn(copyScripts)
  inputs.dir(layout.projectDirectory.file("src/main/lib"))
  from(layout.projectDirectory.dir("src/main/lib"))
  into(layout.buildDirectory.dir("stage/lib"))
}

tasks.register("stage-release") {
  inputs.files(xmlcalabashJar)
  inputs.files(copyScripts)
  inputs.files(copyLib)
  dependsOn("jar")

  doLast {
    mkdir(layout.buildDirectory.dir("stage"))
    mkdir(layout.buildDirectory.dir("stage/lib"))
    copy {
      from(layout.buildDirectory.dir("libs"))
      into(layout.buildDirectory.dir("stage"))
    }
  }

  doLast {
    distClasspath().forEach { path ->
      copy {
        from(path)
        into(layout.buildDirectory.dir("stage/lib"))
        exclude("META-INF/**")
        exclude("com/**")
      }                      
    }
  }

  doLast {
    copy {
      from(layout.projectDirectory.dir("src/main/docs"))
      into(layout.buildDirectory.dir("stage"))
      filter { line ->
        line.replace("@@VERSION@@", xmlbuild.version.get())
      }
    }
  }

  doLast {
    listOf(xmlcalabashJar).forEach { jar ->
      copy {
        from(jar)
        into(layout.buildDirectory.dir("stage/lib"))
      }
    }
  }
}

tasks.register<Zip>("release") {
  dependsOn("stage-release")
  from(layout.buildDirectory.dir("stage"))
  into("xmlcalabash-${xmlbuild.version.get()}/")
  archiveFileName = "xmlcalabash-${xmlbuild.version.get()}.zip"
}

val sourcesJar by tasks.registering(Jar::class) {
  archiveClassifier = "sources"
  from(sourceSets.main.get().allSource)
}

tasks.javadoc {
  if (JavaVersion.current().isJava9Compatible) {
    (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
  }
}

val javadocJar = tasks.register<Jar>("javadocJar") {
  dependsOn("dokkaJavadoc")
  archiveClassifier = "javadoc"
  from(tasks.dokkaJavadoc)
}

publishing {
  publications {
    create<MavenPublication>("mavenApp") {
      pom {
        groupId = project.findProperty("xmlcalabashGroup").toString()
        version = project.findProperty("xmlcalabashVersion").toString()
        name = "XML Calabash application"
        packaging = "jar"
        description = "An XML Calabash application runner"
        url = "https://codeberg.org/xmlcalabash/xmlcalabash3"

        scm {
          url = "scm:git@codeberg.org:xmlcalabash/xmlcalabash3.git"
          connection = "scm:git@codeberg.org:xmlcalabash/xmlcalabash3.git"
          developerConnection = "scm:git@codeberg.org:xmlcalabash/xmlcalabash3.git"
        }

        licenses {
          license {
            name = "MIT License"
            url = "https://opensource.org/api/license/mit"
            distribution = "repo"
          }
        }

        developers {
          developer {
            id = "ndw"
            name = "Norm Tovey-Walsh"
          }
        }
      }

      from(components["java"])
      artifact(sourcesJar.get())
      artifact(javadocJar.get())
    }
  }

  repositories {
    maven {
      url = layout.buildDirectory.dir("maven-release").get().asFile.toURI()
    }
  }
}
