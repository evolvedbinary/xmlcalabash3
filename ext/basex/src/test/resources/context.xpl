<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-inline-prefixes="cx xs" version="3.0">

<p:import href="https://xmlcalabash.com/ext/library/basex.xpl"/>

<p:output port="result" sequence="true"/>

<!-- 'cx:host':'localhost', 'cx:username':'admin', 'cx:password':'admin', -->

<cx:basex p:expand-text="false"
          parameters="map{}">
  <p:with-input>
    <doc name="one"/>
    <doc name="two"/>
    <doc name="one"/>
  </p:with-input>
  <p:with-input port="query">
    <p:inline content-type="text/plain">
(., count(/))
</p:inline>
  </p:with-input>
</cx:basex>

<!--
(/doc[@name="one"], "hello", 17, (/)[1])
-->
</p:declare-step>
