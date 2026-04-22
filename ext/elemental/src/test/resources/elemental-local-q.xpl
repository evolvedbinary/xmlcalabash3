<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:other="http://other"
                exclude-inline-prefixes="cx" version="3.0">

<p:import href="https://xmlcalabash.com/ext/library/elemental.xpl"/>

  <p:output port="result" sequence="true"/>

  <p:variable name="attr" select="/elem/@other:x">
    <p:inline><elem other:x="y"/></p:inline>
  </p:variable>

  <p:xquery cx:processor="https://elemental.xyz/"
            parameters="map {
                  'name': 'greeting',
                  'attr': $attr,
                  'map': map { 'x': 'y' },
                  'array': ['x', 'y'] }">

    <p:with-input port="source">
      <p:inline>
        <elem x="y">
          <subElem>some text</subElem>
        </elem>
      </p:inline>
    </p:with-input>

    <p:with-input port="query" href="simple-external-variable-binding.xq"/>

  </p:xquery>

</p:declare-step>
