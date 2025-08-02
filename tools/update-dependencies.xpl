<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:x="http://example.com/steps"
                xmlns:xs="http://www.w3.org/2001/XMLSchema" 
                xmlns:map="http://www.w3.org/2005/xpath-functions/map"
                xmlns:u="http://xylarium.org/ns/xproc/steps/utils"   
                name="main" version="3.1">
<p:output port="result" sequence="true"/>
<p:input port="source">
  <p:document href="ExternalDependencies.xml"/>
</p:input>

<p:for-each>
  <p:with-input select="/dependencies/repository[@search]"/>
  <p:variable name="search" select="/*/@search/string()"/>
  <p:for-each>
    <p:with-input select="/*/group"/>
    <p:variable name="group" select="/*/@id"/>
    <p:for-each>
      <p:with-input select="/*/artifact"/>
      <p:variable name="artifact" select="/*/@id"/>
      <p:variable name="query" select="string-join(($search, $group, $artifact), '/')"/>
      <p:load message="Loading {$query}…" href="{$query}"/>
      <p:xslt>
        <p:with-input port="stylesheet" href="latest-versions.xsl"/>
        <p:with-option name="parameters"
                       select="map{'group':$group, 'artifact':$artifact}"/>
      </p:xslt>
    </p:for-each>
  </p:for-each>
</p:for-each>

<p:wrap-sequence wrapper="updated-dependencies"/>

<p:xslt>
  <p:with-input port="source" pipe="@main"/>
  <p:with-input port="stylesheet" href="update-ExternalDependencies.xsl"/>
  <p:with-option name="parameters"
                 select="map{'updated.xml': .}"/>
</p:xslt>

</p:declare-step>
