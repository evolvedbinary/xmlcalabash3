<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:g="http://nwalsh.com/ns/git-repo-info"
                xmlns="http://docbook.org/ns/docbook"
                exclude-result-prefixes="#all"
                expand-text="yes"
                version="3.0">

<xsl:output method="xml" encoding="utf-8" indent="no"/>

<xsl:param name="version" as="xs:string"/>

<xsl:template match="g:git-repo-info">
  <xsl:variable name="last-release" select="g:commit[contains(g:message, 'XML Calabash 3.')]"/>
  <revhistory>
  <xsl:text>&#10;</xsl:text>
  <revision xml:id="r{replace($version, '\.', '')}">
  <xsl:text>&#10;</xsl:text>
  <revnumber>{$version}</revnumber>
  <xsl:text>&#10;</xsl:text>
  <date>{substring(string(current-date()), 1, 10)}</date>
  <xsl:text>&#10;</xsl:text>
  <revdescription>
  <xsl:text>&#10;</xsl:text>
  <itemizedlist>
    <xsl:text>&#10;</xsl:text>
    <xsl:for-each select="g:commit[. &lt;&lt; $last-release[1]
                                   and not(starts-with(g:message, 'Merge pull request '))]">
      <listitem>
      <para>
        <xsl:if test="contains(g:message, 'Fix #') or contains(g:message, 'Close #')">
          <xsl:variable name="num" select="replace(g:message, '^.*#([0-9]+).*$', '$1', 's')"/>
          <xsl:text>Closed </xsl:text>
          <cbissue number="{$num}"/>.
        </xsl:if>
        <xsl:sequence select="string(g:message)"/>
      <xsl:text>&#10;</xsl:text>
      </para>
      </listitem>
      <xsl:text>&#10;</xsl:text>
    </xsl:for-each>
  </itemizedlist>
  <xsl:text>&#10;</xsl:text>
  </revdescription>
  <xsl:text>&#10;</xsl:text>
  </revision>
  <xsl:text>&#10;</xsl:text>
  </revhistory>
</xsl:template>

</xsl:stylesheet>
