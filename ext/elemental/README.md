#  Elemental extension step

This is an extension step for [XML Calabash 3.x](https://github.com/xmlcalabash/xmlcalabash3), and it requires Elemental version 7.7.0 or newer.

To use this step, you must add it to your XML Calabash configuration. It doesn’t
have any standalone functionality.

NOTE: This step can utilize Elemental as a library. Elemental 7.x.x requires
Java 21, therefore if you wish to use this extension and connect to Elemental
operating in local mode (i.e. embedded) you must run XML Calabash on Java 21 or higher.
If you are instead connecting to a remote Elemental server (remote mode) then
XML Calabash only requires Java 11+ to operate.

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

# Globally Configure as your default XQuery implementation

One approach is that at a global level you can configure XML Calabash so that it will use Elemental as the default
implementation of the `p:xquery` step. You will need a configuration that is
similar to the following:

```xml
<cc:xml-calabash xmlns:cc="https://xmlcalabash.com/ns/configuration" version="1.0"
    default-xquery-processor="https://elemental.xyz/"/>

...
```

See the [Configuration chapter](https://docs.xmlcalabash.com/userguide/current/configuration.html) of the User Guide for more details.

# Locally Configure as your XQuery implementation

Another approach is that at a per-query step label you can configure XML Calabash so that it will use Elemental as the
implementation of the `p:xquery` step.

XQuery may be executed via Elemental in one of two modes:

1. Remote Mode - whereby you have a separate Elemental Server running and listening for connections.
2. Local Mode - whereby a copy of Elemental is embedded within the same JVM as XML Calabash and they communicate directly.

Your step configuration will need to be similar to one of the following:

## Remote Mode

```xml
...

<p:xquery cx:processor="https://elemental.xyz/"
          parameters="map {
                  'cx:database-uri': 'http://elemental:8080/exist/rest/db/',
                  'cx:request-timeout': 2000,
                  'cx:response-timeout': 1000,
                  'my-external-variable-1': 'value-1',
                  'my-external-variable-2': 'value-2' }">

...
```

## Local Mode

```xml
...

<p:xquery cx:processor="https://elemental.xyz/"
          parameters="map {
                  'my-external-variable-1': 'value-1',
                  'my-external-variable-2': 'value-2' }">

...
```
