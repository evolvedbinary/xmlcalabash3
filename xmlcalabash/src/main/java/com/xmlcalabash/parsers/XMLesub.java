package com.xmlcalabash.parsers;

// This file was generated on Sat Aug 30, 2025 15:23 (UTC+01) by REx v6.2-SNAPSHOT which is Copyright (c) 1979-2025 by Gunther Rademacher <grd@gmx.net>
// REx command line: -faster -java -tree XMLesub.ebnf

import java.util.Arrays;

public class XMLesub
{
    public static class ParseException extends RuntimeException
    {
        private static final long serialVersionUID = 1L;
        private int begin, end, offending, expected, state;

        public ParseException(int b, int e, int s, int o, int x)
        {
            begin = b;
            end = e;
            state = s;
            offending = o;
            expected = x;
        }

        @Override
        public String getMessage()
        {
            return offending < 0
                    ? "lexical analysis failed"
                    : "syntax error";
        }

        public void serialize(EventHandler eventHandler)
        {
        }

        public int getBegin() {return begin;}
        public int getEnd() {return end;}
        public int getState() {return state;}
        public int getOffending() {return offending;}
        public int getExpected() {return expected;}
        public boolean isAmbiguousInput() {return false;}
    }

    public static class TopDownTreeBuilder implements EventHandler
    {
        private CharSequence input = null;
        private Nonterminal[] stack = new Nonterminal[64];
        private int top = -1;

        @Override
        public void reset(CharSequence input)
        {
            this.input = input;
            top = -1;
        }

        @Override
        public void startNonterminal(String name, int begin)
        {
            Nonterminal nonterminal = new Nonterminal(name, begin, begin, new Symbol[0]);
            if (top >= 0) addChild(nonterminal);
            if (++top >= stack.length) stack = Arrays.copyOf(stack, stack.length << 1);
            stack[top] = nonterminal;
        }

        @Override
        public void endNonterminal(String name, int end)
        {
            stack[top].end = end;
            if (top > 0) --top;
        }

        @Override
        public void terminal(String name, int begin, int end)
        {
            addChild(new Terminal(name, begin, end));
        }

        @Override
        public void whitespace(int begin, int end)
        {
        }

        private void addChild(Symbol s)
        {
            Nonterminal current = stack[top];
            current.children = Arrays.copyOf(current.children, current.children.length + 1);
            current.children[current.children.length - 1] = s;
        }

        public void serialize(EventHandler e)
        {
            e.reset(input);
            stack[0].send(e);
        }
    }

    public static abstract class Symbol
    {
        public String name;
        public int begin;
        public int end;

        protected Symbol(String name, int begin, int end)
        {
            this.name = name;
            this.begin = begin;
            this.end = end;
        }

        public abstract void send(EventHandler e);
    }

    public static class Terminal extends Symbol
    {
        public Terminal(String name, int begin, int end)
        {
            super(name, begin, end);
        }

        @Override
        public void send(EventHandler e)
        {
            e.terminal(name, begin, end);
        }
    }

    public static class Nonterminal extends Symbol
    {
        public Symbol[] children;

        public Nonterminal(String name, int begin, int end, Symbol[] children)
        {
            super(name, begin, end);
            this.children = children;
        }

        @Override
        public void send(EventHandler e)
        {
            e.startNonterminal(name, begin);
            int pos = begin;
            for (Symbol c : children)
            {
                if (pos < c.begin) e.whitespace(pos, c.begin);
                c.send(e);
                pos = c.end;
            }
            if (pos < end) e.whitespace(pos, end);
            e.endNonterminal(name, end);
        }
    }

    public XMLesub(CharSequence string, EventHandler t)
    {
        initialize(string, t);
    }

    public void initialize(CharSequence source, EventHandler parsingEventHandler)
    {
        eventHandler = parsingEventHandler;
        input = source;
        size = source.length();
        reset(0, 0, 0);
    }

    public CharSequence getInput()
    {
        return input;
    }

    public int getTokenOffset()
    {
        return b0;
    }

    public int getTokenEnd()
    {
        return e0;
    }

    public final void reset(int l, int b, int e)
    {
        b0 = b; e0 = b;
        l1 = l; b1 = b; e1 = e;
        l2 = 0; b2 = 0; e2 = 0;
        l3 = 0; b3 = 0; e3 = 0;
        end = e;
        eventHandler.reset(input);
    }

    public void reset()
    {
        reset(0, 0, 0);
    }

    public static String getOffendingToken(ParseException e)
    {
        return e.getOffending() < 0 ? null : TOKEN[e.getOffending()];
    }

    public static String[] getExpectedTokenSet(ParseException e)
    {
        String[] expected;
        if (e.getExpected() >= 0)
        {
            expected = new String[]{TOKEN[e.getExpected()]};
        }
        else
        {
            expected = getTokenSet(- e.getState());
        }
        return expected;
    }

    public String getErrorMessage(ParseException e)
    {
        String message = e.getMessage();
        String[] tokenSet = getExpectedTokenSet(e);
        String found = getOffendingToken(e);
        int size = e.getEnd() - e.getBegin();
        message += (found == null ? "" : ", found " + found)
                + "\nwhile expecting "
                + (tokenSet.length == 1 ? tokenSet[0] : java.util.Arrays.toString(tokenSet))
                + "\n"
                + (size == 0 || found != null ? "" : "after successfully scanning " + size + " characters beginning ");
        String prefix = input.subSequence(0, e.getBegin()).toString();
        int line = prefix.replaceAll("[^\n]", "").length() + 1;
        int column = prefix.length() - prefix.lastIndexOf('\n');
        return message
                + "at line " + line + ", column " + column + ":\n..."
                + input.subSequence(e.getBegin(), Math.min(input.length(), e.getBegin() + 64))
                + "...";
    }

    public void parse_extSubset()
    {
        eventHandler.startNonterminal("extSubset", e0);
        lookahead1(64);                 // EOF | S | Comment | PI | PEReference | '<!ATTLIST' | '<!ELEMENT' | '<!ENTITY' |
        // '<!NOTATION' | '<![' | '<?xml'
        if (l1 == 35)                   // '<?xml'
        {
            parse_TextDecl();
        }
        parse_extSubsetDecl();
        consume(4);                     // EOF
        eventHandler.endNonterminal("extSubset", e0);
    }

    private void parse_extSubsetDecl()
    {
        eventHandler.startNonterminal("extSubsetDecl", e0);
        for (;;)
        {
            lookahead1(65);               // EOF | S | Comment | PI | PEReference | '<!ATTLIST' | '<!ELEMENT' | '<!ENTITY' |
            // '<!NOTATION' | '<![' | ']]>'
            if (l1 == 4                   // EOF
                    || l1 == 57)                 // ']]>'
            {
                break;
            }
            switch (l1)
            {
                case 34:                      // '<!['
                    parse_conditionalSect();
                    break;
                case 5:                       // S
                case 15:                      // PEReference
                    parse_DeclSep();
                    break;
                default:
                    parse_markupdecl();
            }
        }
        eventHandler.endNonterminal("extSubsetDecl", e0);
    }

    private void parse_TextDecl()
    {
        eventHandler.startNonterminal("TextDecl", e0);
        consume(35);                    // '<?xml'
        parse_VersionInfo();
        lookahead1(29);                 // S | '?>'
        switch (l1)
        {
            case 5:                         // S
                lookahead2(42);               // '?>' | 'encoding'
                break;
            default:
                lk = l1;
        }
        if (lk == 3717)                 // S 'encoding'
        {
            parse_EncodingDecl();
        }
        lookahead1(29);                 // S | '?>'
        if (l1 == 5)                    // S
        {
            consume(5);                   // S
        }
        lookahead1(16);                 // '?>'
        consume(39);                    // '?>'
        eventHandler.endNonterminal("TextDecl", e0);
    }

    private void parse_VersionInfo()
    {
        eventHandler.startNonterminal("VersionInfo", e0);
        lookahead1(0);                  // S
        consume(5);                     // S
        lookahead1(22);                 // 'version'
        consume(59);                    // 'version'
        parse_Eq();
        lookahead1(37);                 // '"' | "'"
        switch (l1)
        {
            case 23:                        // "'"
                consume(23);                  // "'"
                lookahead1(6);                // VersionNum
                consume(14);                  // VersionNum
                lookahead1(11);               // "'"
                consume(23);                  // "'"
                break;
            default:
                consume(17);                  // '"'
                lookahead1(6);                // VersionNum
                consume(14);                  // VersionNum
                lookahead1(8);                // '"'
                consume(17);                  // '"'
        }
        eventHandler.endNonterminal("VersionInfo", e0);
    }

    private void parse_Eq()
    {
        eventHandler.startNonterminal("Eq", e0);
        lookahead1(27);                 // S | '='
        if (l1 == 5)                    // S
        {
            consume(5);                   // S
        }
        lookahead1(14);                 // '='
        consume(36);                    // '='
        lookahead1(46);                 // S | '"' | "'"
        if (l1 == 5)                    // S
        {
            consume(5);                   // S
        }
        eventHandler.endNonterminal("Eq", e0);
    }

    private void parse_DeclSep()
    {
        eventHandler.startNonterminal("DeclSep", e0);
        switch (l1)
        {
            case 15:                        // PEReference
                consume(15);                  // PEReference
                break;
            default:
                consume(5);                   // S
        }
        eventHandler.endNonterminal("DeclSep", e0);
    }

    private void parse_markupdecl()
    {
        eventHandler.startNonterminal("markupdecl", e0);
        switch (l1)
        {
            case 31:                        // '<!ELEMENT'
                parse_elementdecl();
                break;
            case 30:                        // '<!ATTLIST'
                parse_AttlistDecl();
                break;
            case 32:                        // '<!ENTITY'
                parse_EntityDecl();
                break;
            case 33:                        // '<!NOTATION'
                parse_NotationDecl();
                break;
            case 13:                        // PI
                consume(13);                  // PI
                break;
            default:
                consume(12);                  // Comment
        }
        eventHandler.endNonterminal("markupdecl", e0);
    }

    private void parse_elementdecl()
    {
        eventHandler.startNonterminal("elementdecl", e0);
        consume(31);                    // '<!ELEMENT'
        lookahead1(0);                  // S
        consume(5);                     // S
        lookahead1(1);                  // Name
        consume(6);                     // Name
        lookahead1(0);                  // S
        consume(5);                     // S
        parse_contentspec();
        lookahead1(28);                 // S | '>'
        if (l1 == 5)                    // S
        {
            consume(5);                   // S
        }
        lookahead1(15);                 // '>'
        consume(37);                    // '>'
        eventHandler.endNonterminal("elementdecl", e0);
    }

    private void parse_contentspec()
    {
        eventHandler.startNonterminal("contentspec", e0);
        lookahead1(53);                 // '(' | 'ANY' | 'EMPTY'
        switch (l1)
        {
            case 24:                        // '('
                lookahead2(56);               // S | Name | '#PCDATA' | '('
                switch (lk)
                {
                    case 344:                     // '(' S
                        lookahead3(51);             // Name | '#PCDATA' | '('
                        break;
                }
                break;
            default:
                lk = l1;
        }
        switch (lk)
        {
            case 42:                        // 'EMPTY'
                consume(42);                  // 'EMPTY'
                break;
            case 40:                        // 'ANY'
                consume(40);                  // 'ANY'
                break;
            case 1304:                      // '(' '#PCDATA'
            case 82264:                     // '(' S '#PCDATA'
                parse_Mixed();
                break;
            default:
                parse_children();
        }
        eventHandler.endNonterminal("contentspec", e0);
    }

    private void parse_children()
    {
        eventHandler.startNonterminal("children", e0);
        parse_choiceOrSeq();
        lookahead1(61);                 // S | '*' | '+' | '>' | '?'
        if (l1 != 5                     // S
                && l1 != 37)                   // '>'
        {
            switch (l1)
            {
                case 38:                      // '?'
                    consume(38);                // '?'
                    break;
                case 27:                      // '*'
                    consume(27);                // '*'
                    break;
                default:
                    consume(28);                // '+'
            }
        }
        eventHandler.endNonterminal("children", e0);
    }

    private void parse_cp()
    {
        eventHandler.startNonterminal("cp", e0);
        lookahead1(34);                 // Name | '('
        switch (l1)
        {
            case 6:                         // Name
                consume(6);                   // Name
                break;
            default:
                parse_choiceOrSeq();
        }
        lookahead1(62);                 // S | ')' | '*' | '+' | ',' | '?' | '|'
        if (l1 == 27                    // '*'
                || l1 == 28                    // '+'
                || l1 == 38)                   // '?'
        {
            switch (l1)
            {
                case 38:                      // '?'
                    consume(38);                // '?'
                    break;
                case 27:                      // '*'
                    consume(27);                // '*'
                    break;
                default:
                    consume(28);                // '+'
            }
        }
        eventHandler.endNonterminal("cp", e0);
    }

    private void parse_choiceOrSeq()
    {
        eventHandler.startNonterminal("choiceOrSeq", e0);
        consume(24);                    // '('
        lookahead1(45);                 // S | Name | '('
        if (l1 == 5)                    // S
        {
            consume(5);                   // S
        }
        parse_cp();
        lookahead1(58);                 // S | ')' | ',' | '|'
        if (l1 == 5)                    // S
        {
            consume(5);                   // S
        }
        lookahead1(55);                 // ')' | ',' | '|'
        switch (l1)
        {
            case 60:                        // '|'
                for (;;)
                {
                    consume(60);                // '|'
                    lookahead1(45);             // S | Name | '('
                    if (l1 == 5)                // S
                    {
                        consume(5);               // S
                    }
                    parse_cp();
                    lookahead1(48);             // S | ')' | '|'
                    if (l1 == 5)                // S
                    {
                        consume(5);               // S
                    }
                    lookahead1(39);             // ')' | '|'
                    if (l1 != 60)               // '|'
                    {
                        break;
                    }
                }
                break;
            default:
                for (;;)
                {
                    lookahead1(38);             // ')' | ','
                    if (l1 != 29)               // ','
                    {
                        break;
                    }
                    consume(29);                // ','
                    lookahead1(45);             // S | Name | '('
                    if (l1 == 5)                // S
                    {
                        consume(5);               // S
                    }
                    parse_cp();
                    lookahead1(47);             // S | ')' | ','
                    if (l1 == 5)                // S
                    {
                        consume(5);               // S
                    }
                }
        }
        consume(25);                    // ')'
        eventHandler.endNonterminal("choiceOrSeq", e0);
    }

    private void parse_Mixed()
    {
        eventHandler.startNonterminal("Mixed", e0);
        consume(24);                    // '('
        lookahead1(26);                 // S | '#PCDATA'
        if (l1 == 5)                    // S
        {
            consume(5);                   // S
        }
        lookahead1(9);                  // '#PCDATA'
        consume(20);                    // '#PCDATA'
        lookahead1(57);                 // S | ')' | ')*' | '|'
        if (l1 == 5)                    // S
        {
            consume(5);                   // S
        }
        lookahead1(54);                 // ')' | ')*' | '|'
        switch (l1)
        {
            case 25:                        // ')'
                consume(25);                  // ')'
                break;
            default:
                for (;;)
                {
                    lookahead1(40);             // ')*' | '|'
                    if (l1 != 60)               // '|'
                    {
                        break;
                    }
                    consume(60);                // '|'
                    lookahead1(24);             // S | Name
                    if (l1 == 5)                // S
                    {
                        consume(5);               // S
                    }
                    lookahead1(1);              // Name
                    consume(6);                 // Name
                    lookahead1(49);             // S | ')*' | '|'
                    if (l1 == 5)                // S
                    {
                        consume(5);               // S
                    }
                }
                consume(26);                  // ')*'
        }
        eventHandler.endNonterminal("Mixed", e0);
    }

    private void parse_AttlistDecl()
    {
        eventHandler.startNonterminal("AttlistDecl", e0);
        consume(30);                    // '<!ATTLIST'
        lookahead1(0);                  // S
        consume(5);                     // S
        lookahead1(1);                  // Name
        consume(6);                     // Name
        for (;;)
        {
            lookahead1(28);               // S | '>'
            switch (l1)
            {
                case 5:                       // S
                    lookahead2(35);             // Name | '>'
                    break;
                default:
                    lk = l1;
            }
            if (lk != 389)                // S Name
            {
                break;
            }
            parse_AttDef();
        }
        if (l1 == 5)                    // S
        {
            consume(5);                   // S
        }
        lookahead1(15);                 // '>'
        consume(37);                    // '>'
        eventHandler.endNonterminal("AttlistDecl", e0);
    }

    private void parse_AttDef()
    {
        eventHandler.startNonterminal("AttDef", e0);
        consume(5);                     // S
        lookahead1(1);                  // Name
        consume(6);                     // Name
        lookahead1(0);                  // S
        consume(5);                     // S
        parse_AttType();
        lookahead1(0);                  // S
        consume(5);                     // S
        parse_DefaultDecl();
        eventHandler.endNonterminal("AttDef", e0);
    }

    private void parse_AttType()
    {
        eventHandler.startNonterminal("AttType", e0);
        lookahead1(63);                 // '(' | 'CDATA' | 'ENTITIES' | 'ENTITY' | 'ID' | 'IDREF' | 'IDREFS' | 'NMTOKEN' |
        // 'NMTOKENS' | 'NOTATION'
        switch (l1)
        {
            case 41:                        // 'CDATA'
                parse_StringType();
                break;
            case 24:                        // '('
            case 53:                        // 'NOTATION'
                parse_EnumeratedType();
                break;
            default:
                parse_TokenizedType();
        }
        eventHandler.endNonterminal("AttType", e0);
    }

    private void parse_StringType()
    {
        eventHandler.startNonterminal("StringType", e0);
        consume(41);                    // 'CDATA'
        eventHandler.endNonterminal("StringType", e0);
    }

    private void parse_TokenizedType()
    {
        eventHandler.startNonterminal("TokenizedType", e0);
        switch (l1)
        {
            case 45:                        // 'ID'
                consume(45);                  // 'ID'
                break;
            case 46:                        // 'IDREF'
                consume(46);                  // 'IDREF'
                break;
            case 47:                        // 'IDREFS'
                consume(47);                  // 'IDREFS'
                break;
            case 44:                        // 'ENTITY'
                consume(44);                  // 'ENTITY'
                break;
            case 43:                        // 'ENTITIES'
                consume(43);                  // 'ENTITIES'
                break;
            case 51:                        // 'NMTOKEN'
                consume(51);                  // 'NMTOKEN'
                break;
            default:
                consume(52);                  // 'NMTOKENS'
        }
        eventHandler.endNonterminal("TokenizedType", e0);
    }

    private void parse_EnumeratedType()
    {
        eventHandler.startNonterminal("EnumeratedType", e0);
        switch (l1)
        {
            case 53:                        // 'NOTATION'
                parse_NotationType();
                break;
            default:
                parse_Enumeration();
        }
        eventHandler.endNonterminal("EnumeratedType", e0);
    }

    private void parse_NotationType()
    {
        eventHandler.startNonterminal("NotationType", e0);
        consume(53);                    // 'NOTATION'
        lookahead1(0);                  // S
        consume(5);                     // S
        lookahead1(12);                 // '('
        consume(24);                    // '('
        lookahead1(24);                 // S | Name
        if (l1 == 5)                    // S
        {
            consume(5);                   // S
        }
        lookahead1(1);                  // Name
        consume(6);                     // Name
        for (;;)
        {
            lookahead1(48);               // S | ')' | '|'
            switch (l1)
            {
                case 5:                       // S
                    lookahead2(39);             // ')' | '|'
                    break;
                default:
                    lk = l1;
            }
            if (lk != 60                  // '|'
                    && lk != 3845)               // S '|'
            {
                break;
            }
            if (l1 == 5)                  // S
            {
                consume(5);                 // S
            }
            lookahead1(23);               // '|'
            consume(60);                  // '|'
            lookahead1(24);               // S | Name
            if (l1 == 5)                  // S
            {
                consume(5);                 // S
            }
            lookahead1(1);                // Name
            consume(6);                   // Name
        }
        if (l1 == 5)                    // S
        {
            consume(5);                   // S
        }
        lookahead1(13);                 // ')'
        consume(25);                    // ')'
        eventHandler.endNonterminal("NotationType", e0);
    }

    private void parse_Enumeration()
    {
        eventHandler.startNonterminal("Enumeration", e0);
        consume(24);                    // '('
        lookahead1(25);                 // S | Nmtoken
        if (l1 == 5)                    // S
        {
            consume(5);                   // S
        }
        lookahead1(2);                  // Nmtoken
        consume(7);                     // Nmtoken
        for (;;)
        {
            lookahead1(48);               // S | ')' | '|'
            switch (l1)
            {
                case 5:                       // S
                    lookahead2(39);             // ')' | '|'
                    break;
                default:
                    lk = l1;
            }
            if (lk != 60                  // '|'
                    && lk != 3845)               // S '|'
            {
                break;
            }
            if (l1 == 5)                  // S
            {
                consume(5);                 // S
            }
            lookahead1(23);               // '|'
            consume(60);                  // '|'
            lookahead1(25);               // S | Nmtoken
            if (l1 == 5)                  // S
            {
                consume(5);                 // S
            }
            lookahead1(2);                // Nmtoken
            consume(7);                   // Nmtoken
        }
        if (l1 == 5)                    // S
        {
            consume(5);                   // S
        }
        lookahead1(13);                 // ')'
        consume(25);                    // ')'
        eventHandler.endNonterminal("Enumeration", e0);
    }

    private void parse_DefaultDecl()
    {
        eventHandler.startNonterminal("DefaultDecl", e0);
        lookahead1(59);                 // AttValue | '#FIXED' | '#IMPLIED' | '#REQUIRED'
        switch (l1)
        {
            case 21:                        // '#REQUIRED'
                consume(21);                  // '#REQUIRED'
                break;
            case 19:                        // '#IMPLIED'
                consume(19);                  // '#IMPLIED'
                break;
            default:
                if (l1 == 18)                 // '#FIXED'
                {
                    consume(18);                // '#FIXED'
                    lookahead1(0);              // S
                    consume(5);                 // S
                }
                lookahead1(3);                // AttValue
                consume(9);                   // AttValue
        }
        eventHandler.endNonterminal("DefaultDecl", e0);
    }

    private void parse_EntityDecl()
    {
        eventHandler.startNonterminal("EntityDecl", e0);
        switch (l1)
        {
            case 32:                        // '<!ENTITY'
                lookahead2(0);                // S
                switch (lk)
                {
                    case 352:                     // '<!ENTITY' S
                        lookahead3(33);             // Name | '%'
                        break;
                }
                break;
            default:
                lk = l1;
        }
        switch (lk)
        {
            case 24928:                     // '<!ENTITY' S Name
                parse_GEDecl();
                break;
            default:
                parse_PEDecl();
        }
        eventHandler.endNonterminal("EntityDecl", e0);
    }

    private void parse_GEDecl()
    {
        eventHandler.startNonterminal("GEDecl", e0);
        consume(32);                    // '<!ENTITY'
        lookahead1(0);                  // S
        consume(5);                     // S
        lookahead1(1);                  // Name
        consume(6);                     // Name
        lookahead1(0);                  // S
        consume(5);                     // S
        parse_EntityDef();
        lookahead1(28);                 // S | '>'
        if (l1 == 5)                    // S
        {
            consume(5);                   // S
        }
        lookahead1(15);                 // '>'
        consume(37);                    // '>'
        eventHandler.endNonterminal("GEDecl", e0);
    }

    private void parse_PEDecl()
    {
        eventHandler.startNonterminal("PEDecl", e0);
        consume(32);                    // '<!ENTITY'
        lookahead1(0);                  // S
        consume(5);                     // S
        lookahead1(10);                 // '%'
        consume(22);                    // '%'
        lookahead1(0);                  // S
        consume(5);                     // S
        lookahead1(1);                  // Name
        consume(6);                     // Name
        lookahead1(0);                  // S
        consume(5);                     // S
        parse_PEDef();
        lookahead1(28);                 // S | '>'
        if (l1 == 5)                    // S
        {
            consume(5);                   // S
        }
        lookahead1(15);                 // '>'
        consume(37);                    // '>'
        eventHandler.endNonterminal("PEDecl", e0);
    }

    private void parse_EntityDef()
    {
        eventHandler.startNonterminal("EntityDef", e0);
        lookahead1(52);                 // EntityValue | 'PUBLIC' | 'SYSTEM'
        switch (l1)
        {
            case 8:                         // EntityValue
                consume(8);                   // EntityValue
                break;
            default:
                parse_ExternalID();
                lookahead1(28);               // S | '>'
                switch (l1)
                {
                    case 5:                       // S
                        lookahead2(41);             // '>' | 'NDATA'
                        break;
                    default:
                        lk = l1;
                }
                if (lk == 3205)               // S 'NDATA'
                {
                    parse_NDataDecl();
                }
        }
        eventHandler.endNonterminal("EntityDef", e0);
    }

    private void parse_PEDef()
    {
        eventHandler.startNonterminal("PEDef", e0);
        lookahead1(52);                 // EntityValue | 'PUBLIC' | 'SYSTEM'
        switch (l1)
        {
            case 8:                         // EntityValue
                consume(8);                   // EntityValue
                break;
            default:
                parse_ExternalID();
        }
        eventHandler.endNonterminal("PEDef", e0);
    }

    private void parse_ExternalID()
    {
        eventHandler.startNonterminal("ExternalID", e0);
        switch (l1)
        {
            case 55:                        // 'SYSTEM'
                consume(55);                  // 'SYSTEM'
                lookahead1(0);                // S
                consume(5);                   // S
                lookahead1(4);                // SystemLiteral
                consume(10);                  // SystemLiteral
                break;
            default:
                consume(54);                  // 'PUBLIC'
                lookahead1(0);                // S
                consume(5);                   // S
                lookahead1(5);                // PubidLiteral
                consume(11);                  // PubidLiteral
                lookahead1(0);                // S
                consume(5);                   // S
                lookahead1(4);                // SystemLiteral
                consume(10);                  // SystemLiteral
        }
        eventHandler.endNonterminal("ExternalID", e0);
    }

    private void parse_NDataDecl()
    {
        eventHandler.startNonterminal("NDataDecl", e0);
        consume(5);                     // S
        lookahead1(19);                 // 'NDATA'
        consume(50);                    // 'NDATA'
        lookahead1(0);                  // S
        consume(5);                     // S
        lookahead1(1);                  // Name
        consume(6);                     // Name
        eventHandler.endNonterminal("NDataDecl", e0);
    }

    private void parse_EncodingDecl()
    {
        eventHandler.startNonterminal("EncodingDecl", e0);
        consume(5);                     // S
        lookahead1(21);                 // 'encoding'
        consume(58);                    // 'encoding'
        parse_Eq();
        lookahead1(37);                 // '"' | "'"
        switch (l1)
        {
            case 17:                        // '"'
                consume(17);                  // '"'
                lookahead1(7);                // EncName
                consume(16);                  // EncName
                lookahead1(8);                // '"'
                consume(17);                  // '"'
                break;
            default:
                consume(23);                  // "'"
                lookahead1(7);                // EncName
                consume(16);                  // EncName
                lookahead1(11);               // "'"
                consume(23);                  // "'"
        }
        eventHandler.endNonterminal("EncodingDecl", e0);
    }

    private void parse_NotationDecl()
    {
        eventHandler.startNonterminal("NotationDecl", e0);
        consume(33);                    // '<!NOTATION'
        lookahead1(0);                  // S
        consume(5);                     // S
        lookahead1(1);                  // Name
        consume(6);                     // Name
        lookahead1(0);                  // S
        consume(5);                     // S
        parse_ExternalOrPublicID();
        lookahead1(28);                 // S | '>'
        if (l1 == 5)                    // S
        {
            consume(5);                   // S
        }
        lookahead1(15);                 // '>'
        consume(37);                    // '>'
        eventHandler.endNonterminal("NotationDecl", e0);
    }

    private void parse_ExternalOrPublicID()
    {
        eventHandler.startNonterminal("ExternalOrPublicID", e0);
        lookahead1(44);                 // 'PUBLIC' | 'SYSTEM'
        switch (l1)
        {
            case 55:                        // 'SYSTEM'
                consume(55);                  // 'SYSTEM'
                lookahead1(0);                // S
                consume(5);                   // S
                lookahead1(4);                // SystemLiteral
                consume(10);                  // SystemLiteral
                break;
            default:
                consume(54);                  // 'PUBLIC'
                lookahead1(0);                // S
                consume(5);                   // S
                lookahead1(5);                // PubidLiteral
                consume(11);                  // PubidLiteral
                lookahead1(28);               // S | '>'
                switch (l1)
                {
                    case 5:                       // S
                        lookahead2(36);             // SystemLiteral | '>'
                        break;
                    default:
                        lk = l1;
                }
                if (lk == 645)                // S SystemLiteral
                {
                    consume(5);                 // S
                    lookahead1(4);              // SystemLiteral
                    consume(10);                // SystemLiteral
                }
        }
        eventHandler.endNonterminal("ExternalOrPublicID", e0);
    }

    private void parse_conditionalSect()
    {
        eventHandler.startNonterminal("conditionalSect", e0);
        switch (l1)
        {
            case 34:                        // '<!['
                lookahead2(50);               // S | 'IGNORE' | 'INCLUDE'
                switch (lk)
                {
                    case 354:                     // '<![' S
                        lookahead3(43);             // 'IGNORE' | 'INCLUDE'
                        break;
                }
                break;
            default:
                lk = l1;
        }
        switch (lk)
        {
            case 3170:                      // '<![' 'INCLUDE'
            case 201058:                    // '<![' S 'INCLUDE'
                parse_includeSect();
                break;
            default:
                parse_ignoreSect();
        }
        eventHandler.endNonterminal("conditionalSect", e0);
    }

    private void parse_includeSect()
    {
        eventHandler.startNonterminal("includeSect", e0);
        consume(34);                    // '<!['
        lookahead1(31);                 // S | 'INCLUDE'
        if (l1 == 5)                    // S
        {
            consume(5);                   // S
        }
        lookahead1(18);                 // 'INCLUDE'
        consume(49);                    // 'INCLUDE'
        lookahead1(32);                 // S | '['
        if (l1 == 5)                    // S
        {
            consume(5);                   // S
        }
        lookahead1(20);                 // '['
        consume(56);                    // '['
        parse_extSubsetDecl();
        consume(57);                    // ']]>'
        eventHandler.endNonterminal("includeSect", e0);
    }

    private void parse_ignoreSect()
    {
        eventHandler.startNonterminal("ignoreSect", e0);
        consume(34);                    // '<!['
        lookahead1(30);                 // S | 'IGNORE'
        if (l1 == 5)                    // S
        {
            consume(5);                   // S
        }
        lookahead1(17);                 // 'IGNORE'
        consume(48);                    // 'IGNORE'
        lookahead1(32);                 // S | '['
        if (l1 == 5)                    // S
        {
            consume(5);                   // S
        }
        lookahead1(20);                 // '['
        consume(56);                    // '['
        parse_ignoreSectContents();
        consume(57);                    // ']]>'
        eventHandler.endNonterminal("ignoreSect", e0);
    }

    private void parse_ignoreSectContents()
    {
        eventHandler.startNonterminal("ignoreSectContents", e0);
        for (;;)
        {
            lookahead1(60);               // NotOne | NotTwo | NotThree | '<![' | ']]>'
            if (l1 == 57)                 // ']]>'
            {
                break;
            }
            parse_Ignore();
        }
        eventHandler.endNonterminal("ignoreSectContents", e0);
    }

    private void parse_Ignore()
    {
        eventHandler.startNonterminal("Ignore", e0);
        switch (l1)
        {
            case 1:                         // NotOne
                consume(1);                   // NotOne
                break;
            case 2:                         // NotTwo
                consume(2);                   // NotTwo
                break;
            case 3:                         // NotThree
                consume(3);                   // NotThree
                break;
            default:
                consume(34);                  // '<!['
                parse_ignoreSectContents();
                consume(57);                  // ']]>'
        }
        eventHandler.endNonterminal("Ignore", e0);
    }

    private void consume(int t)
    {
        if (l1 == t)
        {
            eventHandler.terminal(TOKEN[l1], b1, e1);
            b0 = b1; e0 = e1; l1 = l2; if (l1 != 0) {
            b1 = b2; e1 = e2; l2 = l3; if (l2 != 0) {
                b2 = b3; e2 = e3; l3 = 0; }}
        }
        else
        {
            error(b1, e1, 0, l1, t);
        }
    }

    private void lookahead1(int tokenSetId)
    {
        if (l1 == 0)
        {
            l1 = match(tokenSetId);
            b1 = begin;
            e1 = end;
        }
    }

    private void lookahead2(int tokenSetId)
    {
        if (l2 == 0)
        {
            l2 = match(tokenSetId);
            b2 = begin;
            e2 = end;
        }
        lk = (l2 << 6) | l1;
    }

    private void lookahead3(int tokenSetId)
    {
        if (l3 == 0)
        {
            l3 = match(tokenSetId);
            b3 = begin;
            e3 = end;
        }
        lk |= l3 << 12;
    }

    private int error(int b, int e, int s, int l, int t)
    {
        throw new ParseException(b, e, s, l, t);
    }

    private int lk, b0, e0;
    private int l1, b1, e1;
    private int l2, b2, e2;
    private int l3, b3, e3;
    private EventHandler eventHandler = null;
    private CharSequence input = null;
    private int size = 0;
    private int begin = 0;
    private int end = 0;

    private int match(int tokenSetId)
    {
        begin = end;
        int current = end;
        int result = INITIAL[tokenSetId];
        int state = 0;

        for (int code = result & 255; code != 0; )
        {
            int charclass;
            int c0 = current < size ? input.charAt(current) : 0;
            ++current;
            if (c0 < 0x80)
            {
                charclass = MAP0[c0];
            }
            else if (c0 < 0xd800)
            {
                int c1 = c0 >> 4;
                charclass = MAP1[(c0 & 15) + MAP1[(c1 & 31) + MAP1[c1 >> 5]]];
            }
            else
            {
                if (c0 < 0xdc00)
                {
                    int c1 = current < size ? input.charAt(current) : 0;
                    if (c1 >= 0xdc00 && c1 < 0xe000)
                    {
                        ++current;
                        c0 = ((c0 & 0x3ff) << 10) + (c1 & 0x3ff) + 0x10000;
                    }
                }

                int lo = 0, hi = 4;
                for (int m = 2; ; m = (hi + lo) >> 1)
                {
                    if (MAP2[m] > c0) {hi = m - 1;}
                    else if (MAP2[5 + m] < c0) {lo = m + 1;}
                    else {charclass = MAP2[10 + m]; break;}
                    if (lo > hi) {charclass = 0; break;}
                }
            }

            state = code;
            int i0 = (charclass << 8) + code - 1;
            code = TRANSITION[(i0 & 7) + TRANSITION[i0 >> 3]];

            if (code > 255)
            {
                result = code;
                code &= 255;
                end = current;
            }
        }

        result >>= 8;
        if (result == 0)
        {
            end = current - 1;
            int c1 = end < size ? input.charAt(end) : 0;
            if (c1 >= 0xdc00 && c1 < 0xe000)
            {
                --end;
            }
            return error(begin, end, state, -1, -1);
        }

        if (end > size) end = size;
        return (result & 63) - 1;
    }

    private static String[] getTokenSet(int tokenSetId)
    {
        java.util.ArrayList<String> expected = new java.util.ArrayList<>();
        int s = tokenSetId < 0 ? - tokenSetId : INITIAL[tokenSetId] & 255;
        for (int i = 0; i < 61; i += 32)
        {
            int j = i;
            int i0 = (i >> 5) * 250 + s - 1;
            int f = EXPECTED[(i0 & 3) + EXPECTED[i0 >> 2]];
            for ( ; f != 0; f >>>= 1, ++j)
            {
                if ((f & 1) != 0)
                {
                    expected.add(TOKEN[j]);
                }
            }
        }
        return expected.toArray(new String[]{});
    }

    private static final int[] MAP0 =
            {
                    /*   0 */ 68, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 3, 4,
                    /*  35 */ 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 6, 17, 18, 17, 17, 17, 17, 17, 17, 17, 17, 19, 20, 21, 22, 23,
                    /*  63 */ 24, 6, 25, 26, 27, 28, 29, 30, 31, 32, 33, 32, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 32, 32, 45, 46,
                    /*  90 */ 32, 47, 48, 49, 48, 50, 48, 51, 51, 52, 53, 54, 51, 55, 32, 56, 32, 32, 57, 58, 59, 60, 32, 32, 61, 62,
                    /* 116 */ 32, 32, 63, 32, 64, 32, 32, 48, 65, 48, 48, 48
            };

    private static final int[] MAP1 =
            {
                    /*   0 */ 108, 124, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 156, 181, 181, 181, 181,
                    /*  21 */ 181, 214, 215, 213, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214,
                    /*  42 */ 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214,
                    /*  63 */ 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214,
                    /*  84 */ 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214,
                    /* 105 */ 214, 214, 214, 247, 261, 277, 308, 292, 324, 340, 356, 393, 393, 393, 385, 441, 433, 441, 433, 441, 441,
                    /* 126 */ 441, 441, 441, 441, 441, 441, 441, 441, 441, 441, 441, 441, 441, 441, 410, 410, 410, 410, 410, 410, 410,
                    /* 147 */ 426, 441, 441, 441, 441, 441, 441, 441, 441, 369, 393, 393, 394, 392, 393, 393, 441, 441, 441, 441, 441,
                    /* 168 */ 441, 441, 441, 441, 441, 441, 441, 441, 441, 441, 441, 441, 441, 393, 393, 393, 393, 393, 393, 393, 393,
                    /* 189 */ 393, 393, 393, 393, 393, 393, 393, 393, 393, 393, 393, 393, 393, 393, 393, 393, 393, 393, 393, 393, 393,
                    /* 210 */ 393, 393, 393, 440, 441, 441, 441, 441, 441, 441, 441, 441, 441, 441, 441, 441, 441, 441, 441, 441, 441,
                    /* 231 */ 441, 441, 441, 441, 441, 441, 441, 441, 441, 441, 441, 441, 441, 441, 441, 393, 68, 0, 0, 0, 0, 0, 0, 0,
                    /* 255 */ 0, 1, 2, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,
                    /* 289 */ 14, 15, 16, 6, 25, 26, 27, 28, 29, 30, 31, 32, 33, 32, 34, 35, 36, 37, 38, 17, 18, 17, 17, 17, 17, 17, 17,
                    /* 316 */ 17, 17, 19, 20, 21, 22, 23, 24, 39, 40, 41, 42, 43, 44, 32, 32, 45, 46, 32, 47, 48, 49, 48, 50, 48, 51,
                    /* 342 */ 51, 52, 53, 54, 51, 55, 32, 56, 32, 32, 57, 58, 59, 60, 32, 32, 61, 62, 32, 32, 63, 32, 64, 32, 32, 48,
                    /* 368 */ 65, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 67, 67, 48, 48, 48, 48, 48, 48, 48, 48, 48, 66, 48,
                    /* 394 */ 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 66, 66, 66, 66, 66, 66, 66, 66, 66, 66, 66,
                    /* 420 */ 66, 66, 66, 66, 66, 66, 67, 67, 67, 67, 67, 67, 67, 67, 67, 67, 67, 67, 67, 67, 48, 67, 67, 67, 67, 67,
                    /* 446 */ 67, 67, 67, 67, 67, 67, 67, 67, 67, 67, 67
            };

    private static final int[] MAP2 =
            {
                    /*  0 */ 57344, 63744, 64976, 65008, 65536, 63743, 64975, 65007, 65533, 1114111, 48, 67, 48, 67, 48
            };

    private static final int[] INITIAL =
            {
                    /*  0 */ 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29,
                    /* 29 */ 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56,
                    /* 56 */ 57, 58, 59, 60, 61, 62, 63, 64, 65, 66
            };

    private static final int[] TRANSITION =
            {
                    /*    0 */ 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216,
                    /*   17 */ 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2215, 2216,
                    /*   34 */ 2216, 2208, 2215, 3743, 2213, 2225, 2230, 2216, 2811, 3613, 2216, 2216, 2216, 2238, 2216, 2216, 2216,
                    /*   51 */ 2216, 3739, 2216, 2216, 3290, 3539, 2216, 3288, 2912, 2216, 2216, 2216, 2216, 2215, 2216, 2216, 2208,
                    /*   68 */ 2215, 3743, 2213, 2225, 2249, 3287, 2811, 3613, 2216, 2216, 2216, 2238, 2216, 2216, 2216, 2216, 3739,
                    /*   85 */ 2216, 2216, 3290, 3539, 2216, 3288, 2912, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216,
                    /*  102 */ 2216, 3335, 2688, 3287, 2811, 3130, 3488, 2216, 2216, 2238, 2216, 2216, 2216, 2216, 2216, 2216, 2216,
                    /*  119 */ 3290, 2914, 2216, 3288, 2912, 2216, 2216, 2216, 2216, 3224, 2257, 2216, 2216, 2552, 2955, 2670, 2490,
                    /*  136 */ 2928, 2216, 3753, 3613, 2216, 2216, 2216, 2238, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 3290, 2914,
                    /*  153 */ 2216, 3288, 2912, 2216, 2216, 2216, 2216, 2216, 2366, 2216, 2365, 2216, 2216, 2364, 2266, 2688, 3287,
                    /*  170 */ 2811, 3613, 3247, 2216, 2284, 2238, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 3290, 2914, 2216, 3288,
                    /*  187 */ 2912, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 3335, 2688, 3287, 2811, 3613,
                    /*  204 */ 2216, 2216, 2216, 2238, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 3290, 2914, 2216, 3288, 2912, 2216,
                    /*  221 */ 2216, 2216, 2216, 2216, 2296, 2216, 2216, 2297, 2216, 2216, 3335, 2305, 3287, 3257, 3613, 2216, 2216,
                    /*  238 */ 2216, 2238, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 3290, 2914, 2216, 3288, 2912, 2216, 2216, 2216,
                    /*  255 */ 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 3335, 3082, 2216, 3394, 3613, 2216, 2216, 2216, 2238,
                    /*  272 */ 2216, 2216, 2216, 2216, 2216, 2216, 2216, 3290, 2914, 2216, 3288, 2912, 2216, 2216, 2216, 2216, 3282,
                    /*  289 */ 2397, 2216, 2216, 2579, 2394, 2782, 2683, 3049, 2313, 3204, 3613, 2216, 2216, 2216, 2238, 2216, 2216,
                    /*  306 */ 2216, 2216, 2216, 2216, 2216, 3290, 2914, 2216, 3288, 2912, 2216, 2216, 2216, 2216, 2216, 2434, 2216,
                    /*  323 */ 2216, 2952, 2433, 2942, 2947, 2688, 3287, 2811, 3613, 2216, 2216, 2216, 2238, 2216, 2216, 2216, 2216,
                    /*  340 */ 2216, 2216, 2216, 3290, 2914, 2216, 3288, 2912, 2216, 2216, 2216, 2216, 2216, 2598, 2216, 2216, 2323,
                    /*  357 */ 2322, 2331, 2336, 2688, 3287, 2811, 3613, 2216, 2216, 2216, 2238, 2216, 2216, 2216, 2216, 2216, 2216,
                    /*  374 */ 2216, 3290, 2914, 2216, 3288, 2912, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216,
                    /*  391 */ 2973, 2688, 3287, 2344, 3613, 2216, 2216, 2216, 2238, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 3290,
                    /*  408 */ 2914, 2216, 3288, 2912, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 3384, 2688,
                    /*  425 */ 3287, 2811, 3613, 2216, 2216, 2216, 2238, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 3290, 2914, 2216,
                    /*  442 */ 3288, 2912, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 3323, 3322, 3322, 2352, 2688, 3287, 2811,
                    /*  459 */ 3613, 2216, 2216, 2216, 2238, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 3290, 2914, 2216, 3288, 2912,
                    /*  476 */ 2216, 2216, 2216, 2216, 2374, 2216, 2216, 2375, 2216, 2216, 2216, 3335, 2383, 2391, 2811, 3613, 2216,
                    /*  493 */ 2216, 2216, 2238, 2405, 2566, 2429, 3289, 2594, 2216, 2216, 3524, 3219, 2216, 2567, 2912, 2216, 2216,
                    /*  510 */ 2216, 2216, 2374, 2216, 2216, 2375, 2216, 2216, 2216, 3335, 2383, 2442, 2811, 3613, 2216, 2216, 2216,
                    /*  527 */ 2238, 2453, 2566, 2429, 2216, 2594, 2216, 2216, 3290, 3219, 2216, 3288, 2912, 2216, 2216, 2216, 2216,
                    /*  544 */ 2374, 2216, 2216, 2375, 2216, 2216, 2216, 3335, 2383, 2391, 2811, 3613, 2854, 2216, 2216, 2238, 2477,
                    /*  561 */ 2566, 2485, 2216, 2723, 2216, 2508, 3290, 3481, 3680, 3288, 2912, 2216, 2216, 2216, 2216, 2520, 2216,
                    /*  578 */ 2216, 2375, 2216, 2216, 2216, 3335, 2383, 2391, 2811, 3613, 2854, 2216, 2216, 2238, 2477, 2566, 2485,
                    /*  595 */ 2216, 2723, 2216, 2508, 3290, 3481, 3680, 3288, 2912, 2216, 2216, 2216, 2216, 2539, 2216, 2216, 2540,
                    /*  612 */ 2548, 3492, 3494, 3044, 2383, 3287, 2811, 3613, 2560, 2216, 2575, 2238, 2587, 2566, 2429, 2216, 2594,
                    /*  629 */ 2216, 2216, 3290, 3219, 2216, 3288, 2912, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216,
                    /*  646 */ 2216, 3335, 2688, 3287, 2811, 3613, 2216, 2216, 2216, 2238, 2606, 3193, 2616, 2216, 2608, 2216, 2617,
                    /*  663 */ 3290, 3187, 2811, 3288, 2912, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 3618,
                    /*  680 */ 2625, 2216, 2811, 3613, 2216, 2216, 2216, 2238, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 3290, 2914,
                    /*  697 */ 2216, 3288, 2912, 2216, 2216, 2216, 2216, 2216, 2643, 2216, 2646, 2216, 2216, 2216, 3335, 2688, 3287,
                    /*  714 */ 2811, 3613, 2216, 2216, 2216, 2238, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 3290, 2914, 2216, 3288,
                    /*  731 */ 2912, 2216, 2216, 2216, 2216, 2216, 3366, 2216, 3638, 3277, 3641, 2216, 2873, 2276, 3544, 2811, 3613,
                    /*  748 */ 2216, 2216, 2216, 2640, 2271, 2216, 2216, 2216, 2216, 2216, 2216, 3290, 2654, 2216, 3288, 3628, 2241,
                    /*  765 */ 2216, 2216, 2216, 2216, 2216, 2853, 2848, 2216, 2851, 2216, 3440, 2688, 3287, 2811, 3613, 2527, 2216,
                    /*  782 */ 2216, 2238, 2216, 2216, 2216, 2216, 2666, 2216, 2216, 3290, 2678, 2216, 3288, 3236, 2216, 2216, 2216,
                    /*  799 */ 2216, 2696, 2216, 2216, 2540, 2548, 3492, 2761, 3044, 2383, 2391, 2811, 3613, 2560, 2778, 2575, 2704,
                    /*  816 */ 2716, 2566, 2429, 3405, 2632, 2731, 2743, 2755, 3481, 3680, 3288, 3567, 2216, 2216, 2216, 2216, 2696,
                    /*  833 */ 2216, 2216, 2540, 2548, 3492, 3494, 3044, 2383, 2391, 2811, 3613, 2560, 3415, 2575, 2238, 2587, 2566,
                    /*  850 */ 2429, 2216, 2632, 2216, 2743, 3290, 3481, 3680, 3288, 2912, 2216, 2216, 2216, 2216, 2696, 2216, 2216,
                    /*  867 */ 2540, 2548, 3492, 3494, 3153, 2383, 2391, 2811, 3613, 2560, 2790, 2575, 2238, 2587, 2566, 2429, 2216,
                    /*  884 */ 2632, 2216, 2743, 3290, 3481, 3140, 3288, 2912, 2216, 2216, 2216, 2216, 2696, 2216, 2216, 2540, 2548,
                    /*  901 */ 3492, 3494, 3044, 2383, 2801, 2811, 3701, 2560, 2216, 2575, 2238, 2587, 2809, 2429, 2216, 2632, 2216,
                    /*  918 */ 2743, 3290, 3481, 2819, 2827, 2912, 2216, 3633, 2288, 2216, 2696, 2216, 2216, 2540, 2548, 3492, 2767,
                    /*  935 */ 2837, 2383, 2391, 2811, 3613, 2560, 2216, 2575, 2845, 2862, 2566, 2429, 2465, 2632, 2258, 2881, 2829,
                    /*  952 */ 3481, 2900, 3578, 3069, 2908, 2922, 2216, 2216, 2696, 2216, 2216, 2540, 2548, 3492, 3494, 3044, 2383,
                    /*  969 */ 2391, 2811, 2359, 2560, 2216, 2575, 2238, 2587, 2566, 2429, 2216, 2632, 2216, 2743, 2936, 3481, 3680,
                    /*  986 */ 3288, 2912, 2216, 2216, 2216, 2216, 2696, 2216, 2216, 2540, 2548, 3492, 3494, 3044, 2383, 2963, 3095,
                    /* 1003 */ 3613, 2560, 2216, 2575, 2238, 2587, 2566, 2429, 2216, 2594, 2216, 2216, 3290, 3219, 2216, 3288, 2912,
                    /* 1020 */ 2216, 2216, 2216, 2216, 2696, 2216, 2216, 2540, 2548, 3492, 3494, 3044, 2383, 2391, 2811, 3613, 2560,
                    /* 1037 */ 2216, 2575, 2238, 2587, 2566, 2429, 2216, 2594, 2216, 2216, 3290, 3219, 2216, 3288, 2912, 2216, 2216,
                    /* 1054 */ 2216, 2216, 2696, 2216, 2986, 2981, 2548, 3039, 3454, 3000, 2383, 2391, 2811, 2412, 2560, 2216, 3008,
                    /* 1071 */ 2238, 2587, 2566, 2429, 3015, 2594, 3588, 2216, 3290, 3219, 2216, 3026, 3034, 3194, 2216, 3313, 2216,
                    /* 1088 */ 2696, 2216, 2216, 2540, 2548, 3492, 3494, 3044, 2383, 2391, 2811, 3613, 2560, 2216, 2575, 2238, 2587,
                    /* 1105 */ 2566, 2429, 2216, 2594, 2216, 2216, 2892, 3219, 2216, 3288, 2912, 2216, 2216, 2216, 2216, 2696, 2216,
                    /* 1122 */ 2216, 2540, 2548, 3492, 3494, 3044, 2383, 2391, 2811, 3613, 2560, 2216, 2575, 2238, 2587, 3057, 2429,
                    /* 1139 */ 2216, 3065, 2216, 3711, 3290, 3219, 2216, 3288, 3077, 2216, 2216, 2216, 2216, 2696, 2216, 2216, 2540,
                    /* 1156 */ 2548, 3492, 3494, 3044, 2383, 2391, 2811, 3090, 3103, 2216, 3117, 2238, 2587, 2566, 2429, 2216, 2594,
                    /* 1173 */ 2216, 2216, 3290, 3219, 2512, 3288, 3125, 2216, 2216, 2216, 2216, 2696, 2216, 2417, 2540, 2548, 3148,
                    /* 1190 */ 3494, 3161, 2383, 3169, 3172, 3180, 2560, 3202, 2575, 2238, 3212, 2566, 2429, 2216, 3232, 2216, 2216,
                    /* 1207 */ 3290, 3219, 2216, 3288, 2912, 3135, 2216, 3244, 3255, 2696, 2216, 2216, 2540, 2548, 3492, 3494, 3044,
                    /* 1224 */ 2383, 2391, 2811, 3613, 3265, 2216, 2575, 2238, 2587, 3298, 2429, 2469, 3309, 2216, 2216, 3290, 3219,
                    /* 1241 */ 2216, 3288, 2912, 3662, 2216, 2216, 3321, 2696, 2216, 2216, 2540, 2548, 2658, 2773, 3044, 2383, 3331,
                    /* 1258 */ 2811, 3613, 2560, 2216, 3343, 2238, 2587, 2566, 3357, 2216, 2594, 2216, 2216, 3290, 3219, 2216, 3288,
                    /* 1275 */ 2912, 2216, 2216, 2216, 2216, 2696, 2216, 2216, 2540, 2548, 3492, 3494, 3044, 2383, 2391, 2811, 3613,
                    /* 1292 */ 2560, 2216, 2575, 2238, 2587, 2566, 2429, 3365, 2594, 2216, 2216, 3290, 3219, 2216, 3288, 2912, 2216,
                    /* 1309 */ 2216, 2216, 2216, 2696, 2216, 2216, 2540, 2548, 3492, 3494, 3044, 2383, 2391, 2811, 2460, 2560, 2216,
                    /* 1326 */ 2575, 3374, 2587, 2566, 2429, 2216, 2594, 3392, 2216, 3290, 3219, 2216, 3288, 2912, 3402, 2216, 2216,
                    /* 1343 */ 2216, 2696, 2216, 2216, 2540, 2548, 2531, 2887, 3044, 2383, 2391, 2811, 3613, 2560, 2217, 2575, 2238,
                    /* 1360 */ 2587, 2566, 2429, 2216, 2594, 2216, 2216, 3290, 3219, 2216, 3554, 2912, 2216, 2793, 3413, 2216, 2696,
                    /* 1377 */ 2216, 2216, 2540, 2548, 3492, 3494, 3044, 2383, 2391, 2811, 3613, 2560, 2216, 2575, 3423, 2587, 3431,
                    /* 1394 */ 3448, 3466, 2594, 2216, 2216, 3474, 3502, 2216, 3288, 2912, 2216, 3510, 2445, 2216, 2696, 2216, 2216,
                    /* 1411 */ 2540, 2548, 3492, 3494, 3044, 2383, 2391, 2708, 3613, 2560, 2216, 2575, 2238, 2587, 2566, 2429, 2216,
                    /* 1428 */ 2594, 3521, 2314, 3290, 3219, 2216, 3288, 2912, 2216, 2216, 2216, 2216, 2696, 2216, 2216, 2540, 2548,
                    /* 1445 */ 3492, 3494, 3044, 2383, 2391, 2811, 3613, 2560, 2216, 2575, 2238, 2587, 2566, 3532, 2216, 2594, 2216,
                    /* 1462 */ 2216, 3290, 3219, 2216, 3288, 2912, 2216, 2216, 2216, 2216, 2696, 2216, 2216, 2540, 2548, 3492, 3494,
                    /* 1479 */ 3044, 2383, 2391, 3018, 3613, 2560, 2216, 3562, 2238, 2587, 2566, 2429, 2216, 2594, 2216, 3549, 3290,
                    /* 1496 */ 3219, 2216, 3349, 2912, 2216, 2216, 2969, 2216, 2216, 2216, 2495, 2216, 2499, 2216, 2216, 3335, 2276,
                    /* 1513 */ 2216, 2811, 3613, 2216, 2216, 2216, 3575, 3586, 2216, 2216, 2216, 2216, 2216, 2216, 3290, 2914, 2216,
                    /* 1530 */ 3288, 2912, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 3335, 2276, 2216, 2811,
                    /* 1547 */ 3613, 2216, 2216, 2216, 2238, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 3290, 2914, 2216, 3288, 2912,
                    /* 1564 */ 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 3623, 3596, 2216, 2811, 3608, 2735,
                    /* 1581 */ 2216, 2216, 2238, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 3290, 2914, 2216, 3288, 2912, 2216, 2216,
                    /* 1598 */ 2216, 2216, 2539, 2216, 2216, 2540, 2548, 3492, 3494, 3044, 2383, 2391, 2811, 3613, 2560, 2216, 2575,
                    /* 1615 */ 2238, 2587, 2566, 2429, 2216, 2594, 2216, 2216, 3290, 3219, 2216, 3288, 2912, 2216, 2216, 2216, 2216,
                    /* 1632 */ 2696, 2216, 2216, 2540, 2548, 3492, 3494, 3044, 2383, 2391, 2811, 3613, 2560, 2216, 2575, 2238, 2587,
                    /* 1649 */ 2566, 2429, 2216, 2632, 2216, 2743, 3290, 3481, 3680, 3288, 2912, 2216, 2216, 2216, 2216, 2696, 2216,
                    /* 1666 */ 2216, 2540, 2548, 3492, 3494, 3044, 2383, 2391, 2811, 3613, 2560, 2747, 2575, 2238, 2587, 2566, 2429,
                    /* 1683 */ 2216, 2632, 2216, 2743, 3290, 3481, 3680, 3288, 2912, 2216, 2216, 2216, 2216, 2696, 2216, 2216, 2540,
                    /* 1700 */ 2548, 3492, 3494, 3044, 2383, 2391, 2811, 3613, 2560, 2216, 2575, 2238, 2587, 2566, 2429, 2216, 2632,
                    /* 1717 */ 2421, 2743, 3290, 3481, 3680, 3288, 2912, 2216, 2216, 2216, 2216, 2696, 2216, 2989, 2540, 2548, 2992,
                    /* 1734 */ 3494, 3044, 2383, 2391, 3651, 3613, 2560, 2216, 2575, 2238, 2587, 2566, 2429, 2216, 2632, 2216, 2743,
                    /* 1751 */ 3290, 3481, 3680, 3288, 2912, 2216, 2216, 2216, 2216, 2696, 2216, 2216, 2540, 2548, 3492, 3494, 3044,
                    /* 1768 */ 2383, 2391, 2811, 3613, 2560, 2216, 2575, 2238, 2587, 2566, 2429, 2216, 2594, 2216, 2216, 3290, 3219,
                    /* 1785 */ 2216, 3288, 2912, 2216, 3380, 2216, 2216, 2696, 2216, 2216, 2540, 2548, 3492, 3494, 3044, 2383, 2391,
                    /* 1802 */ 2811, 3613, 2560, 2216, 2575, 2238, 2587, 2566, 2429, 2216, 2594, 3513, 2216, 3290, 3219, 3659, 3288,
                    /* 1819 */ 2912, 2216, 2216, 2216, 2216, 2696, 2216, 2216, 2540, 2548, 3492, 3494, 3044, 2383, 2391, 2811, 3613,
                    /* 1836 */ 2560, 2216, 2575, 2238, 2587, 2566, 2429, 2216, 2594, 2216, 2216, 3290, 3603, 2216, 3288, 2912, 2216,
                    /* 1853 */ 2216, 2216, 2216, 2696, 2216, 2216, 2540, 2548, 3492, 3494, 3044, 2383, 2391, 2811, 3613, 2560, 2216,
                    /* 1870 */ 2575, 2238, 2587, 2566, 2429, 2216, 2869, 2216, 2216, 3290, 3219, 2216, 3288, 2912, 2216, 2216, 2216,
                    /* 1887 */ 2216, 2696, 2216, 2216, 2540, 2548, 3492, 3494, 3044, 2383, 2391, 3670, 3613, 2560, 2216, 2575, 2238,
                    /* 1904 */ 2587, 2566, 2429, 2216, 2594, 2216, 2216, 3290, 3219, 2216, 3288, 3458, 3678, 2216, 2216, 2216, 2696,
                    /* 1921 */ 2216, 2216, 2540, 2548, 3492, 3494, 3044, 2383, 2391, 2811, 3613, 2560, 2216, 2575, 2238, 2587, 3271,
                    /* 1938 */ 2429, 2216, 2594, 2216, 2216, 3290, 3219, 3436, 3288, 2912, 2216, 2216, 2216, 2216, 2696, 2216, 2216,
                    /* 1955 */ 2540, 2548, 3492, 3494, 3044, 2383, 2391, 2811, 3613, 2560, 3301, 2575, 2238, 2587, 2566, 2429, 2216,
                    /* 1972 */ 2594, 2216, 2216, 3290, 3219, 2216, 3288, 2912, 2216, 2216, 2216, 2216, 2696, 2216, 2216, 2540, 2548,
                    /* 1989 */ 3492, 3494, 3044, 2383, 2391, 2811, 3613, 2560, 2216, 2575, 2238, 2587, 3109, 2429, 2216, 2594, 2216,
                    /* 2006 */ 2216, 3290, 3219, 2216, 3288, 2912, 2216, 2216, 2216, 2216, 2696, 2216, 3643, 2540, 2548, 3492, 3494,
                    /* 2023 */ 3044, 2383, 2391, 2811, 3613, 2560, 2216, 2575, 2238, 2587, 2566, 2429, 2216, 2594, 2216, 2216, 3290,
                    /* 2040 */ 3219, 2216, 3288, 2912, 2216, 2216, 2216, 2216, 2696, 2216, 2216, 2540, 2548, 3492, 3494, 3044, 2383,
                    /* 2057 */ 2391, 2811, 3613, 2560, 2216, 2575, 2238, 3688, 2566, 3696, 2216, 2594, 2216, 2216, 3290, 3219, 2216,
                    /* 2074 */ 3288, 2912, 2216, 2216, 2216, 2216, 2216, 2216, 2500, 2216, 2500, 3709, 3719, 3724, 2276, 2216, 2811,
                    /* 2091 */ 3613, 2216, 2216, 2216, 2238, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 3290, 2914, 2216, 3288, 2912,
                    /* 2108 */ 2216, 2216, 2216, 2216, 2374, 2216, 2216, 2375, 2216, 2216, 2216, 3335, 3732, 2216, 2811, 3613, 2216,
                    /* 2125 */ 2216, 2216, 2238, 2453, 2566, 2429, 2216, 2594, 2216, 2216, 3290, 3219, 2216, 3288, 2912, 2216, 2216,
                    /* 2142 */ 2216, 2216, 2539, 2216, 2216, 2540, 2548, 3492, 3494, 3044, 3732, 2216, 2811, 3613, 2560, 2216, 2575,
                    /* 2159 */ 2238, 2587, 2566, 2429, 2216, 2594, 2216, 2216, 3290, 3219, 2216, 3288, 2912, 2216, 2216, 2216, 2216,
                    /* 2176 */ 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 3751, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216,
                    /* 2193 */ 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 2216, 1537, 1537,
                    /* 2210 */ 1537, 1537, 1537, 1537, 1537, 1537, 0, 0, 0, 0, 0, 0, 0, 0, 145, 1537, 1537, 1537, 0, 512, 1537, 1537, 0,
                    /* 2233 */ 68, 69, 70, 71, 0, 0, 1024, 1024, 0, 0, 0, 0, 0, 0, 3328, 0, 1537, 1537, 0, 68, 69, 70, 71, 72, 4608, 0,
                    /* 2259 */ 0, 0, 0, 0, 0, 0, 206, 76, 0, 0, 91, 512, 0, 0, 0, 0, 14848, 0, 0, 0, 68, 69, 70, 71, 0, 0, 147, 0, 149,
                    /* 2288 */ 0, 0, 0, 0, 0, 5632, 0, 0, 0, 0, 5888, 0, 0, 0, 0, 0, 0, 98, 98, 0, 68, 69, 70, 71, 72, 3072, 0, 0, 0, 0,
                    /* 2318 */ 0, 0, 0, 211, 83, 0, 0, 0, 0, 0, 0, 6656, 6656, 6656, 83, 0, 0, 0, 0, 6739, 6656, 0, 512, 0, 6656, 0, 0,
                    /* 2345 */ 0, 6912, 0, 0, 0, 87, 88, 0, 0, 7680, 0, 512, 0, 7680, 0, 0, 119, 768, 768, 0, 0, 0, 76, 0, 0, 0, 0, 0,
                    /* 2373 */ 0, 0, 0, 2051, 0, 0, 0, 0, 0, 0, 0, 0, 1859, 68, 69, 70, 71, 72, 73, 0, 4427, 0, 0, 0, 0, 0, 0, 6144, 0,
                    /* 2402 */ 0, 0, 0, 129, 159, 0, 0, 0, 0, 135, 0, 0, 120, 768, 768, 0, 0, 0, 80, 0, 0, 0, 0, 203, 0, 0, 0, 0, 146,
                    /* 2431 */ 0, 148, 0, 0, 0, 0, 0, 6400, 0, 0, 0, 73, 104, 4427, 0, 0, 0, 0, 0, 0, 7936, 8192, 129, 0, 0, 0, 0, 0,
                    /* 2459 */ 135, 0, 0, 121, 768, 768, 0, 0, 0, 187, 0, 0, 0, 0, 188, 0, 0, 0, 129, 0, 0, 0, 0, 165, 135, 167, 0, 146,
                    /* 2487 */ 177, 148, 179, 0, 0, 0, 68, 512, 0, 0, 0, 0, 14592, 0, 0, 0, 0, 0, 0, 0, 15616, 177, 207, 179, 208, 0, 0,
                    /* 2514 */ 0, 0, 0, 14336, 0, 0, 0, 0, 2051, 0, 0, 0, 74, 0, 0, 131, 132, 0, 0, 0, 0, 86, 1859, 0, 0, 0, 1859, 2051,
                    /* 2542 */ 0, 0, 0, 0, 0, 0, 0, 1859, 1859, 1859, 0, 0, 0, 0, 70, 4608, 0, 0, 0, 129, 0, 0, 0, 135, 137, 0, 0, 0, 0,
                    /* 2571 */ 0, 0, 0, 231, 146, 146, 148, 148, 0, 0, 0, 0, 71, 6144, 0, 0, 129, 0, 163, 163, 0, 0, 135, 0, 0, 163,
                    /* 2597 */ 163, 0, 0, 0, 0, 0, 6656, 0, 0, 4096, 0, 0, 0, 0, 0, 68, 0, 69, 0, 0, 87, 0, 88, 0, 0, 0, 0, 0, 99, 100,
                    /* 2627 */ 0, 0, 0, 70, 71, 0, 0, 163, 163, 0, 198, 0, 199, 0, 1024, 14848, 0, 0, 0, 0, 0, 0, 9472, 0, 0, 0, 0, 0,
                    /* 2655 */ 0, 195, 3584, 0, 0, 0, 0, 85, 1859, 0, 0, 0, 0, 196, 196, 0, 0, 0, 0, 87, 0, 0, 0, 0, 0, 221, 0, 196, 0,
                    /* 2684 */ 0, 0, 69, 512, 0, 0, 0, 68, 69, 70, 71, 72, 0, 1859, 2051, 0, 0, 0, 0, 4427, 0, 1024, 1024, 154, 0, 0, 0,
                    /* 2711 */ 0, 111, 0, 87, 88, 129, 160, 163, 163, 0, 0, 135, 0, 0, 163, 163, 165, 198, 167, 199, 200, 0, 0, 13056,
                    /* 2735 */ 0, 0, 0, 0, 133, 0, 0, 0, 0, 207, 0, 208, 0, 0, 0, 0, 142, 0, 0, 0, 10752, 0, 0, 0, 0, 190, 0, 0, 0,
                    /* 2764 */ 1859, 0, 89, 0, 0, 0, 1859, 0, 90, 0, 0, 0, 1859, 85, 0, 0, 0, 141, 0, 0, 0, 0, 88, 0, 0, 0, 138, 0, 140,
                    /* 2793 */ 0, 0, 0, 0, 0, 0, 11264, 13568, 73, 0, 4427, 0, 0, 0, 0, 108, 137, 169, 0, 0, 0, 0, 0, 0, 87, 88, 0, 223,
                    /* 2821 */ 0, 0, 0, 0, 207, 208, 4864, 0, 0, 0, 0, 0, 0, 190, 0, 218, 1859, 0, 0, 0, 512, 0, 0, 95, 153, 1024, 1024,
                    /* 2848 */ 0, 0, 0, 0, 0, 77, 0, 0, 0, 0, 0, 0, 0, 3944, 129, 161, 163, 163, 0, 0, 135, 0, 0, 163, 197, 0, 0, 0, 0,
                    /* 2877 */ 512, 9728, 0, 0, 0, 207, 0, 208, 0, 209, 0, 0, 0, 1859, 86, 0, 0, 0, 214, 0, 190, 0, 0, 12544, 0, 0, 0,
                    /* 2904 */ 0, 0, 207, 208, 0, 237, 0, 239, 0, 0, 0, 0, 195, 0, 0, 0, 0, 0, 243, 0, 0, 0, 0, 246, 0, 0, 0, 2560, 69,
                    /* 2933 */ 2816, 71, 3072, 0, 0, 12245, 0, 0, 190, 0, 0, 0, 6400, 0, 6400, 0, 0, 0, 512, 0, 0, 6400, 0, 0, 0, 0, 0,
                    /* 2960 */ 0, 4608, 0, 73, 0, 4427, 0, 0, 106, 0, 0, 0, 8448, 0, 0, 0, 0, 512, 7168, 7168, 0, 1859, 2051, 0, 0, 0,
                    /* 2986 */ 0, 78, 79, 0, 0, 0, 0, 0, 81, 0, 0, 1859, 0, 0, 1859, 0, 0, 0, 512, 0, 0, 96, 146, 146, 148, 148, 0, 0,
                    /* 3014 */ 151, 0, 0, 186, 0, 0, 0, 0, 0, 112, 87, 88, 0, 226, 227, 228, 0, 0, 230, 190, 0, 0, 234, 0, 195, 0, 0, 0,
                    /* 3042 */ 84, 0, 1859, 0, 0, 0, 512, 0, 0, 0, 68, 2560, 70, 2816, 72, 137, 0, 0, 171, 0, 0, 0, 175, 192, 0, 163,
                    /* 3068 */ 163, 0, 0, 0, 0, 195, 0, 12800, 0, 232, 0, 0, 0, 195, 0, 0, 0, 102, 103, 70, 71, 0, 0, 118, 0, 768, 768,
                    /* 3095 */ 0, 0, 0, 106, 0, 0, 87, 88, 127, 129, 0, 0, 0, 135, 137, 0, 0, 0, 0, 0, 174, 0, 146, 146, 148, 148, 0, 0,
                    /* 3123 */ 0, 152, 0, 233, 0, 0, 195, 0, 0, 0, 122, 768, 0, 0, 0, 0, 13552, 0, 0, 0, 0, 14080, 0, 207, 208, 0, 80,
                    /* 3150 */ 0, 0, 0, 1859, 0, 0, 0, 512, 0, 0, 94, 1859, 0, 0, 0, 512, 0, 0, 97, 73, 0, 4427, 0, 0, 0, 107, 0, 0, 87,
                    /* 3179 */ 88, 117, 0, 0, 768, 768, 0, 125, 0, 0, 195, 0, 0, 68, 69, 0, 0, 0, 0, 0, 0, 0, 242, 0, 139, 0, 0, 0, 0,
                    /* 3208 */ 0, 0, 87, 2304, 129, 162, 163, 163, 0, 0, 135, 0, 0, 195, 0, 163, 0, 0, 0, 68, 70, 72, 0, 0, 193, 0, 163,
                    /* 3235 */ 163, 0, 0, 0, 0, 221, 0, 0, 0, 13824, 0, 248, 0, 0, 0, 0, 0, 134, 136, 0, 0, 8704, 0, 0, 0, 0, 0, 0, 113,
                    /* 3264 */ 115, 128, 129, 0, 0, 0, 135, 137, 0, 0, 0, 0, 173, 0, 0, 0, 9728, 9728, 0, 0, 0, 69, 71, 73, 0, 0, 0, 0,
                    /* 3292 */ 0, 0, 0, 190, 0, 0, 137, 0, 170, 0, 0, 0, 0, 0, 143, 0, 0, 0, 194, 163, 163, 0, 0, 0, 0, 249, 0, 0, 0,
                    /* 3321 */ 250, 0, 0, 0, 0, 0, 0, 0, 7680, 0, 73, 0, 4427, 105, 0, 0, 0, 0, 512, 0, 0, 0, 146, 146, 148, 148, 0,
                    /* 3348 */ 150, 0, 0, 0, 11520, 0, 0, 0, 190, 0, 146, 0, 148, 0, 0, 0, 183, 184, 0, 0, 0, 0, 0, 0, 0, 9728, 0, 1024,
                    /* 3376 */ 1024, 0, 0, 156, 0, 0, 0, 15104, 0, 0, 0, 0, 512, 7424, 7424, 0, 0, 201, 0, 0, 0, 0, 0, 0, 114, 116, 0,
                    /* 3403 */ 0, 238, 0, 0, 0, 0, 0, 189, 0, 0, 0, 247, 0, 0, 0, 0, 0, 0, 144, 0, 0, 1024, 1024, 0, 155, 0, 157, 158,
                    /* 3431 */ 137, 0, 0, 0, 172, 0, 0, 0, 225, 0, 0, 0, 0, 512, 9984, 9984, 0, 176, 146, 0, 148, 0, 181, 0, 0, 84,
                    /* 3457 */ 1859, 0, 0, 0, 0, 195, 0, 0, 236, 0, 185, 0, 0, 0, 0, 0, 191, 0, 212, 0, 0, 215, 190, 217, 0, 0, 195, 0,
                    /* 3485 */ 163, 198, 199, 0, 0, 130, 130, 0, 0, 0, 0, 0, 1859, 0, 0, 0, 0, 219, 220, 195, 0, 163, 0, 0, 222, 0, 244,
                    /* 3512 */ 245, 0, 0, 0, 0, 0, 204, 0, 0, 0, 0, 202, 0, 0, 0, 0, 0, 216, 0, 0, 0, 146, 0, 148, 0, 0, 182, 0, 0, 195,
                    /* 3542 */ 0, 195, 0, 0, 0, 0, 10240, 0, 0, 0, 0, 11008, 0, 0, 0, 0, 12288, 0, 0, 190, 146, 146, 148, 148, 10496, 0,
                    /* 3568 */ 0, 0, 235, 195, 5376, 0, 0, 0, 8960, 1024, 0, 0, 0, 0, 0, 229, 0, 190, 0, 8960, 0, 0, 0, 0, 0, 0, 205, 0,
                    /* 3596 */ 0, 101, 0, 68, 69, 70, 71, 0, 0, 195, 0, 9379, 0, 0, 0, 768, 123, 0, 0, 0, 768, 768, 0, 0, 0, 0, 604, 0,
                    /* 3624 */ 0, 0, 0, 605, 0, 0, 0, 0, 3584, 0, 0, 0, 0, 5120, 0, 0, 0, 0, 9728, 0, 0, 0, 0, 0, 0, 82, 0, 0, 110, 0,
                    /* 3654 */ 0, 0, 0, 87, 88, 0, 0, 224, 0, 0, 0, 0, 0, 241, 0, 0, 109, 0, 0, 0, 0, 0, 87, 88, 15360, 0, 0, 0, 0, 0,
                    /* 3684 */ 0, 0, 207, 208, 129, 0, 164, 163, 0, 166, 135, 168, 0, 146, 178, 148, 180, 0, 0, 0, 768, 768, 124, 0,
                    /* 3708 */ 11902, 15616, 0, 0, 0, 0, 0, 0, 0, 210, 0, 15616, 15616, 0, 0, 0, 0, 15616, 15616, 0, 512, 0, 15616, 0,
                    /* 3732 */ 0, 0, 1859, 68, 69, 70, 71, 0, 0, 195, 195, 0, 0, 0, 0, 0, 1537, 1537, 1537, 1280, 1280, 0, 0, 0, 0, 0,
                    /* 3758 */ 0, 2304, 88
            };

    private static final int[] EXPECTED =
            {
                    /*   0 */ 289, 294, 125, 152, 355, 355, 283, 286, 129, 249, 354, 133, 137, 141, 145, 149, 159, 292, 295, 355, 321,
                    /*  21 */ 195, 310, 313, 163, 167, 177, 355, 197, 263, 347, 355, 171, 183, 176, 355, 196, 188, 267, 205, 213, 184,
                    /*  42 */ 177, 355, 197, 264, 355, 206, 256, 174, 355, 195, 265, 204, 202, 216, 192, 205, 203, 266, 201, 210, 355,
                    /*  63 */ 355, 355, 355, 239, 222, 231, 238, 243, 253, 340, 260, 339, 273, 277, 280, 299, 355, 355, 324, 328, 303,
                    /*  84 */ 246, 366, 307, 318, 154, 328, 227, 246, 314, 368, 372, 349, 355, 327, 226, 268, 178, 370, 357, 234, 154,
                    /* 105 */ 328, 227, 269, 179, 371, 357, 233, 155, 225, 355, 332, 267, 359, 336, 344, 356, 353, 363, 358, 218,
                    /* 125 */ 131072, 1048576, 4194304, 8388608, 32, 4194368, 16777280, 64, 0, 16777312, 8519712, 570425376, 33554464,
                    /* 138 */ 67108896, 32, 17825856, 256, 16777216, 100663296, 570425344, 17825888, 100663328, 570425376, 2884096, 14,
                    /* 150 */ 402653216, 973078560, 16777216, 33554432, 0, 0, 0, 65536, 131072, -1073696720, -1073696720, 64, 512, 0,
                    /* 164 */ 32768, -1073729536, -1073729536, 0, 512, 512, 16384, 32768, -1073737728, 8192, 8192, 512, 512, 1048576, 0,
                    /* 179 */ 0, 0, 512, 6144, 0, 512, 512, 512, 512, 256, 0, 262144, 524288, 0, 524288, 2097152, 0, 0, 256, 256, 256,
                    /* 200 */ 256, 0, 1073741824, -2147483648, 0, 0, 0, 4096, 1073741824, -2147483648, 0, 2097152, 1073741824,
                    /* 213 */ -2147483648, 0, 8192, 8192, 1048576, 0, 0, 2, 2, 131072, 262144, 16777216, 67108864, 134217728, 4194304,
                    /* 228 */ 8388608, 0, 0, 134217728, 268435456, 0, 0, 8, 0, 0, 0, 16, 32, 128, 65536, 65536, 131072, 16777216, 0, 0,
                    /* 248 */ 256, 1024, 8519680, 570425344, 33554432, 0, 32, 32, 0, 0, 8192, 8192, 67108992, 196608, 12582912, 0, 0,
                    /* 265 */ 262144, 524288, 2097152, 0, 0, 0, 1024, 0, 196608, 0, 12582912, 1280, 268435456, 268435456, 0, 268435456,
                    /* 281 */ 0, 33554436, 96, 160, 1048608, 32, 32, 32, 32, 64, 128, 512, 1024, 1024, 2048, 16384, 65536, 1048576,
                    /* 299 */ 268435520, 3734016, 15, 33554439, 0, 196608, 4194304, 8388608, 6144, 57344, 3670016, 0, 0, 2883584, 12, 0,
                    /* 315 */ 0, 0, 4, 15, 7, 33554432, 0, 0, 67108864, 0, 0, 128, 65536, 131072, 262144, 67108864, 134217728, 0, 6144,
                    /* 334 */ 32768, 1572864, 131072, 67108864, 134217728, 0, 0, 268435456, 268435456, 262176, 0, 2048, 1572864,
                    /* 347 */ 2097152, 8, 8, 0, 33554432, 0, 2, 67108864, 0, 0, 0, 0, 1, 2, 0, 0, 2048, 1048576, 2097152, 0, 4,
                    /* 368 */ 33554432, 512, 6144, 49152, 1572864, 2097152, 0, 7
            };

    private static final String[] TOKEN =
            {
                    "%ERROR",
                    "NotOne",
                    "NotTwo",
                    "NotThree",
                    "EOF",
                    "S",
                    "Name",
                    "Nmtoken",
                    "EntityValue",
                    "AttValue",
                    "SystemLiteral",
                    "PubidLiteral",
                    "Comment",
                    "PI",
                    "VersionNum",
                    "PEReference",
                    "EncName",
                    "'\"'",
                    "'#FIXED'",
                    "'#IMPLIED'",
                    "'#PCDATA'",
                    "'#REQUIRED'",
                    "'%'",
                    "''''",
                    "'('",
                    "')'",
                    "')*'",
                    "'*'",
                    "'+'",
                    "','",
                    "'<!ATTLIST'",
                    "'<!ELEMENT'",
                    "'<!ENTITY'",
                    "'<!NOTATION'",
                    "'<!['",
                    "'<?xml'",
                    "'='",
                    "'>'",
                    "'?'",
                    "'?>'",
                    "'ANY'",
                    "'CDATA'",
                    "'EMPTY'",
                    "'ENTITIES'",
                    "'ENTITY'",
                    "'ID'",
                    "'IDREF'",
                    "'IDREFS'",
                    "'IGNORE'",
                    "'INCLUDE'",
                    "'NDATA'",
                    "'NMTOKEN'",
                    "'NMTOKENS'",
                    "'NOTATION'",
                    "'PUBLIC'",
                    "'SYSTEM'",
                    "'['",
                    "']]>'",
                    "'encoding'",
                    "'version'",
                    "'|'"
            };
}

// End
