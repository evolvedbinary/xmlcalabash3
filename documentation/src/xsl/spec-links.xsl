<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-result-prefixes="xs"
                version="3.0">

<xsl:output method="xml" encoding="utf-8" indent="yes"/>

<xsl:param name="path" as="xs:string" required="true"/>

<xsl:key name="ids" match="*" use="@target"/>

<xsl:template name="xsl:initial-template">
  <link-targets>
    <xsl:for-each select="collection('file:' || $path || '?select=*.xml')/*">
      <spec href="{@href}">
        <xsl:for-each-group select="link" group-by="@target">
          <xsl:sort select="@target"/>
          <xsl:sequence select="current-group()[1]"/>
          <xsl:if test="count(current-group()) ne 1">
            <xsl:message select="current-group()[1]/@target/string() || ' duplicated in ' || ../@href"/>
          </xsl:if>
        </xsl:for-each-group>
      </spec>
    </xsl:for-each>
  </link-targets>
</xsl:template>

</xsl:stylesheet>
