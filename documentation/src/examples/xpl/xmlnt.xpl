<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                version="3.0">
<p:import href="https://xmlcalabash.com/ext/library/merge-properties.xpl"/>
<p:output port="result" sequence="true"/>

<p:load name="original"
        href="some-document.xml"
        parameters="map{'cx:xmlnt':true()}"/>

<p:xslt>
  <p:with-input port="stylesheet"
                href="some-transform.xsl"/>
</p:xslt>

<cx:merge-properties>
  <p:with-input port="alternate"
                pipe="@original"/>
</cx:merge-properties>

</p:declare-step>
