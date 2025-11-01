import com.xmlcalabash.build.ExternalDependencies

plugins {
    id("buildlogic.kotlin-common-conventions")
}

dependencies {
  ExternalDependencies.of(ExternalDependencies.implSteps).forEach {
    implementation(it) {
      exclude(group="net.sf.saxon", module="Saxon-HE")
    }
  }

  ExternalDependencies.of(ExternalDependencies.compileSteps).forEach {
    compileOnly(it) {
      exclude(group="net.sf.saxon", module="Saxon-HE")
    }
  }
}
