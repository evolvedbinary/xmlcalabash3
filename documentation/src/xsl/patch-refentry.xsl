<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:db="http://docbook.org/ns/docbook"
                exclude-result-prefixes="xs"
                expand-text="yes"
                version="3.0">

<xsl:output method="xml" encoding="utf-8" indent="no"/>

<xsl:mode on-no-match="shallow-copy"/>

<xsl:template match="db:refentry">
  <xsl:variable name="refname" select="string(db:refnamediv/db:refname)"/>
  <xsl:choose>
    <xsl:when test="starts-with($refname, 'p:')">
      <xsl:copy>
        <xsl:apply-templates select="@*, node()"/>
        <refsection xmlns="http://docbook.org/ns/docbook">
          <info>
            <title>Additional examples</title>
          </info>
          <para>
            <xsl:text>The XProc test suite </xsl:text>
            <link xmlns:xlink="http://www.w3.org/1999/xlink"
                  xlink:href="https://test-suite.xproc.org/element.html#p_{substring-after($refname, ':')}"
                  >contains examples</link>
            <xsl:text> of the </xsl:text>
            <tag>{$refname}</tag>
            <xsl:text> step.</xsl:text>
          </para>
        </refsection>
      </xsl:copy>
    </xsl:when>
    <xsl:otherwise>
      <xsl:next-match/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

</xsl:stylesheet>
