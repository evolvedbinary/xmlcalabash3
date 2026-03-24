<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-inline-prefixes="cx xs" version="3.0">

<p:import href="https://xmlcalabash.com/ext/library/elemental.xpl"/>

<p:output port="result"/>

<p:xquery cx:processor="https://elemental.xyz/"
          parameters="map{'cx:database-uri': 'http://elemental:8080/exist/rest/db/'}">
  <p:with-input port="source"><p:empty/></p:with-input>
  <p:with-input port='query'>
    <p:inline content-type="text/plain">&lt;doc>{3+4}&lt;/doc></p:inline>
  </p:with-input>
</p:xquery>

</p:declare-step>
