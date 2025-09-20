<?xml version="1.0" encoding="UTF-8"?>
<p:library xmlns:p="http://www.w3.org/ns/xproc"
           xmlns:xs="http://www.w3.org/2001/XMLSchema" 
           xmlns:ex="http://example.com/ns"
           version="3.1">

  <p:declare-step type="ex:hash">
    <p:output port="result" content-types="text"/>
    <p:option name="text" as="xs:string" required="true"/>
    <p:option name="algorithm" as="xs:string" select="'sha'"/>
    <p:option name="version" as="xs:string?"
              select="if ($algorithm = 'sha') then '256' else ()"/>

    <ex:private-hash text="{$text}" algorithm="{$algorithm}" version="{$version}"/>
  </p:declare-step>

  <p:declare-step type="ex:hash-function">
    <p:output port="result" content-types="text"/>
    <p:option name="text" as="xs:string" required="true"/>
    <p:option name="algorithm" as="xs:string" select="'sha'"/>
    <p:option name="version" as="xs:string?"
              select="if ($algorithm = 'sha') then '256' else ()"/>

    <p:identity>
      <p:with-input select="hash/node()">
        <hash>{ex:private-hash(map{'text': 'Hello, world.'})?result}</hash>
      </p:with-input>
    </p:identity>
  </p:declare-step>

  <p:declare-step type="ex:private-hash" visibility="private">
    <p:output port="result" content-types="text"/>
    <p:option name="text" as="xs:string" required="true"/>
    <p:option name="algorithm" as="xs:string" select="'sha'"/>
    <p:option name="version" as="xs:string?"
              select="if ($algorithm = 'sha') then '256' else ()"/>

    <p:hash algorithm="{$algorithm}" value="{$text}" match="/doc">
      <p:with-option name="version" select="$version"/>
      <p:with-input>
        <doc/>
      </p:with-input>
    </p:hash>
  </p:declare-step>

</p:library>
