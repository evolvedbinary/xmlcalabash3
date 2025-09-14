<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:ex="http://example.com/ns"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-result-prefixes="ex xs"
                expand-text="yes"
                version="3.0">

<xsl:output method="xml" encoding="utf-8" indent="no"/>

<xsl:mode on-no-match="shallow-copy"/>

<xsl:template match="hash">
  <p>The hash of “{string(.)}” is:</p>
  <xsl:copy>
    <xsl:apply-templates select="@*"/>
    <xsl:sequence select="ex:hash(map{'text': string(.)})?result"/>
  </xsl:copy>
</xsl:template>

</xsl:stylesheet>
