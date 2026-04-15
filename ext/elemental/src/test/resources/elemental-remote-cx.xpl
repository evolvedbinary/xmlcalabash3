<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:other="http://other"
                exclude-inline-prefixes="cx" version="3.0">

<p:import href="https://xmlcalabash.com/ext/library/elemental.xpl"/>

  <p:output port="result" sequence="true"/>

  <p:variable name="attr" select="/elem/@other:x">
    <p:inline><elem other:x="y"/></p:inline>
  </p:variable>

  <cx:elemental parameters="map {
                  'cx:database-uri': 'http://elemental:8080/exist/rest/db/',
                  'cx:request-timeout': 2000,
                  'cx:response-timeout': 1000,
                  'name': 'greeting',
                  'attr': $attr,
                  'map': map { 'x': 'y' },
                  'array': ['x', 'y'] }">

    <p:with-input port="source"><p:empty/></p:with-input>

    <p:with-input port="query" href="simple-external-variable-binding.xq"/>

  </cx:elemental>

</p:declare-step>
