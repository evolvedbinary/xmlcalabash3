<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-result-prefixes="xs"
                version="3.0">

<xsl:output method="xml" encoding="utf-8" indent="yes"/>

<xsl:param name="spec" select="'https://spec.xproc.org/3.1/xproc/'"/>

<xsl:template name="xsl:initial-template">
  <link-targets href="{$spec}">
    <xsl:for-each select="doc($spec)//*[@id]">
      <link target="{@id}" element="{local-name(.)}"/>
    </xsl:for-each>
  </link-targets>
</xsl:template>

</xsl:stylesheet>
