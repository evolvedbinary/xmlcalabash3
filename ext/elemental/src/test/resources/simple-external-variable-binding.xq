xquery version "3.1";

declare namespace st = "http://smoke-test";

declare variable $name as xs:string external;
declare variable $attr as attribute() external;
declare variable $map as map(*) external;
declare variable $array as array(*) external;

document {
    element { $name } {
        text { "Hello, world." }
    }
}
,
element { xs:QName("st:" || $name) } {
    text { "Hello, continent." }
}
,
element { $name } {
    text { "Hello, country." }
}
,
$attr
,
attribute { xs:QName("st:" || $name) } { "Hello, region."}
,
attribute { $name } { "Hello, state."}
,
text { $name || ": Hello, town." }
,
comment { $name || ": Hello, village." }
,
processing-instruction { $name } { "Hello" }
,
$name || ": Hello, hamlet."
,
xs:integer(1234567890)
,
xs:nonPositiveInteger(-123456789)
,
xs:float(1.2)
,
xs:double(2.3)
,
xs:time("02:00:00Z")
,
xs:dateTime("1981-02-04T02:00:00Z")
,
xs:dateTimeStamp("1981-02-04T02:00:00Z")
,
$map
,
map {
    "key1": $name || "1",
    "key2": $name || "2",
    "key3": element { $name } { text { "Hello, country." } }
}
,
map {
    "key1": $name || "1",
    "key2": map {
        "key3": element { $name } { text { "Hello, country." } }
    }
}
,
$array
,
[
    ($name, "you"),
    element { $name } { text { "Hello, country." } }
]
,
array {
    $name,
    ["you"]
}
