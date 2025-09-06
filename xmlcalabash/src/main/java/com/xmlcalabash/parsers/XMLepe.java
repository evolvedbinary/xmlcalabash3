package com.xmlcalabash.parsers;

// This file was generated on Sat Aug 30, 2025 11:41 (UTC+01) by REx v6.2-SNAPSHOT which is Copyright (c) 1979-2025 by Gunther Rademacher <grd@gmx.net>
// REx command line: -faster -java -tree XMLepe.ebnf

import java.util.Arrays;

public class XMLepe
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

    public XMLepe(CharSequence string, EventHandler t)
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

    public void parse_extParsedEnt()
    {
        eventHandler.startNonterminal("extParsedEnt", e0);
        lookahead1(24);                 // EOF | CharData | Comment | PI | CDSect | CharRef | '&' | '<' | '<?xml'
        if (l1 == 19)                   // '<?xml'
        {
            parse_TextDecl();
        }
        parse_content();
        consume(1);                     // EOF
        eventHandler.endNonterminal("extParsedEnt", e0);
    }

    private void parse_TextDecl()
    {
        eventHandler.startNonterminal("TextDecl", e0);
        consume(19);                    // '<?xml'
        parse_VersionInfo();
        lookahead1(15);                 // S | '?>'
        switch (l1)
        {
            case 2:                         // S
                lookahead2(18);               // '?>' | 'encoding'
                break;
            default:
                lk = l1;
        }
        if (lk == 738)                  // S 'encoding'
        {
            parse_EncodingDecl();
        }
        lookahead1(15);                 // S | '?>'
        if (l1 == 2)                    // S
        {
            consume(2);                   // S
        }
        lookahead1(10);                 // '?>'
        consume(22);                    // '?>'
        eventHandler.endNonterminal("TextDecl", e0);
    }

    private void parse_VersionInfo()
    {
        eventHandler.startNonterminal("VersionInfo", e0);
        lookahead1(0);                  // S
        consume(2);                     // S
        lookahead1(12);                 // 'version'
        consume(24);                    // 'version'
        parse_Eq();
        lookahead1(16);                 // '"' | "'"
        switch (l1)
        {
            case 14:                        // "'"
                consume(14);                  // "'"
                lookahead1(3);                // VersionNum
                consume(9);                   // VersionNum
                lookahead1(6);                // "'"
                consume(14);                  // "'"
                break;
            default:
                consume(12);                  // '"'
                lookahead1(3);                // VersionNum
                consume(9);                   // VersionNum
                lookahead1(5);                // '"'
                consume(12);                  // '"'
        }
        eventHandler.endNonterminal("VersionInfo", e0);
    }

    private void parse_Eq()
    {
        eventHandler.startNonterminal("Eq", e0);
        lookahead1(13);                 // S | '='
        if (l1 == 2)                    // S
        {
            consume(2);                   // S
        }
        lookahead1(8);                  // '='
        consume(20);                    // '='
        lookahead1(21);                 // S | AttValue | '"' | "'"
        if (l1 == 2)                    // S
        {
            consume(2);                   // S
        }
        eventHandler.endNonterminal("Eq", e0);
    }

    private void parse_element()
    {
        eventHandler.startNonterminal("element", e0);
        consume(17);                    // '<'
        lookahead1(1);                  // Name
        consume(3);                     // Name
        for (;;)
        {
            lookahead1(19);               // S | '/>' | '>'
            switch (l1)
            {
                case 2:                       // S
                    lookahead2(20);             // Name | '/>' | '>'
                    break;
                default:
                    lk = l1;
            }
            if (lk != 98)                 // S Name
            {
                break;
            }
            consume(2);                   // S
            parse_Attribute();
        }
        if (l1 == 2)                    // S
        {
            consume(2);                   // S
        }
        lookahead1(17);                 // '/>' | '>'
        switch (l1)
        {
            case 15:                        // '/>'
                consume(15);                  // '/>'
                break;
            default:
                consume(21);                  // '>'
                parse_content();
                parse_ETag();
        }
        eventHandler.endNonterminal("element", e0);
    }

    private void parse_Attribute()
    {
        eventHandler.startNonterminal("Attribute", e0);
        lookahead1(1);                  // Name
        consume(3);                     // Name
        parse_Eq();
        lookahead1(2);                  // AttValue
        consume(4);                     // AttValue
        eventHandler.endNonterminal("Attribute", e0);
    }

    private void parse_ETag()
    {
        eventHandler.startNonterminal("ETag", e0);
        consume(18);                    // '</'
        lookahead1(1);                  // Name
        consume(3);                     // Name
        lookahead1(14);                 // S | '>'
        if (l1 == 2)                    // S
        {
            consume(2);                   // S
        }
        lookahead1(9);                  // '>'
        consume(21);                    // '>'
        eventHandler.endNonterminal("ETag", e0);
    }

    private void parse_content()
    {
        eventHandler.startNonterminal("content", e0);
        lookahead1(23);                 // EOF | CharData | Comment | PI | CDSect | CharRef | '&' | '<' | '</'
        if (l1 == 5)                    // CharData
        {
            consume(5);                   // CharData
        }
        for (;;)
        {
            lookahead1(22);               // EOF | Comment | PI | CDSect | CharRef | '&' | '<' | '</'
            if (l1 == 1                   // EOF
                    || l1 == 18)                 // '</'
            {
                break;
            }
            switch (l1)
            {
                case 17:                      // '<'
                    parse_element();
                    break;
                case 8:                       // CDSect
                    consume(8);                 // CDSect
                    break;
                case 7:                       // PI
                    consume(7);                 // PI
                    break;
                case 6:                       // Comment
                    consume(6);                 // Comment
                    break;
                default:
                    parse_Reference();
            }
            lookahead1(23);               // EOF | CharData | Comment | PI | CDSect | CharRef | '&' | '<' | '</'
            if (l1 == 5)                  // CharData
            {
                consume(5);                 // CharData
            }
        }
        eventHandler.endNonterminal("content", e0);
    }

    private void parse_Reference()
    {
        eventHandler.startNonterminal("Reference", e0);
        switch (l1)
        {
            case 13:                        // '&'
                parse_EntityRef();
                break;
            default:
                consume(10);                  // CharRef
        }
        eventHandler.endNonterminal("Reference", e0);
    }

    private void parse_EntityRef()
    {
        eventHandler.startNonterminal("EntityRef", e0);
        consume(13);                    // '&'
        lookahead1(1);                  // Name
        consume(3);                     // Name
        lookahead1(7);                  // ';'
        consume(16);                    // ';'
        eventHandler.endNonterminal("EntityRef", e0);
    }

    private void parse_EncodingDecl()
    {
        eventHandler.startNonterminal("EncodingDecl", e0);
        consume(2);                     // S
        lookahead1(11);                 // 'encoding'
        consume(23);                    // 'encoding'
        parse_Eq();
        lookahead1(16);                 // '"' | "'"
        switch (l1)
        {
            case 12:                        // '"'
                consume(12);                  // '"'
                lookahead1(4);                // EncName
                consume(11);                  // EncName
                lookahead1(5);                // '"'
                consume(12);                  // '"'
                break;
            default:
                consume(14);                  // "'"
                lookahead1(4);                // EncName
                consume(11);                  // EncName
                lookahead1(6);                // "'"
                consume(14);                  // "'"
        }
        eventHandler.endNonterminal("EncodingDecl", e0);
    }

    private void consume(int t)
    {
        if (l1 == t)
        {
            eventHandler.terminal(TOKEN[l1], b1, e1);
            b0 = b1; e0 = e1; l1 = l2; if (l1 != 0) {
            b1 = b2; e1 = e2; l2 = 0; }
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
        lk = (l2 << 5) | l1;
    }

    private int error(int b, int e, int s, int l, int t)
    {
        throw new ParseException(b, e, s, l, t);
    }

    private int lk, b0, e0;
    private int l1, b1, e1;
    private int l2, b2, e2;
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

        for (int code = result & 127; code != 0; )
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
            int i0 = (charclass << 7) + code - 1;
            code = TRANSITION[(i0 & 7) + TRANSITION[i0 >> 3]];

            if (code > 127)
            {
                result = code;
                code &= 127;
                end = current;
            }
        }

        result >>= 7;
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
        return (result & 31) - 1;
    }

    private static String[] getTokenSet(int tokenSetId)
    {
        java.util.ArrayList<String> expected = new java.util.ArrayList<>();
        int s = tokenSetId < 0 ? - tokenSetId : INITIAL[tokenSetId] & 127;
        for (int i = 0; i < 25; i += 32)
        {
            int j = i;
            int i0 = (i >> 5) * 90 + s - 1;
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
                    /*   0 */ 42, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 3,
                    /*  35 */ 4, 5, 5, 6, 7, 5, 5, 5, 5, 5, 8, 9, 10, 11, 12, 11, 11, 11, 11, 11, 11, 11, 11, 13, 14, 15, 16, 17, 18, 5,
                    /*  65 */ 19, 20, 21, 22, 20, 20, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 24, 23, 23, 23, 23, 23, 23,
                    /*  91 */ 25, 5, 26, 5, 27, 5, 20, 20, 28, 29, 30, 20, 31, 23, 32, 23, 23, 33, 34, 35, 36, 23, 23, 37, 38, 23, 23,
                    /* 118 */ 39, 23, 40, 23, 23, 5, 5, 5, 5, 5
            };

    private static final int[] MAP1 =
            {
                    /*   0 */ 108, 124, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 156, 181, 181, 181, 181,
                    /*  21 */ 181, 214, 215, 213, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214,
                    /*  42 */ 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214,
                    /*  63 */ 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214,
                    /*  84 */ 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214,
                    /* 105 */ 214, 214, 214, 247, 261, 277, 293, 309, 321, 337, 353, 388, 388, 388, 380, 436, 428, 436, 428, 436, 436,
                    /* 126 */ 436, 436, 436, 436, 436, 436, 436, 436, 436, 436, 436, 436, 436, 436, 405, 405, 405, 405, 405, 405, 405,
                    /* 147 */ 421, 436, 436, 436, 436, 436, 436, 436, 436, 364, 388, 388, 389, 387, 388, 388, 436, 436, 436, 436, 436,
                    /* 168 */ 436, 436, 436, 436, 436, 436, 436, 436, 436, 436, 436, 436, 436, 388, 388, 388, 388, 388, 388, 388, 388,
                    /* 189 */ 388, 388, 388, 388, 388, 388, 388, 388, 388, 388, 388, 388, 388, 388, 388, 388, 388, 388, 388, 388, 388,
                    /* 210 */ 388, 388, 388, 435, 436, 436, 436, 436, 436, 436, 436, 436, 436, 436, 436, 436, 436, 436, 436, 436, 436,
                    /* 231 */ 436, 436, 436, 436, 436, 436, 436, 436, 436, 436, 436, 436, 436, 436, 436, 388, 42, 0, 0, 0, 0, 0, 0, 0,
                    /* 255 */ 0, 1, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4, 5, 5, 6, 7, 5, 5, 5, 5, 5,
                    /* 290 */ 8, 9, 10, 11, 12, 11, 11, 11, 11, 11, 11, 11, 11, 13, 14, 15, 16, 17, 18, 5, 19, 20, 21, 22, 20, 20, 23,
                    /* 317 */ 23, 23, 23, 23, 23, 23, 23, 23, 24, 23, 23, 23, 23, 23, 23, 25, 5, 26, 5, 27, 5, 20, 20, 28, 29, 30, 20,
                    /* 344 */ 31, 23, 32, 23, 23, 33, 34, 35, 36, 23, 23, 37, 38, 23, 23, 39, 23, 40, 23, 23, 5, 5, 5, 5, 5, 5, 5, 5, 5,
                    /* 373 */ 5, 5, 5, 13, 13, 5, 5, 5, 5, 5, 5, 5, 5, 5, 41, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 41, 41,
                    /* 406 */ 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13,
                    /* 432 */ 13, 13, 13, 5, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13
            };

    private static final int[] MAP2 =
            {
                    /*  0 */ 57344, 63744, 64976, 65008, 65536, 63743, 64975, 65007, 65533, 1114111, 5, 13, 5, 13, 5
            };

    private static final int[] INITIAL =
            {
                    /*  0 */ 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25
            };

    private static final int[] TRANSITION =
            {
                    /*    0 */ 689, 689, 689, 689, 689, 689, 689, 689, 689, 689, 689, 689, 689, 689, 689, 689, 688, 988, 1035, 1042,
                    /*   20 */ 710, 1315, 689, 849, 854, 853, 690, 957, 689, 689, 689, 689, 689, 689, 1315, 1042, 915, 1315, 689, 689,
                    /*   40 */ 854, 1158, 690, 957, 689, 689, 689, 689, 1182, 689, 1187, 1110, 710, 1315, 689, 689, 854, 1158, 690, 957,
                    /*   60 */ 689, 689, 689, 689, 689, 689, 1315, 1042, 699, 707, 689, 689, 854, 1158, 690, 957, 689, 689, 689, 689,
                    /*   80 */ 689, 689, 1315, 1042, 710, 1315, 689, 689, 854, 1158, 690, 957, 689, 689, 689, 689, 689, 689, 1046, 718,
                    /*  100 */ 689, 689, 689, 689, 854, 1158, 690, 957, 689, 689, 689, 689, 1287, 689, 1293, 814, 710, 1315, 689, 689,
                    /*  120 */ 854, 1158, 690, 957, 689, 689, 689, 689, 689, 689, 1315, 1260, 710, 818, 733, 745, 799, 1237, 690, 957,
                    /*  140 */ 689, 689, 689, 689, 689, 689, 1315, 757, 710, 1315, 733, 1233, 854, 749, 690, 957, 689, 689, 689, 689,
                    /*  160 */ 689, 689, 775, 1042, 1129, 1315, 689, 689, 854, 1158, 690, 957, 689, 689, 689, 689, 689, 689, 1315, 1260,
                    /*  180 */ 710, 807, 826, 834, 842, 862, 690, 957, 689, 689, 689, 689, 763, 689, 1315, 1260, 710, 807, 826, 834,
                    /*  200 */ 842, 862, 690, 957, 689, 689, 689, 689, 870, 689, 1306, 1154, 975, 737, 880, 1233, 854, 749, 690, 957,
                    /*  220 */ 689, 689, 689, 689, 1012, 689, 1315, 1042, 710, 1315, 725, 722, 1061, 892, 690, 957, 689, 689, 689, 689,
                    /*  240 */ 689, 689, 1114, 900, 689, 689, 689, 689, 854, 1158, 690, 957, 689, 689, 689, 689, 689, 909, 1315, 1042,
                    /*  260 */ 710, 1315, 689, 689, 854, 1158, 690, 957, 689, 689, 689, 689, 689, 923, 928, 782, 936, 689, 689, 689,
                    /*  280 */ 1159, 1085, 950, 1010, 689, 689, 689, 689, 689, 967, 972, 1042, 942, 1315, 689, 1068, 1073, 1072, 690,
                    /*  299 */ 957, 689, 689, 689, 689, 983, 689, 1306, 1260, 975, 737, 880, 996, 842, 1020, 1139, 957, 689, 689, 689,
                    /*  319 */ 689, 983, 689, 1306, 1260, 975, 737, 880, 996, 842, 862, 690, 957, 689, 689, 689, 689, 983, 689, 1306,
                    /*  339 */ 1260, 975, 737, 880, 1028, 842, 862, 690, 957, 689, 689, 689, 689, 983, 689, 1306, 1260, 975, 737, 880,
                    /*  359 */ 996, 1054, 862, 690, 957, 689, 689, 689, 689, 983, 689, 1306, 1260, 975, 737, 880, 1233, 854, 749, 690,
                    /*  379 */ 957, 689, 689, 689, 689, 983, 689, 1306, 1260, 975, 737, 880, 1233, 854, 749, 1003, 957, 689, 689, 689,
                    /*  399 */ 689, 689, 689, 1315, 1042, 710, 767, 689, 689, 854, 1158, 691, 957, 689, 689, 689, 689, 689, 689, 959,
                    /*  419 */ 1081, 1266, 872, 689, 689, 854, 1158, 901, 1093, 689, 689, 689, 689, 870, 689, 1306, 1260, 975, 737, 880,
                    /*  439 */ 1233, 854, 749, 690, 957, 689, 689, 689, 689, 983, 689, 1306, 1260, 975, 1103, 880, 996, 842, 862, 690,
                    /*  459 */ 957, 689, 689, 689, 689, 983, 689, 1306, 1260, 975, 737, 880, 996, 1122, 862, 690, 957, 689, 689, 689,
                    /*  479 */ 689, 983, 1135, 1147, 1260, 1167, 737, 880, 996, 842, 862, 690, 957, 689, 689, 689, 689, 983, 689, 1306,
                    /*  499 */ 1260, 975, 737, 880, 1233, 854, 749, 1272, 957, 689, 689, 689, 689, 983, 689, 1306, 1260, 975, 737, 880,
                    /*  519 */ 1233, 1175, 1195, 690, 957, 689, 689, 689, 689, 983, 689, 1306, 1260, 975, 737, 880, 1233, 854, 1203,
                    /*  538 */ 690, 957, 689, 689, 689, 689, 983, 689, 1306, 1260, 975, 737, 880, 789, 854, 749, 690, 957, 689, 689,
                    /*  558 */ 689, 689, 983, 689, 1306, 1211, 975, 737, 880, 1233, 854, 749, 1219, 957, 689, 689, 689, 689, 983, 689,
                    /*  578 */ 1306, 1260, 975, 737, 1227, 1233, 854, 1245, 690, 957, 689, 689, 689, 689, 983, 689, 1306, 1260, 975,
                    /*  597 */ 1253, 880, 1233, 854, 749, 690, 957, 689, 689, 689, 689, 983, 689, 1306, 1260, 975, 737, 1280, 1233, 854,
                    /*  617 */ 749, 690, 957, 689, 689, 689, 689, 983, 794, 1306, 1260, 975, 737, 880, 1233, 854, 749, 690, 957, 689,
                    /*  637 */ 689, 689, 689, 983, 689, 1306, 1260, 975, 884, 1301, 1233, 854, 749, 690, 957, 689, 689, 689, 689, 689,
                    /*  657 */ 689, 1315, 1154, 710, 1315, 733, 1233, 854, 749, 690, 957, 689, 689, 689, 689, 689, 689, 1095, 1314, 689,
                    /*  677 */ 689, 689, 689, 689, 689, 689, 689, 689, 689, 689, 689, 385, 0, 0, 0, 0, 0, 0, 0, 0, 88, 88, 0, 0, 45, 0,
                    /*  703 */ 805, 805, 0, 50, 52, 0, 0, 0, 0, 0, 0, 805, 805, 0, 0, 1827, 0, 40, 41, 0, 0, 0, 0, 0, 27, 0, 28, 0, 0,
                    /*  732 */ 1408, 0, 0, 51, 0, 53, 0, 0, 0, 0, 0, 60, 805, 0, 69, 0, 60, 60, 0, 0, 0, 0, 69, 0, 71, 805, 538, 27, 28,
                    /*  761 */ 42, 1566, 0, 0, 0, 29, 0, 0, 0, 0, 0, 59, 0, 805, 0, 34, 0, 34, 34, 0, 0, 805, 0, 27, 28, 0, 0, 2944, 0,
                    /*  790 */ 0, 0, 60, 73, 0, 0, 0, 0, 33, 0, 0, 0, 0, 78, 0, 71, 0, 0, 1322, 0, 0, 56, 0, 0, 805, 0, 27, 640, 0, 0,
                    /*  820 */ 0, 0, 0, 58, 0, 805, 0, 62, 51, 64, 53, 0, 0, 56, 68, 0, 0, 60, 60, 62, 74, 64, 75, 0, 0, 68, 69, 0, 71,
                    /*  849 */ 0, 0, 0, 71, 71, 0, 0, 0, 0, 69, 0, 71, 0, 60, 74, 75, 0, 0, 69, 0, 71, 0, 538, 0, 0, 0, 0, 0, 0, 0, 816,
                    /*  880 */ 60, 0, 51, 0, 53, 0, 0, 0, 57, 0, 60, 805, 0, 27, 28, 0, 0, 69, 0, 71, 2343, 0, 0, 0, 0, 0, 0, 0, 89,
                    /*  909 */ 2688, 0, 0, 0, 0, 2688, 0, 0, 0, 46, 805, 805, 46, 0, 0, 2816, 0, 0, 0, 0, 2816, 0, 2816, 2816, 0, 0,
                    /*  935 */ 805, 0, 2048, 0, 0, 805, 805, 0, 0, 0, 47, 805, 805, 49, 0, 0, 0, 896, 0, 0, 0, 0, 88, 88, 0, 0, 0, 0, 0,
                    /*  964 */ 0, 0, 806, 0, 0, 31, 0, 0, 0, 0, 31, 0, 0, 0, 0, 805, 805, 0, 51, 0, 538, 0, 0, 1566, 0, 0, 0, 0, 0, 385,
                    /*  994 */ 385, 385, 68, 0, 0, 60, 60, 0, 74, 0, 0, 0, 86, 0, 0, 0, 88, 1152, 0, 0, 0, 0, 0, 0, 0, 2176, 60, 74, 75,
                    /* 1023 */ 0, 0, 69, 84, 71, 68, 0, 70, 60, 60, 0, 74, 0, 0, 0, 385, 0, 385, 0, 805, 0, 27, 28, 0, 0, 0, 0, 0, 0,
                    /* 1052 */ 1827, 1827, 75, 0, 0, 68, 69, 79, 71, 0, 0, 0, 1408, 69, 0, 71, 0, 0, 0, 72, 72, 0, 0, 0, 0, 69, 0, 80,
                    /* 1080 */ 0, 806, 0, 27, 28, 0, 0, 0, 0, 0, 69, 0, 1024, 90, 90, 0, 0, 0, 0, 0, 0, 256, 256, 53, 0, 54, 0, 0, 0,
                    /* 1109 */ 60, 805, 0, 640, 28, 0, 0, 0, 0, 0, 0, 2340, 2340, 75, 76, 0, 68, 69, 0, 71, 0, 0, 0, 2432, 805, 805, 0,
                    /* 1136 */ 0, 0, 32, 0, 0, 0, 0, 0, 87, 0, 88, 0, 0, 32, 0, 538, 0, 0, 805, 538, 27, 28, 0, 0, 0, 0, 0, 69, 0, 71,
                    /* 1166 */ 1024, 44, 0, 0, 0, 805, 805, 0, 51, 0, 0, 77, 0, 69, 0, 71, 0, 0, 27, 0, 0, 1664, 0, 0, 0, 0, 1691, 0,
                    /* 1194 */ 805, 60, 0, 0, 81, 0, 69, 0, 71, 2620, 0, 0, 0, 0, 69, 0, 71, 805, 538, 27, 28, 0, 1566, 0, 43, 85, 3200,
                    /* 1221 */ 0, 0, 0, 0, 0, 88, 60, 0, 51, 0, 53, 66, 0, 0, 0, 60, 60, 0, 0, 0, 0, 83, 0, 71, 60, 0, 0, 0, 82, 69, 0,
                    /* 1252 */ 71, 53, 0, 0, 55, 0, 0, 60, 805, 538, 27, 28, 0, 1566, 0, 0, 0, 0, 806, 816, 0, 0, 0, 0, 3072, 0, 0, 88,
                    /* 1280 */ 60, 0, 51, 0, 53, 0, 67, 0, 0, 28, 0, 0, 0, 1920, 0, 0, 0, 0, 1948, 0, 805, 61, 63, 51, 65, 53, 0, 0, 0,
                    /* 1309 */ 0, 538, 0, 0, 805, 256, 0, 0, 0, 0, 0, 0, 0, 805
            };

    private static final int[] EXPECTED =
            {
                    /*  0 */ 23, 32, 36, 43, 47, 51, 55, 59, 63, 70, 25, 67, 74, 85, 78, 74, 85, 79, 83, 28, 27, 39, 41, 4, 8, 16, 512,
                    /* 27 */ 8388608, 16777216, 64, 256, 128, 2048, 4096, 16384, 65536, 1048576, 2097152, 4194304, 8388608, 256, 256,
                    /* 42 */ 256, 16777216, 1048580, 2097156, 4194308, 20480, 2129920, 12582912, 2129924, 2129928, 20500, 402882,
                    /* 54 */ 402914, 665058, 8, 16, 16, 512, 2048, 4194304, 8388608, 16777216, 32768, 1024, 262592, 1024, 320, 128, 32,
                    /* 71 */ 32, 524736, 16, 524416, 16, 16, 16, 1024, 64, 256, 128, 128, 524416, 16, 16, 8388608, 16777216, 1024
            };

    private static final String[] TOKEN =
            {
                    "%ERROR",
                    "EOF",
                    "S",
                    "Name",
                    "AttValue",
                    "CharData",
                    "Comment",
                    "PI",
                    "CDSect",
                    "VersionNum",
                    "CharRef",
                    "EncName",
                    "'\"'",
                    "'&'",
                    "''''",
                    "'/>'",
                    "';'",
                    "'<'",
                    "'</'",
                    "'<?xml'",
                    "'='",
                    "'>'",
                    "'?>'",
                    "'encoding'",
                    "'version'"
            };
}

// End
