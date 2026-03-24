#  Elemental extension step

This is an extension step for [XML Calabash 3.x](https://github.com/xmlcalabash/xmlcalabash3).

To use this step, you must add it to your XML Calabash configuration. It doesn’t
have any standalone functionality.

# Install with Maven

Alpha versions of XML Calabash and this step are published to the Maven
[SNAPSHOT](https://help.sonatype.com/en/maven-repositories.html) repository,
https://oss.sonatype.org/content/repositories/snapshots/

Add `com.xmlcalabash:xmlcalabash:<version>` and
`com.xmlcalabash:elemental:<version>` to your project.

# Install “by hand”

To install this package by hand, download the distribution
[from GitHub](https://github.com/xmlcalabash/xmlcalabash3/releases) and
unzip it somewhere. When you run XML Calabash, make sure that
all of the jar files in the `extra` directory are included on your classpath.

One way to do this is to copy all of them into the `extra` directory
in the XML Calabash release. The scripts included in the release will
then load them automatically.

# Configure as your default XQuery implementation

You can configure XML Calabash so that it will use Elemental as the default
implementation of the `p:xquery` step. You will need a configuration that is
similar to the following:

```xml
<cc:xml-calabash xmlns:cc="https://xmlcalabash.com/ns/configuration" version="1.0"
    default-xquery-processor="https://elemental.xyz/"/>
```

See the [Configuration chapter](https://docs.xmlcalabash.com/userguide/current/configuration.html) of the User Guide for more details.
