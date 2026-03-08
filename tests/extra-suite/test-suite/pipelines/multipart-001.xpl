<p:declare-step version="3.0" xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-inline-prefixes="#all"
                name="main">
  <p:output port="result" sequence="true"
            serialization="map{'method':'xml', 'indent':true(), 'omit-xml-declaration':true()}"/>

  <p:identity name="greeting">
    <p:with-input><greeting>Hello, world.</greeting></p:with-input>
  </p:identity>
 
  <p:identity name="alternate-greeting">
    <p:with-input><greeting>Yo, yo, yo, yo!</greeting></p:with-input>
  </p:identity>

  <p:identity name="icon">
    <p:with-input>
      <p:inline encoding="base64" content-type="image/webp"
>UklGRiAEAABXRUJQVlA4WAoAAAAQAAAAHwAAHwAAQUxQSCECAAABkC1Jtmlbva1zbdu2bd
u2bdu2bdu2bdv3mHsf9MOaOJ8QERMA5WTd1h6Y1u+A//1Vzx5sKm+GrrPBsQgyNoribz2d
Gs7pIdQM2N3FqWBK1Cuc+uEDzLLG1/wZn/9bSNzHGM+vywpMpV+qeAPjFOIOC4q9pjFo4f
5bmyZ2qjTherDsnKBztGCNw53UBgBJax2IFXgnG1wrSDJ6e0YoZrwrOJvDUM2f5LW+iaBc
8z3JczlhbOMl/1WHbtlFp+ZlgTDV0Yiffa1agMsEpweAqczI7f1s0HaUGDRtypLRAGzrw/
+fTqRVYNP9tX1rVSgOU+Ksw2+c35TXrVH20Y0yFlgSIMXoK1/+BgUFfjnSK4FKkSevCgOp
6/YqtzGG0vCGCinPRPcEkDxvoaLjvsgu5paZxvJMEgBZa1VLiWahos9FIS/9LaI5AGv+oh
WdSP9KtNMuS3iAF5MCcBYoUtMF20bRQsg7R3q7AECxFmVyAujgFcyVZXvEKykMKXN1zQgg
4xPBTIltEb2dAcBstqayAMAUwSJJLX9eSwkgUZZs2SAs8N6wyyEodZtRXQC4C1VtUVVkGh
tL8n0eg2UleSGZFclyVavVMbkIaS+TjFuRALB0/E8utKVJW7dXj3oOyMu+Ixm1r1fbpf7k
hczpc9cf2KihHap1XpFkTBTJO8VQoWfnZmksUC+4LYBG3/ECcBTvmSkxtD31d/6ODHk8Ii
XgSpEYigBWUDgg2AEAANAIAJ0BKiAAIAA+kTqXR6WjIiEwCACwEglpABFEYvdYDLA0gXuD
oJM2P0b7Af6pf7TgL/1yW16OyB/ggoIlY9WeflnTO+mpaZXy2UHVKoAA/vQwb5IIhbS0M9
ahMCBU5Zo61A9Zwvifc9gRvLO3YtqLLt0d/Sj38f9e2xh5k9S+luTyAd3UMniH4JgZf+ts
fxETBt1z77/no0eRiEKMVc+qMxP47NVteuKc3nTI9IK7tb772GzRhHxTXR2IlkwPy/T+/F
CoOC8DXmsR8nqz6VtBu3RX8rDCexM0drikVoDZyL/GreNYdOTKvkK4RepSJ7UvMaFqE6Jb
i1c9b7ikmzfjj3wQin3EMf/YixdU0RB18C0FAcQ5xWIoHdkde9qp9eBupnoASo4N7p3n89
IeNd/1WX0f+i0zgu+vu7R91JVYs99B57pUVOth7bsz0YTa9C+bOUhMeX+F7ge5vssG9b9/
i8EDkYT0tB+dR+j9HrA9uXPmshvux/eoH9T3SvFAR+e6gjhffYLZ813uhNxSZ8ah8Pw/Bq
zRBpCv1eucxRJFvJlpAhVhPS3qXXnwvphNce+ZQkDX8gBy6zQcdzv6UeG4OG96c2FABqWf
MovnvTayJSOosTxAAAA=</p:inline>
    </p:with-input>
  </p:identity>

  <p:identity>
    <p:with-input pipe="@greeting @alternate-greeting @icon"/>
  </p:identity>


</p:declare-step>
