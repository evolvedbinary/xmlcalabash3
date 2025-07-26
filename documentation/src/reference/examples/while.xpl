<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:xs="http://www.w3.org/2001/XMLSchema" 
                exclude-inline-prefixes="#all"
                name="main" version="3.1">
  <p:output port="result" serialization="map{'indent':true()}"/>

  <p:identity name="identity">
    <p:with-input>
      <doc count="3"/>
    </p:with-input>
  </p:identity>

  <cx:while test="/doc/@count and xs:integer(/doc/@count) gt 0">
    <p:insert position="first-child">
      <p:with-input port="insertion">
        <insertion for="{/doc/@count}"/>
      </p:with-input>
    </p:insert>

    <p:add-attribute attribute-name="count" attribute-value="{xs:integer(/doc/@count) - 1}"/>

    <p:if test="xs:integer(/doc/@count) = 0">
      <p:delete match="/doc/@count"/>
    </p:if>
  </cx:while>

</p:declare-step>
