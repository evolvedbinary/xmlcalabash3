package com.xmlcalabash.parsers;

public interface EventHandler {
    void reset(CharSequence string);
    void startNonterminal(String name, int begin);
    void endNonterminal(String name, int end);
    void terminal(String name, int begin, int end);
    void whitespace(int begin, int end);
}
