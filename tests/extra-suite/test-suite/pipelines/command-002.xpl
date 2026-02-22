<p:declare-step version="3.0" xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                name="main">
  <p:output port="result" primary="true"/>
  <p:output port="image" pipe="@image"/>
  <p:output port="json" pipe="@json" serialization="map{'method':'json'}"/>
  <p:output port="text" pipe="@text" serialization="map{'method':'text'}"/>

  <p:load name="image" href="../documents/document.png"/> 
  <p:load name="json" href="../documents/rdf.json"/> 
  <p:load name="text" href="../documents/document.txt"/> 

  <p:identity>
    <p:with-input>
      <doc>primary</doc>
    </p:with-input>
  </p:identity>

</p:declare-step>
