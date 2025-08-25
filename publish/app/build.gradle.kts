import org.gradle.api.GradleException
import org.jreleaser.gradle.plugin.JReleaserExtension
import org.jreleaser.model.Active
import org.jreleaser.model.api.deploy.maven.MavenCentralMavenDeployer

plugins {
  id("org.jreleaser") version "1.18.0"
}

repositories {
  mavenLocal()
  mavenCentral()
}

val releaseName = project.name
val releaseGroup = "com.xmlcalabash"
val releaseArtifact = "app"
val releaseDescription = "An XProc 3.0 processor"
val releaseUrl = "https://codeberg.org/xmlcalabash/xmlcalabash3"
val releaseScm = "scm:git@codeberg.org:xmlcalabash/xmlcalabash3.git"

val releaseVersion = project.findProperty("releaseVersion")?.toString()
    ?: throw GradleException("No release version provided")

configure<JReleaserExtension> {
  gitRootSearch = true
  project {
    group = releaseGroup
    version = releaseVersion
    description = releaseDescription
    authors = listOf("ndw")
    license = "MIT"
    links {
      homepage = releaseUrl
      bugTracker = "${releaseUrl}/issues"
      contact = releaseUrl
    }
    inceptionYear = "2024"
    vendor = "Norman Walsh"
    copyright = "Copyright (c) 2024-2025 Norman Walsh"
  }

  release {
    forgejo {
      username = "ndw"
      host = "codeberg.org"
      apiEndpoint = "https://codeberg.org"
      commitAuthor {
        name = "Norman Walsh"
        email = "ndw@nwalsh.com"
      }
    }
  }

  signing {
    active = Active.ALWAYS
    armored = true
    command {
      keyName = "21FDBF8F"
    }
  }

  deploy {
    maven {
      mavenCentral {
        register("release-deploy") {
          active = Active.RELEASE
          stage = MavenCentralMavenDeployer.Stage.UPLOAD
          url = "https://central.sonatype.com/api/v1/publisher"
          stagingRepositories.add("../../app/build/maven-release")
        }
      }
      nexus2 {
        register("snapshot") {
          active = Active.SNAPSHOT
          url = "https://central.sonatype.com/repository/maven-snapshots/"
          snapshotUrl = "https://central.sonatype.com/repository/maven-snapshots/"
          applyMavenCentralRules = true
          snapshotSupported = true
          closeRepository = true
          releaseRepository = false
          stagingRepositories.add("../../app/build/maven-release")
        }
      }
    }
  }
}

tasks.register("clean") {
  doLast {
    delete(layout.buildDirectory.get().asFile)
  }
}

tasks.register("helloWorld") {
  doLast {
    println("Publishing XML Calabash...")
  }
}
