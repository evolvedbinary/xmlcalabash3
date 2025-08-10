<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-inline-prefixes="cx xs" version="3.0">

<p:import href="https://xmlcalabash.com/ext/library/basex.xpl"/>

<p:output port="result"/>

<cx:basex parameters="map{'name': 'EEE'}" p:expand-text="false">
  <p:with-input><p:empty/></p:with-input>
  <p:with-input port="query">
    <p:inline content-type="text/plain">
declare variable $name external;
element { $name } { "Hello, world." }
    </p:inline>
  </p:with-input>
</cx:basex>

</p:declare-step>
