<p:declare-step version="3.0" xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                name="main">
  <p:output port="result" primary="true"/>
  <p:output port="alternate" pipe="@alt"/>

  <p:identity>
    <p:with-input>
      <doc>alternate</doc>
    </p:with-input>
  </p:identity>

  <p:set-properties name="alt"
                    xmlns:cx="http://xmlcalabash.com/ns/extensions"
                    properties="map { 'author': 'ndw',
                                      'purpose': 'testing',
                                      'cx:motto': 'Spoon!' }"/>

  <p:identity>
    <p:with-input>
      <doc>primary</doc>
    </p:with-input>
  </p:identity>

</p:declare-step>
