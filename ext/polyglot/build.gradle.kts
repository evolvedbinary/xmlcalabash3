import java.nio.file.*
import com.xmlcalabash.build.XmlCalabashBuildExtension
import com.xmlcalabash.build.ExternalDependencies

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

import org.jetbrains.dokka.DokkaConfiguration.Visibility
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.DokkaTaskPartial
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration

buildscript {
  dependencies {
    classpath("org.jetbrains.dokka:dokka-base:1.9.20")
  }
}

plugins {
  id("buildlogic.kotlin-library-conventions")
  id("com.xmlcalabash.build.xmlcalabash-build")
  id("org.graalvm.buildtools.native") version "1.1.2"
  id("org.jetbrains.dokka") version "1.9.20"
  id("maven-publish")
}

val xmlcalabash = configurations.create("xmlcalabash") {}

configurations.forEach {
  it.exclude("net.sf.saxon.Saxon-HE")
}

var graalvm_version: String? = null
ExternalDependencies.of(listOf("polyglot")).forEach {
  if (it.contains("polyglot:polyglot:")) {
    graalvm_version = it.split(":").last()
  }
}

dependencies {
  implementation(project(":xmlcalabash"))
  ExternalDependencies.of(listOf("polyglot")).forEach {
    implementation(it)
  }
  xmlcalabash(project(":xmlcalabash"))
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

val xmlbuild = the<XmlCalabashBuildExtension>()

val isGraalVM = Files.exists(Paths.get("${System.getProperty("java.home")}/lib/graalvm"))
val jvmDefaultArgs = if (isGraalVM) {
  // None required...
  listOf<String>()
} else {
  configurations {
    create("compilerClasspath") {
      isCanBeResolved = true
    }
  }
  
  dependencies {
    "compilerClasspath"("org.graalvm.compiler:compiler:${graalvm_version}")
  }
  
  val compilerDependencies = configurations.getByName("compilerClasspath")
      .filter { it.name.endsWith(".jar") }  // Filter out POMs

  listOf("-XX:+UnlockExperimentalVMOptions", "-XX:+EnableJVMCI",
         "--upgrade-module-path=${compilerDependencies.asPath}")
}

if (isGraalVM) {
  println("Building with GraalVM Java version ${System.getProperty("java.version")}")
} else {
  println("Building with Java version ${System.getProperty("java.version")}")
}

tasks.jar {
  archiveFileName.set(xmlbuild.jarArchiveFilename())
}

val sourcesJar = tasks.register<Jar>("sourcesJar") {
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

tasks.withType<Test> {
  jvmArgs(jvmDefaultArgs)
}

tasks.register("stage-release") {
  dependsOn("jar")

  doLast {
    mkdir(layout.buildDirectory.dir("stage"))
    mkdir(layout.buildDirectory.dir("stage/extra"))
    copy {
      from(layout.buildDirectory.dir("libs"))
      into(layout.buildDirectory.dir("stage/extra"))
    }
  }

  doLast {
    xmlbuild.distClasspath(configurations["xmlcalabash"], configurations["distributionClasspath"])
        .forEach { path ->
          copy {
            from(path)
            into(layout.buildDirectory.dir("stage/extra"))
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
    copy {
      from(layout.projectDirectory)
      into(layout.buildDirectory.dir("stage"))
      include("README.md")
      filter { line ->
        line.replace("Polyglot extension step",
                     "Polyglot extension step version ${xmlbuild.version.get()}")
            .replace("<version>", xmlbuild.version.get())
      }
    }
  }
}

tasks.register<Zip>("release") {
  dependsOn("stage-release")
  from(layout.buildDirectory.dir("stage"))
  into("${xmlbuild.name.get()}-${xmlbuild.version.get()}/")
  archiveFileName = "${xmlbuild.name.get()}-${xmlbuild.version.get()}.zip"
}

publishing {
  publications {
    create<MavenPublication>("mavenPolyglot") {
      pom {
        groupId = project.findProperty("xmlcalabashGroup").toString()
        version = project.findProperty("xmlcalabashVersion").toString()
        name = "XML Calabash Polyglot Step"
        packaging = "jar"
        description = "An polyglot step for XML Calabash 3.x"
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
