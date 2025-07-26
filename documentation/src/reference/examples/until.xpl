<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:xs="http://www.w3.org/2001/XMLSchema" 
                exclude-inline-prefixes="#all"
                name="main" version="3.1">
  <p:output port="result" serialization="map{'indent':true()}"/>

  <p:identity name="identity">
    <p:with-input>
      <list>
        <item/>
        <item/>
        <item/>
      </list>
    </p:with-input>
  </p:identity>

  <cx:until test="deep-equal(., $cx:previous)">
    <p:replace match="/list/item[1]">
      <p:with-input port="replacement">
        <li number="{p:iteration-position()}"/>
      </p:with-input>
    </p:replace>
  </cx:until>

</p:declare-step>
