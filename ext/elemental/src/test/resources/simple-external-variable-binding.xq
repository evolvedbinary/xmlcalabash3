xquery version "1.0";

declare variable $name external;

element { $name } {
    text { "Hello, world." }
}