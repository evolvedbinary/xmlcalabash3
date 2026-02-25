<p:declare-step version="3.0" xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                name="main">
  <p:output port="result" serialization="map{'method':'xml','indent':true()}"/>

  <p:load href="https://central.sonatype.com/artifact/org.apache.logging.log4j/log4j-to-slf4j"/>

</p:declare-step>
