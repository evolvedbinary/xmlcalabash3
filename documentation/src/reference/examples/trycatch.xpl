<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:xs="http://www.w3.org/2001/XMLSchema" 
                xmlns:ex="http://example.com/ns"
                name="main" version="3.1">
  <p:import href="choose.xpl"/>

  <p:input port="source"/>
  <p:output port="result"/>

  <p:try>
    <ex:format/>
    <p:catch code="ex:bad-status">
      <p:xslt>
        <p:with-input port="source" pipe="source@main"/>
        <p:with-input port="stylesheet" href="draft.xsl"/>
      </p:xslt>
    </p:catch>
  </p:try>
</p:declare-step>
