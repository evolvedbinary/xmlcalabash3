<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:xs="http://www.w3.org/2001/XMLSchema" 
                xmlns:ex="http://example.com/ns"
                name="main" version="3.1" type="ex:format">
  <p:input port="source"/>
  <p:output port="result"/>

  <p:choose>
    <p:when test="/*/@status = 'draft'">
      <p:xslt>
        <p:with-input port="stylesheet" href="draft.xsl"/>
      </p:xslt>
    </p:when>
    <p:when test="/*/@status = 'final'">
      <p:with-input pipe="source"/>
      <p:xslt>
        <p:with-input port="stylesheet" href="final.xsl"/>
      </p:xslt>
    </p:when>
    <p:otherwise>
      <p:error code="ex:bad-status">
        <p:with-input>
          <p:inline content-type="text/plain">Unexpected status</p:inline>
        </p:with-input>
      </p:error>
    </p:otherwise>
  </p:choose>
</p:declare-step>
