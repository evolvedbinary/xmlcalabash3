<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-inline-prefixes="cx xs" version="3.0">

<p:import href="https://xmlcalabash.com/ext/library/elemental.xpl"/>

  <p:output port="result"/>

  <cx:elemental parameters="map {
                  'cx:database-uri': 'http://elemental:8080/exist/rest/db/',
                  'cx:request-timeout': 2000,
                  'cx:response-timeout': 1000,
                  'name': 'greeting' }">

    <p:with-input port="source"><p:empty/></p:with-input>

    <p:with-input port="query" href="simple-external-variable-binding.xq"/>

  </cx:elemental>

</p:declare-step>
