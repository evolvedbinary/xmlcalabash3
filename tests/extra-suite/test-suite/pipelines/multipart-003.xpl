<p:declare-step version="3.0" xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-inline-prefixes="#all"
                name="main">
  <p:input port="source" primary="true" sequence="true"/>
  <p:input port="alternate" sequence="true"/>
  <p:output port="result"/>

  <p:count name="src">
    <p:with-input pipe="source@main"/>
  </p:count>

  <p:count name="alt">
    <p:with-input pipe="alternate@main"/>
  </p:count>

  <p:wrap-sequence wrapper="wrapper">
    <p:with-input pipe="@src @alt"/>
  </p:wrap-sequence>
</p:declare-step>
