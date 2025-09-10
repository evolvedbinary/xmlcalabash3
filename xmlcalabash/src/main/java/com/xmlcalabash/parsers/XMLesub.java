package com.xmlcalabash.parsers;

// This file was generated on Mon Sep 8, 2025 09:52 (UTC+01) by REx v6.2-SNAPSHOT which is Copyright (c) 1979-2025 by Gunther Rademacher <grd@gmx.net>
// REx command line: -java -tree XMLesub.ebnf

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
    lookahead1(28);                 // S | '?>'
    switch (l1)
    {
    case 5:                         // S
      lookahead2(41);               // '?>' | 'encoding'
      break;
    default:
      lk = l1;
    }
    if (lk == 3717)                 // S 'encoding'
    {
      parse_EncodingDecl();
    }
    lookahead1(28);                 // S | '?>'
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
    lookahead1(21);                 // 'version'
    consume(59);                    // 'version'
    parse_Eq();
    lookahead1(36);                 // '"' | "'"
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
    lookahead1(26);                 // S | '='
    if (l1 == 5)                    // S
    {
      consume(5);                   // S
    }
    lookahead1(14);                 // '='
    consume(36);                    // '='
    lookahead1(45);                 // S | '"' | "'"
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
    lookahead1(27);                 // S | '>'
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
    lookahead1(52);                 // '(' | 'ANY' | 'EMPTY'
    switch (l1)
    {
    case 24:                        // '('
      lookahead2(55);               // S | Name | '#PCDATA' | '('
      switch (lk)
      {
      case 344:                     // '(' S
        lookahead3(49);             // Name | '#PCDATA' | '('
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
    lookahead1(32);                 // Name | '('
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
    lookahead1(43);                 // S | Name | '('
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
    lookahead1(54);                 // ')' | ',' | '|'
    switch (l1)
    {
    case 60:                        // '|'
      for (;;)
      {
        consume(60);                // '|'
        lookahead1(43);             // S | Name | '('
        if (l1 == 5)                // S
        {
          consume(5);               // S
        }
        parse_cp();
        lookahead1(47);             // S | ')' | '|'
        if (l1 == 5)                // S
        {
          consume(5);               // S
        }
        lookahead1(38);             // ')' | '|'
        if (l1 != 60)               // '|'
        {
          break;
        }
      }
      break;
    default:
      for (;;)
      {
        lookahead1(37);             // ')' | ','
        if (l1 != 29)               // ','
        {
          break;
        }
        consume(29);                // ','
        lookahead1(43);             // S | Name | '('
        if (l1 == 5)                // S
        {
          consume(5);               // S
        }
        parse_cp();
        lookahead1(46);             // S | ')' | ','
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
    lookahead1(25);                 // S | '#PCDATA'
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
    lookahead1(53);                 // ')' | ')*' | '|'
    switch (l1)
    {
    case 25:                        // ')'
      consume(25);                  // ')'
      break;
    default:
      for (;;)
      {
        lookahead1(39);             // ')*' | '|'
        if (l1 != 60)               // '|'
        {
          break;
        }
        consume(60);                // '|'
        lookahead1(23);             // S | Name
        if (l1 == 5)                // S
        {
          consume(5);               // S
        }
        lookahead1(1);              // Name
        consume(6);                 // Name
        lookahead1(48);             // S | ')*' | '|'
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
      lookahead1(27);               // S | '>'
      switch (l1)
      {
      case 5:                       // S
        lookahead2(33);             // Name | '>'
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
    lookahead1(23);                 // S | Name
    if (l1 == 5)                    // S
    {
      consume(5);                   // S
    }
    lookahead1(1);                  // Name
    consume(6);                     // Name
    for (;;)
    {
      lookahead1(47);               // S | ')' | '|'
      switch (l1)
      {
      case 5:                       // S
        lookahead2(38);             // ')' | '|'
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
      lookahead1(22);               // '|'
      consume(60);                  // '|'
      lookahead1(23);               // S | Name
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
    lookahead1(24);                 // S | Nmtoken
    if (l1 == 5)                    // S
    {
      consume(5);                   // S
    }
    lookahead1(2);                  // Nmtoken
    consume(7);                     // Nmtoken
    for (;;)
    {
      lookahead1(47);               // S | ')' | '|'
      switch (l1)
      {
      case 5:                       // S
        lookahead2(38);             // ')' | '|'
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
      lookahead1(22);               // '|'
      consume(60);                  // '|'
      lookahead1(24);               // S | Nmtoken
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
        lookahead3(31);             // Name | '%'
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
    lookahead1(27);                 // S | '>'
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
    lookahead1(27);                 // S | '>'
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
    lookahead1(50);                 // EntityValue | 'PUBLIC' | 'SYSTEM'
    switch (l1)
    {
    case 8:                         // EntityValue
      consume(8);                   // EntityValue
      break;
    default:
      parse_ExternalID();
      lookahead1(27);               // S | '>'
      switch (l1)
      {
      case 5:                       // S
        lookahead2(40);             // '>' | 'NDATA'
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
    lookahead1(50);                 // EntityValue | 'PUBLIC' | 'SYSTEM'
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
    lookahead1(18);                 // 'NDATA'
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
    lookahead1(20);                 // 'encoding'
    consume(58);                    // 'encoding'
    parse_Eq();
    lookahead1(36);                 // '"' | "'"
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
    lookahead1(27);                 // S | '>'
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
    lookahead1(42);                 // 'PUBLIC' | 'SYSTEM'
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
      lookahead1(27);               // S | '>'
      switch (l1)
      {
      case 5:                       // S
        lookahead2(34);             // SystemLiteral | '>'
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
      lookahead2(56);               // S | PEReference | 'IGNORE' | 'INCLUDE'
      switch (lk)
      {
      case 354:                     // '<![' S
        lookahead3(51);             // PEReference | 'IGNORE' | 'INCLUDE'
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
    lookahead1(29);                 // S | 'INCLUDE'
    if (l1 == 5)                    // S
    {
      consume(5);                   // S
    }
    lookahead1(17);                 // 'INCLUDE'
    consume(49);                    // 'INCLUDE'
    lookahead1(30);                 // S | '['
    if (l1 == 5)                    // S
    {
      consume(5);                   // S
    }
    lookahead1(19);                 // '['
    consume(56);                    // '['
    parse_extSubsetDecl();
    consume(57);                    // ']]>'
    eventHandler.endNonterminal("includeSect", e0);
  }

  private void parse_ignoreSect()
  {
    eventHandler.startNonterminal("ignoreSect", e0);
    consume(34);                    // '<!['
    lookahead1(44);                 // S | PEReference | 'IGNORE'
    if (l1 == 5)                    // S
    {
      consume(5);                   // S
    }
    lookahead1(35);                 // PEReference | 'IGNORE'
    switch (l1)
    {
    case 48:                        // 'IGNORE'
      consume(48);                  // 'IGNORE'
      break;
    default:
      consume(15);                  // PEReference
    }
    lookahead1(30);                 // S | '['
    if (l1 == 5)                    // S
    {
      consume(5);                   // S
    }
    lookahead1(19);                 // '['
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
    /*    0 */ 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209,
    /*   17 */ 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2208, 2209,
    /*   34 */ 2414, 2227, 2209, 2224, 2220, 2235, 2240, 2209, 2592, 2445, 2209, 2209, 2209, 2248, 2209, 2209, 2209,
    /*   51 */ 2209, 2260, 2209, 2209, 2212, 2272, 2209, 2210, 3337, 2209, 2209, 2209, 2209, 2208, 2209, 2414, 2227,
    /*   68 */ 2209, 2224, 2220, 2235, 2285, 3471, 2592, 2445, 2209, 2209, 2209, 2248, 2209, 2209, 2209, 2209, 2260,
    /*   85 */ 2209, 2209, 2212, 2272, 2209, 2210, 3337, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209,
    /*  102 */ 2209, 3643, 2755, 3471, 2592, 3158, 2939, 2209, 2209, 2248, 2209, 2209, 2209, 2209, 2209, 2209, 2209,
    /*  119 */ 2212, 3339, 2209, 2210, 3337, 2209, 2209, 2209, 2209, 3138, 2402, 2209, 2209, 2398, 2702, 2649, 3171,
    /*  136 */ 3022, 2209, 3746, 2445, 2209, 2209, 2209, 2248, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2212, 3339,
    /*  153 */ 2209, 2210, 3337, 2209, 2209, 2209, 2209, 2209, 2299, 2209, 2299, 2209, 2209, 2293, 3759, 2755, 3471,
    /*  170 */ 2592, 2445, 3775, 2209, 3413, 2248, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2212, 3339, 2209, 2210,
    /*  187 */ 3337, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 3643, 2755, 3471, 2592, 2445,
    /*  204 */ 2209, 2209, 2209, 2248, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2212, 3339, 2209, 2210, 3337, 2209,
    /*  221 */ 2209, 2209, 2209, 2209, 2311, 2209, 2678, 3090, 3089, 3090, 2323, 2346, 3471, 3305, 2445, 2209, 2209,
    /*  238 */ 2209, 2248, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2212, 3339, 2209, 2210, 3337, 2209, 2209, 2209,
    /*  255 */ 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 3643, 3796, 2209, 3398, 2445, 2209, 2209, 2209, 2248,
    /*  272 */ 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2212, 3339, 2209, 2210, 3337, 2209, 2209, 2209, 2209, 3466,
    /*  289 */ 2716, 2209, 2209, 2440, 2714, 2770, 3711, 3215, 2354, 2894, 2445, 2209, 2209, 2209, 2248, 2209, 2209,
    /*  306 */ 2209, 2209, 2209, 2209, 2209, 2212, 3339, 2209, 2210, 3337, 2209, 2209, 2209, 2209, 2209, 2480, 2209,
    /*  323 */ 2209, 2370, 2481, 2363, 3728, 2755, 3471, 2592, 2445, 2209, 2209, 2209, 2248, 2209, 2209, 2209, 2209,
    /*  340 */ 2209, 2209, 2209, 2212, 3339, 2209, 2210, 3337, 2209, 2209, 2209, 2209, 2209, 2796, 2209, 2209, 2653,
    /*  357 */ 2652, 2379, 2383, 2755, 3471, 2592, 2445, 2209, 2209, 2209, 2248, 2209, 2209, 2209, 2209, 2209, 2209,
    /*  374 */ 2209, 2212, 3339, 2209, 2210, 3337, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209,
    /*  391 */ 2277, 2755, 3471, 3044, 2445, 2209, 2209, 2209, 2248, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2212,
    /*  408 */ 3339, 2209, 2210, 3337, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 3176, 2755,
    /*  425 */ 3471, 2592, 2445, 2209, 2209, 2209, 2248, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2212, 3339, 2209,
    /*  442 */ 2210, 3337, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2315, 2314, 2314, 2391, 2755, 3471, 2592,
    /*  459 */ 2445, 2209, 2209, 2209, 2248, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2212, 3339, 2209, 2210, 3337,
    /*  476 */ 2209, 2209, 2209, 2209, 2411, 2209, 2209, 2413, 2209, 2209, 2209, 3643, 2553, 2422, 2592, 2445, 2209,
    /*  493 */ 3295, 2209, 2248, 2433, 2883, 2453, 2211, 3505, 2209, 2209, 3509, 2465, 2209, 2355, 3337, 2209, 2209,
    /*  510 */ 2209, 2209, 2411, 2209, 2209, 2413, 2209, 2209, 2209, 3643, 2553, 2489, 2592, 2445, 2209, 3295, 2209,
    /*  527 */ 2248, 3482, 2883, 2453, 2209, 3505, 2209, 2209, 2212, 2465, 2209, 2210, 3337, 2209, 2209, 2209, 2209,
    /*  544 */ 2411, 2209, 2209, 2413, 2209, 2209, 2209, 3643, 2553, 2422, 2592, 2445, 2634, 3295, 2209, 2248, 2264,
    /*  561 */ 2883, 2500, 2209, 3668, 2209, 2523, 2212, 2535, 3653, 2210, 3337, 2209, 2209, 2209, 2209, 2561, 2209,
    /*  578 */ 2209, 2413, 2209, 2209, 2209, 3643, 2553, 2422, 2592, 2445, 2634, 3295, 2209, 2248, 2264, 2883, 2500,
    /*  595 */ 2209, 3668, 2209, 2523, 2212, 2535, 3653, 2210, 3337, 2209, 2209, 2209, 2209, 2580, 2209, 2884, 2582,
    /*  612 */ 2590, 3056, 3058, 3643, 2553, 3471, 3380, 2445, 2252, 3295, 2600, 2248, 3558, 2883, 2453, 2209, 3505,
    /*  629 */ 2209, 2209, 2212, 2465, 2209, 2210, 3337, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209,
    /*  646 */ 2209, 3643, 2755, 3471, 2592, 2445, 2209, 2690, 2209, 2248, 2301, 2633, 2618, 2209, 2303, 2209, 2619,
    /*  663 */ 2212, 2627, 2592, 2210, 3337, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2328,
    /*  680 */ 2642, 2209, 2592, 2445, 2209, 2209, 2209, 2248, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2212, 3339,
    /*  697 */ 2209, 2210, 3337, 2209, 2209, 2209, 2209, 2209, 2425, 2209, 2661, 2209, 2209, 2209, 3643, 2755, 3471,
    /*  714 */ 2592, 2445, 2209, 2209, 2209, 2248, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2212, 3339, 2209, 2210,
    /*  731 */ 3337, 2209, 2209, 2209, 2209, 2209, 3008, 2209, 3241, 2675, 2677, 2209, 3764, 2734, 2505, 2592, 2445,
    /*  748 */ 2209, 2209, 2209, 2686, 2729, 2209, 2209, 2209, 2209, 2209, 2209, 2212, 2698, 2209, 2210, 2470, 3818,
    /*  765 */ 2209, 2209, 2209, 2209, 2209, 3007, 3003, 2209, 3006, 2209, 3418, 2755, 3471, 2592, 2445, 3268, 2209,
    /*  782 */ 2209, 2248, 2209, 2209, 2209, 2209, 2710, 2209, 2209, 2212, 2724, 2209, 2210, 3521, 2209, 2209, 2209,
    /*  799 */ 2209, 2742, 2209, 2884, 2582, 2590, 3056, 3072, 3643, 2553, 2422, 3380, 2445, 2252, 3376, 2600, 2750,
    /*  816 */ 2763, 2883, 2453, 2943, 3613, 2781, 2792, 2804, 2535, 3653, 2210, 2993, 2209, 2209, 2209, 2209, 2742,
    /*  833 */ 2209, 2884, 2582, 2590, 3056, 3058, 3643, 2553, 2422, 3380, 2445, 2252, 3429, 2600, 2248, 3558, 2883,
    /*  850 */ 2453, 2209, 3613, 2209, 2792, 2212, 2535, 3653, 2210, 3337, 2209, 2209, 2209, 2209, 2742, 2209, 2884,
    /*  867 */ 2582, 2590, 3056, 3058, 2961, 2553, 2422, 3380, 2445, 2252, 2840, 2600, 2248, 3558, 2883, 2453, 2209,
    /*  884 */ 3613, 2209, 2792, 2212, 2535, 2610, 2210, 3337, 2209, 2209, 2209, 2209, 2742, 2209, 2884, 2582, 2590,
    /*  901 */ 3056, 3058, 3643, 2553, 2852, 3380, 3716, 2877, 3295, 2600, 2248, 3558, 2892, 2453, 2209, 3613, 2209,
    /*  918 */ 2792, 2212, 2535, 2902, 2910, 3337, 2209, 2475, 3272, 2209, 2742, 2209, 2884, 2582, 2590, 3056, 2548,
    /*  935 */ 3680, 2553, 2422, 3380, 2445, 2252, 3295, 2600, 2920, 2932, 2883, 2453, 2999, 3613, 3472, 2951, 2912,
    /*  952 */ 2535, 3535, 2527, 3361, 2969, 2981, 2209, 2209, 2742, 2209, 2884, 2582, 2590, 3056, 3058, 3643, 2553,
    /*  969 */ 2422, 3380, 2810, 2252, 3295, 2600, 2248, 3558, 2883, 2453, 2209, 3613, 2209, 2792, 3016, 2535, 3653,
    /*  986 */ 2210, 3337, 2209, 2209, 2209, 2209, 2742, 2209, 2884, 2582, 2590, 3056, 3058, 3643, 2553, 2422, 3030,
    /* 1003 */ 3038, 2252, 3295, 2600, 2248, 3558, 2883, 2453, 2209, 3505, 2209, 2209, 2212, 2465, 2209, 2210, 3337,
    /* 1020 */ 2209, 2209, 2209, 2209, 2742, 2209, 2884, 2582, 2590, 3056, 3058, 3643, 2553, 2422, 3380, 2445, 2252,
    /* 1037 */ 3295, 2600, 2248, 3558, 2883, 2453, 2209, 3505, 2209, 2209, 2212, 2465, 2209, 2210, 3337, 2209, 2209,
    /* 1054 */ 2209, 2209, 2742, 2209, 3052, 3066, 3085, 3453, 3449, 3098, 2553, 2422, 3380, 2816, 2252, 3295, 3106,
    /* 1071 */ 2248, 3558, 2883, 2453, 3114, 3505, 3598, 2209, 2212, 2465, 2209, 3125, 3133, 2371, 2209, 2869, 2209,
    /* 1088 */ 2742, 2209, 2884, 2582, 2590, 3056, 3058, 3643, 2553, 2422, 3380, 2445, 2252, 3295, 2600, 2248, 3558,
    /* 1105 */ 2883, 2453, 2209, 3505, 2209, 2209, 2987, 2465, 2209, 2210, 3337, 2209, 2209, 2209, 2209, 2742, 2209,
    /* 1122 */ 2884, 2582, 2590, 3056, 3058, 3643, 2553, 2422, 3380, 2445, 2252, 3295, 2600, 2248, 3558, 3146, 2453,
    /* 1139 */ 2209, 3154, 2209, 3698, 2212, 2465, 2209, 2210, 3166, 2209, 2209, 2209, 2209, 2742, 2209, 2884, 2582,
    /* 1156 */ 2590, 3056, 3058, 3643, 2553, 2422, 3380, 2859, 3184, 3295, 2600, 3198, 3558, 2883, 2453, 2209, 3505,
    /* 1173 */ 2209, 2209, 2212, 2465, 2924, 2210, 3210, 2209, 2209, 2209, 2209, 2742, 2209, 2542, 2582, 2590, 3223,
    /* 1190 */ 3058, 3631, 2553, 3235, 3380, 3253, 2252, 3588, 2600, 2248, 3261, 2883, 2453, 2209, 3280, 2209, 2209,
    /* 1207 */ 2212, 2465, 2209, 2210, 3337, 2605, 2209, 3292, 3303, 2742, 2209, 2884, 2582, 2590, 3056, 3058, 3643,
    /* 1224 */ 2553, 2422, 3380, 2445, 3313, 3295, 2600, 2248, 3558, 3190, 2453, 3284, 3333, 2209, 2209, 2212, 2465,
    /* 1241 */ 2209, 2210, 3337, 3245, 2209, 2209, 3347, 2742, 2209, 2884, 2582, 2590, 2568, 3443, 3643, 2553, 3357,
    /* 1258 */ 3380, 2445, 2252, 3295, 3369, 2248, 3558, 2883, 3388, 2209, 3505, 2209, 2209, 2212, 2465, 2209, 2210,
    /* 1275 */ 3337, 2209, 2209, 2209, 2209, 2742, 2209, 2884, 2582, 2590, 3056, 3058, 3643, 2553, 2422, 3380, 2445,
    /* 1292 */ 2252, 3295, 2600, 2248, 3558, 2883, 2453, 3396, 3505, 2209, 2209, 2212, 2465, 2209, 2210, 3337, 2209,
    /* 1309 */ 2209, 2209, 2209, 2742, 2209, 2884, 2582, 2590, 3056, 3058, 3643, 2553, 2422, 3380, 2822, 2252, 3295,
    /* 1326 */ 2600, 3406, 3558, 2883, 2453, 2209, 3505, 2457, 2209, 2212, 2465, 2209, 2210, 3337, 3426, 2209, 2209,
    /* 1343 */ 2209, 2742, 2209, 2884, 2582, 2590, 3565, 3437, 3643, 2553, 2422, 3380, 2445, 2252, 3295, 3461, 2248,
    /* 1360 */ 3558, 2883, 2453, 2209, 3505, 2209, 2209, 2212, 2465, 2209, 2515, 3337, 2209, 2492, 3480, 2209, 2742,
    /* 1377 */ 2209, 2884, 2582, 2590, 3056, 3058, 3643, 2553, 2422, 3380, 2445, 2252, 3295, 2600, 3490, 3498, 3517,
    /* 1394 */ 3529, 3543, 3505, 2209, 2209, 3551, 3577, 2209, 2210, 3337, 2209, 3585, 2773, 2209, 2742, 2209, 2884,
    /* 1411 */ 2582, 2590, 3056, 3058, 3643, 2553, 2422, 2832, 2445, 2252, 3295, 2600, 2248, 3558, 2883, 2453, 2209,
    /* 1428 */ 3505, 3596, 2403, 2212, 2465, 2209, 2210, 3337, 2209, 2209, 2209, 2209, 2742, 2209, 2884, 2582, 2590,
    /* 1445 */ 3056, 3058, 3643, 2553, 2422, 3380, 2445, 2252, 3295, 2600, 2248, 3558, 2883, 3606, 2209, 3505, 2209,
    /* 1462 */ 2209, 2212, 2465, 2209, 2210, 3337, 2209, 2209, 2209, 2209, 2742, 2209, 2884, 2582, 2590, 3056, 3058,
    /* 1479 */ 3643, 2553, 2422, 2844, 2445, 2252, 3295, 3621, 2248, 3558, 2883, 2453, 2209, 3505, 2209, 2510, 2212,
    /* 1496 */ 2465, 2209, 3325, 3337, 2209, 2209, 2865, 2209, 2209, 2209, 2667, 2664, 2209, 2209, 2209, 3643, 2734,
    /* 1513 */ 2209, 2592, 2445, 2209, 2209, 2209, 3639, 3651, 2209, 2209, 2209, 2209, 2209, 2209, 2212, 3339, 2209,
    /* 1530 */ 2210, 3337, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 3643, 2734, 2209, 2592,
    /* 1547 */ 2445, 2209, 2209, 2209, 2248, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2212, 3339, 2209, 2210, 3337,
    /* 1564 */ 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2333, 3661, 2209, 2592, 2338, 3202,
    /* 1581 */ 2209, 2209, 2248, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2212, 3339, 2209, 2210, 3337, 2209, 2209,
    /* 1598 */ 2209, 2209, 2580, 2209, 2884, 2582, 2590, 3056, 3058, 3643, 2553, 2422, 3380, 2445, 2252, 3295, 2600,
    /* 1615 */ 2248, 3558, 2883, 2453, 2209, 3505, 2209, 2209, 2212, 2465, 2209, 2210, 3337, 2209, 2209, 2209, 2209,
    /* 1632 */ 2742, 2209, 2884, 2582, 2590, 3056, 3058, 3643, 2553, 2422, 3380, 2445, 2252, 3295, 2600, 2248, 3558,
    /* 1649 */ 2883, 2453, 2209, 3613, 2209, 2792, 2212, 2535, 3653, 2210, 3337, 2209, 2209, 2209, 2209, 2742, 2209,
    /* 1666 */ 2884, 2582, 2590, 3056, 3058, 3643, 2553, 2422, 3380, 2445, 2252, 2828, 2600, 2248, 3558, 2883, 2453,
    /* 1683 */ 2209, 3613, 2209, 2792, 2212, 2535, 3653, 2210, 3337, 2209, 2209, 2209, 2209, 2742, 2209, 2884, 2582,
    /* 1700 */ 2590, 3056, 3058, 3643, 2553, 2422, 3380, 2445, 2252, 3295, 2600, 2248, 3558, 2883, 2453, 2209, 3613,
    /* 1717 */ 2957, 2792, 2212, 2535, 3653, 2210, 3337, 2209, 2209, 2209, 2209, 2742, 2209, 2973, 2582, 2590, 3676,
    /* 1734 */ 3058, 3643, 2553, 2422, 3688, 2445, 2252, 3295, 2600, 2248, 3558, 2883, 2453, 2209, 3613, 2209, 2792,
    /* 1751 */ 2212, 2535, 3653, 2210, 3337, 2209, 2209, 2209, 2209, 2742, 2209, 2884, 2582, 2590, 3056, 3058, 3643,
    /* 1768 */ 2553, 2422, 3380, 2445, 2252, 3295, 2600, 2248, 3558, 2883, 2453, 2209, 3505, 2209, 2209, 2212, 2465,
    /* 1785 */ 2209, 2210, 3337, 2209, 3627, 2209, 2209, 2742, 2209, 2884, 2582, 2590, 3056, 3058, 3643, 2553, 2422,
    /* 1802 */ 3380, 2445, 2252, 3295, 2600, 2248, 3558, 2883, 2453, 2209, 3505, 3569, 2209, 2212, 2465, 3696, 2210,
    /* 1819 */ 3337, 2209, 2209, 2209, 2209, 2742, 2209, 2884, 2582, 2590, 3056, 3058, 3643, 2553, 2422, 3380, 2445,
    /* 1836 */ 2252, 3295, 2600, 2248, 3558, 2883, 2453, 2209, 3505, 2209, 2209, 2212, 3706, 2209, 2210, 3337, 2209,
    /* 1853 */ 2209, 2209, 2209, 2742, 2209, 2884, 2582, 2590, 3056, 3058, 3643, 2553, 2422, 3380, 2445, 2252, 3295,
    /* 1870 */ 2600, 2248, 3558, 2883, 2453, 2209, 3724, 2209, 2209, 2212, 2465, 2209, 2210, 3337, 2209, 2209, 2209,
    /* 1887 */ 2209, 2742, 2209, 2884, 2582, 2590, 3056, 3058, 3643, 2553, 3736, 3380, 2445, 2252, 3295, 2600, 2248,
    /* 1904 */ 3558, 2883, 2453, 2209, 3505, 2209, 2209, 2212, 2465, 2209, 2210, 2572, 3744, 2209, 2209, 2209, 2742,
    /* 1921 */ 2209, 2884, 2582, 2590, 3056, 3058, 3643, 2553, 2422, 3380, 2445, 2252, 3295, 2600, 2248, 3558, 3754,
    /* 1938 */ 2453, 2209, 3505, 2209, 2209, 2212, 2465, 3772, 2210, 3337, 2209, 2209, 2209, 2209, 2742, 2209, 2884,
    /* 1955 */ 2582, 2590, 3056, 3058, 3643, 2553, 2422, 3380, 2445, 2252, 3227, 2600, 2248, 3558, 2883, 2453, 2209,
    /* 1972 */ 3505, 2209, 2209, 2212, 2465, 2209, 2210, 3337, 2209, 2209, 2209, 2209, 2742, 2209, 2884, 2582, 2590,
    /* 1989 */ 3056, 3058, 3643, 2553, 2422, 3380, 2445, 2252, 3295, 2600, 2248, 3558, 3319, 2453, 2209, 3505, 2209,
    /* 2006 */ 2209, 2212, 2465, 2209, 2210, 3337, 2209, 2209, 2209, 2209, 2742, 2209, 3117, 2582, 2590, 3056, 3058,
    /* 2023 */ 3643, 2553, 2422, 3380, 2445, 2252, 3295, 2600, 2248, 3558, 2883, 2453, 2209, 3505, 2209, 2209, 2212,
    /* 2040 */ 2465, 2209, 2210, 3337, 2209, 2209, 2209, 2209, 2742, 2209, 2884, 2582, 2590, 3056, 3058, 3643, 2553,
    /* 2057 */ 2422, 3380, 2445, 2252, 3295, 2600, 2248, 3783, 2883, 3791, 2209, 3505, 2209, 2209, 2212, 2465, 2209,
    /* 2074 */ 2210, 3337, 2209, 2209, 2209, 2209, 2209, 2209, 3349, 2209, 2784, 3348, 3804, 3808, 2734, 2209, 2592,
    /* 2091 */ 2445, 2209, 2209, 2209, 2248, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2212, 3339, 2209, 2210, 3337,
    /* 2108 */ 2209, 2209, 2209, 2209, 2411, 2209, 2209, 2413, 2209, 2209, 2209, 3643, 3077, 2209, 2592, 2445, 2209,
    /* 2125 */ 3295, 2209, 2248, 3482, 2883, 2453, 2209, 3505, 2209, 2209, 2212, 2465, 2209, 2210, 3337, 2209, 2209,
    /* 2142 */ 2209, 2209, 2580, 2209, 2884, 2582, 2590, 3056, 3058, 3643, 3077, 2209, 3380, 2445, 2252, 3295, 2600,
    /* 2159 */ 2248, 3558, 2883, 2453, 2209, 3505, 2209, 2209, 2212, 2465, 2209, 2210, 3337, 2209, 2209, 2209, 2209,
    /* 2176 */ 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 3816, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209,
    /* 2193 */ 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 2209, 1537, 0, 0, 0,
    /* 2212 */ 0, 0, 0, 0, 0, 190, 0, 0, 1537, 0, 0, 0, 0, 0, 0, 1537, 1537, 1537, 1537, 1537, 1537, 1537, 0, 1537,
    /* 2236 */ 1537, 1537, 0, 512, 1537, 1537, 0, 68, 69, 70, 71, 0, 0, 0, 1024, 1024, 0, 0, 0, 0, 0, 135, 137, 0, 0, 0,
    /* 2262 */ 195, 195, 0, 0, 0, 0, 0, 165, 135, 167, 0, 0, 195, 0, 195, 0, 0, 0, 0, 512, 7168, 7168, 0, 1537, 1537, 0,
    /* 2288 */ 68, 69, 70, 71, 72, 0, 76, 0, 0, 0, 0, 0, 76, 0, 0, 0, 0, 0, 0, 68, 0, 69, 0, 0, 0, 5888, 0, 0, 0, 0, 0,
    /* 2319 */ 0, 7680, 0, 0, 82, 0, 0, 0, 512, 0, 0, 0, 0, 605, 0, 0, 0, 0, 606, 0, 0, 0, 0, 768, 124, 0, 0, 82, 82, 0,
    /* 2349 */ 68, 69, 70, 71, 72, 3072, 0, 0, 0, 0, 0, 0, 0, 231, 0, 6400, 0, 0, 6400, 0, 0, 6400, 0, 0, 0, 0, 0, 0, 0,
    /* 2378 */ 242, 84, 0, 0, 0, 0, 6740, 6656, 0, 512, 0, 6656, 0, 0, 0, 7680, 0, 512, 0, 7680, 0, 0, 70, 0, 4608, 0,
    /* 2404 */ 0, 0, 0, 0, 0, 0, 211, 0, 0, 2051, 0, 0, 0, 0, 0, 0, 0, 1537, 73, 0, 4427, 0, 0, 0, 0, 0, 0, 9472, 0, 0,
    /* 2434 */ 159, 0, 0, 0, 0, 135, 0, 0, 71, 0, 6144, 0, 0, 0, 0, 768, 768, 0, 0, 0, 146, 0, 148, 0, 0, 0, 0, 0, 204,
    /* 2463 */ 0, 0, 0, 0, 195, 0, 163, 0, 0, 0, 0, 3584, 0, 0, 0, 0, 5120, 0, 0, 0, 0, 6400, 0, 0, 0, 0, 73, 104, 4427,
    /* 2492 */ 0, 0, 0, 0, 0, 0, 11264, 13568, 0, 146, 177, 148, 179, 0, 0, 0, 0, 10240, 0, 0, 0, 0, 11008, 0, 0, 0, 0,
    /* 2519 */ 12288, 0, 0, 190, 177, 207, 179, 208, 0, 0, 0, 0, 0, 229, 0, 190, 0, 0, 195, 0, 163, 198, 199, 0, 0, 79,
    /* 2545 */ 0, 0, 0, 0, 1859, 0, 0, 91, 0, 0, 1859, 68, 69, 70, 71, 72, 0, 0, 2051, 0, 0, 0, 74, 0, 0, 85, 1859, 0,
    /* 2573 */ 0, 0, 0, 195, 0, 0, 236, 0, 1859, 2051, 0, 0, 0, 0, 0, 0, 1859, 1859, 1859, 0, 0, 0, 0, 0, 0, 87, 88, 0,
    /* 2601 */ 146, 146, 148, 148, 0, 0, 0, 0, 13552, 0, 0, 0, 0, 14080, 0, 207, 208, 0, 87, 0, 88, 0, 0, 0, 0, 0, 0, 0,
    /* 2629 */ 195, 0, 0, 68, 69, 0, 0, 0, 0, 0, 0, 0, 3944, 99, 100, 0, 0, 0, 70, 71, 0, 0, 87, 0, 0, 0, 0, 0, 0, 6656,
    /* 2659 */ 6656, 84, 0, 0, 9472, 0, 0, 0, 0, 0, 0, 14592, 0, 0, 0, 0, 0, 9728, 9728, 0, 0, 0, 0, 0, 0, 0, 5888, 0,
    /* 2687 */ 0, 1024, 14848, 0, 0, 0, 0, 0, 4096, 0, 0, 0, 0, 195, 3584, 0, 0, 0, 0, 0, 4608, 0, 0, 0, 0, 196, 196, 0,
    /* 2715 */ 0, 0, 0, 0, 6144, 0, 0, 0, 0, 0, 0, 221, 0, 196, 0, 0, 0, 0, 14848, 0, 0, 0, 68, 69, 70, 71, 0, 0, 1859,
    /* 2744 */ 2051, 0, 0, 0, 0, 4427, 0, 0, 1024, 1024, 154, 0, 0, 0, 68, 69, 70, 71, 72, 0, 160, 163, 163, 0, 0, 135,
    /* 2770 */ 0, 0, 88, 0, 0, 0, 0, 0, 0, 7936, 8192, 200, 0, 13056, 0, 0, 0, 0, 0, 0, 15616, 15616, 0, 207, 0, 208, 0,
    /* 2797 */ 0, 0, 0, 0, 6656, 0, 0, 10752, 0, 0, 0, 0, 190, 0, 0, 0, 120, 768, 768, 0, 0, 0, 121, 768, 768, 0, 0, 0,
    /* 2825 */ 122, 768, 768, 0, 0, 0, 141, 0, 110, 0, 0, 112, 0, 87, 88, 138, 139, 0, 0, 0, 110, 0, 0, 0, 113, 87, 88,
    /* 2852 */ 73, 0, 4427, 0, 0, 0, 107, 0, 0, 119, 0, 768, 768, 0, 0, 0, 8448, 0, 0, 0, 0, 249, 0, 0, 0, 11903, 0, 0,
    /* 2880 */ 0, 0, 135, 137, 0, 0, 0, 0, 0, 0, 0, 1859, 137, 169, 0, 0, 0, 0, 0, 0, 87, 2304, 223, 0, 0, 0, 0, 0, 207,
    /* 2909 */ 208, 4864, 0, 0, 0, 0, 0, 0, 190, 0, 218, 0, 153, 1024, 1024, 0, 0, 0, 0, 0, 14336, 0, 0, 0, 161, 163,
    /* 2935 */ 163, 0, 0, 135, 0, 0, 130, 130, 0, 0, 0, 0, 0, 189, 0, 0, 0, 207, 0, 208, 0, 209, 0, 0, 0, 202, 0, 0, 0,
    /* 2964 */ 0, 512, 0, 0, 95, 0, 237, 0, 239, 0, 0, 0, 0, 80, 0, 0, 1859, 243, 0, 0, 0, 0, 246, 0, 0, 0, 214, 0, 190,
    /* 2993 */ 0, 0, 0, 235, 195, 5376, 0, 0, 0, 187, 0, 0, 0, 0, 77, 0, 0, 0, 0, 0, 0, 0, 9728, 0, 0, 12245, 0, 0, 190,
    /* 3022 */ 0, 0, 0, 2560, 69, 2816, 71, 3072, 0, 110, 111, 0, 0, 0, 87, 88, 111, 0, 0, 0, 768, 768, 0, 0, 0, 6912,
    /* 3048 */ 0, 0, 87, 88, 0, 78, 0, 0, 0, 0, 0, 1859, 0, 0, 0, 0, 0, 1859, 2051, 0, 0, 0, 0, 78, 0, 1859, 0, 0, 90,
    /* 3077 */ 0, 0, 1859, 68, 69, 70, 71, 0, 1859, 1859, 0, 83, 0, 0, 0, 0, 82, 0, 0, 0, 0, 89, 0, 0, 0, 512, 0, 0, 97,
    /* 3106 */ 0, 146, 146, 148, 148, 0, 0, 151, 0, 0, 186, 0, 0, 0, 0, 0, 81, 0, 1859, 0, 226, 227, 228, 0, 0, 230,
    /* 3132 */ 190, 0, 0, 234, 0, 195, 0, 0, 0, 68, 70, 72, 0, 0, 137, 0, 170, 0, 0, 0, 0, 175, 192, 0, 163, 163, 0, 0,
    /* 3160 */ 0, 0, 123, 768, 0, 0, 232, 0, 0, 0, 195, 0, 0, 0, 68, 512, 0, 0, 0, 0, 512, 7424, 7424, 0, 0, 128, 0, 0,
    /* 3188 */ 0, 135, 137, 0, 0, 0, 0, 0, 174, 0, 152, 0, 1024, 1024, 0, 0, 0, 0, 133, 0, 0, 0, 0, 233, 0, 0, 195, 0,
    /* 3216 */ 0, 0, 68, 2560, 70, 2816, 72, 79, 0, 0, 1859, 0, 0, 0, 0, 142, 110, 0, 0, 73, 0, 4427, 0, 0, 106, 0, 0,
    /* 3243 */ 0, 9728, 0, 0, 0, 0, 0, 241, 0, 0, 106, 118, 0, 0, 768, 768, 0, 126, 0, 162, 163, 163, 0, 0, 135, 0, 0,
    /* 3270 */ 131, 132, 0, 0, 0, 0, 0, 5632, 0, 0, 193, 0, 163, 163, 0, 0, 0, 0, 188, 0, 0, 0, 13824, 0, 248, 0, 0, 0,
    /* 3298 */ 0, 0, 110, 0, 0, 0, 8704, 0, 0, 0, 0, 0, 0, 114, 116, 0, 129, 0, 0, 0, 135, 137, 0, 0, 0, 0, 173, 0, 0,
    /* 3327 */ 0, 11520, 0, 0, 0, 190, 0, 194, 163, 163, 0, 0, 0, 0, 195, 0, 0, 0, 0, 0, 250, 0, 0, 0, 0, 0, 0, 0,
    /* 3355 */ 15616, 0, 73, 0, 4427, 105, 0, 0, 0, 0, 195, 0, 12800, 0, 0, 146, 146, 148, 148, 0, 150, 0, 0, 140, 0, 0,
    /* 3381 */ 110, 0, 0, 0, 0, 87, 88, 0, 146, 0, 148, 0, 0, 0, 183, 184, 0, 0, 0, 0, 0, 0, 0, 115, 117, 0, 0, 1024,
    /* 3409 */ 1024, 0, 0, 156, 0, 0, 147, 0, 149, 0, 0, 0, 0, 512, 9984, 9984, 0, 0, 0, 238, 0, 0, 0, 0, 0, 110, 0,
    /* 3436 */ 144, 0, 1859, 86, 0, 0, 0, 0, 1859, 85, 0, 0, 0, 0, 1859, 0, 89, 0, 0, 0, 1859, 83, 0, 0, 0, 145, 146,
    /* 3463 */ 146, 148, 148, 0, 0, 0, 69, 71, 73, 0, 0, 0, 0, 0, 0, 0, 206, 0, 247, 0, 0, 0, 0, 0, 0, 135, 0, 0, 0,
    /* 3492 */ 1024, 1024, 0, 155, 0, 157, 158, 0, 163, 163, 0, 0, 135, 0, 0, 163, 163, 0, 0, 0, 0, 0, 216, 0, 0, 137,
    /* 3518 */ 0, 0, 171, 0, 0, 0, 0, 221, 0, 0, 0, 176, 146, 0, 148, 0, 181, 0, 0, 0, 12544, 0, 0, 207, 208, 0, 185, 0,
    /* 3546 */ 0, 0, 0, 0, 191, 0, 212, 0, 0, 215, 190, 217, 0, 0, 163, 163, 0, 0, 135, 0, 0, 86, 1859, 0, 0, 0, 0, 203,
    /* 3574 */ 0, 0, 0, 219, 220, 195, 0, 163, 0, 0, 222, 0, 244, 245, 0, 0, 0, 0, 0, 110, 143, 0, 0, 201, 0, 0, 0, 0,
    /* 3602 */ 0, 0, 205, 0, 0, 146, 0, 148, 0, 0, 182, 0, 0, 163, 163, 0, 198, 0, 199, 0, 146, 146, 148, 148, 10496, 0,
    /* 3628 */ 0, 0, 15104, 0, 0, 0, 0, 512, 0, 0, 98, 0, 0, 8960, 1024, 0, 0, 0, 0, 512, 0, 0, 0, 0, 8960, 0, 0, 0, 0,
    /* 3657 */ 0, 0, 207, 208, 0, 101, 0, 68, 69, 70, 71, 0, 0, 163, 163, 165, 198, 167, 199, 0, 80, 0, 1859, 0, 0, 0,
    /* 3683 */ 0, 512, 0, 0, 96, 109, 110, 0, 0, 0, 0, 87, 88, 0, 224, 0, 0, 0, 0, 0, 0, 210, 0, 0, 0, 195, 0, 9379, 0,
    /* 3712 */ 0, 0, 69, 512, 0, 0, 0, 0, 768, 768, 125, 0, 0, 0, 163, 197, 0, 0, 0, 0, 512, 0, 0, 6400, 73, 0, 4427, 0,
    /* 3740 */ 0, 0, 0, 108, 15360, 0, 0, 0, 0, 0, 0, 0, 2304, 88, 137, 0, 0, 0, 172, 0, 0, 0, 92, 512, 0, 0, 0, 0, 512,
    /* 3769 */ 9728, 0, 0, 0, 0, 225, 0, 0, 0, 0, 0, 134, 136, 0, 0, 0, 164, 163, 0, 166, 135, 168, 0, 146, 178, 148,
    /* 3795 */ 180, 0, 0, 0, 102, 103, 70, 71, 0, 15616, 0, 0, 0, 0, 15616, 15616, 0, 512, 0, 15616, 0, 1280, 1280, 0,
    /* 3819 */ 0, 0, 0, 0, 0, 3328, 0
  };

  private static final int[] EXPECTED =
  {
    /*   0 */ 167, 172, 329, 125, 217, 235, 131, 133, 137, 141, 279, 145, 149, 153, 157, 161, 177, 170, 173, 217, 181,
    /*  21 */ 251, 265, 185, 127, 193, 216, 197, 252, 383, 204, 217, 210, 225, 215, 217, 252, 230, 234, 188, 260, 226,
    /*  42 */ 216, 217, 253, 231, 217, 189, 363, 213, 217, 251, 232, 187, 248, 263, 239, 188, 249, 233, 247, 257, 217,
    /*  63 */ 217, 217, 217, 269, 386, 295, 269, 164, 277, 283, 287, 293, 301, 304, 307, 311, 217, 217, 326, 273, 289,
    /*  84 */ 380, 315, 319, 323, 199, 273, 351, 250, 333, 317, 339, 207, 217, 272, 351, 334, 296, 343, 219, 243, 199,
    /* 105 */ 348, 352, 335, 297, 344, 219, 242, 200, 350, 217, 356, 234, 221, 360, 367, 218, 371, 377, 220, 373,
    /* 125 */ 16777216, 33554432, 0, 0, -1073729536, -1073729536, 160, 1048608, 32, 32, 32, 4194368, 16777280, 64, 1024,
    /* 140 */ 32768, 8519680, 570425344, 33554432, 67108864, 32800, 8519712, 570425376, 33554464, 67108896, 17825856,
    /* 151 */ 256, 32768, 16777216, 100663296, 570425344, 17825888, 32800, 100663328, 570425376, 2884096, 14, 402653216,
    /* 163 */ 973078560, 16777216, 0, 0, 32, 64, 128, 512, 1024, 1024, 2048, 16384, 65536, 1048576, -1073696720,
    /* 178 */ -1073696720, 64, 512, 0, 32768, 0, 67108864, 12, 12, 0, 0, 0, 4096, 1073741824, -2147483648, 0, 512, 512,
    /* 196 */ 16384, 0, 32768, 0, 0, 0, 131072, 67108864, 524288, 2097152, 8, 8, 0, 33554432, 0, -1073737728, 8192,
    /* 213 */ 8192, 512, 512, 1048576, 0, 0, 0, 0, 1, 2, 0, 0, 0, 512, 512, 512, 512, 256, 0, 262144, 524288, 2097152,
    /* 235 */ 0, 0, 0, 96, 0, 524288, 2097152, 0, 0, 8, 0, 0, 0, 1073741824, -2147483648, 0, 0, 0, 256, 256, 256, 256,
    /* 257 */ 0, 2097152, 1073741824, -2147483648, 0, 8192, 8192, 1048576, 0, 0, 0, 2883584, 16, 32, 128, 131072,
    /* 273 */ 262144, 67108864, 134217728, 0, 32, 65536, 0, 0, 0, 16777312, 268435456, 268435456, 262176, 67108992,
    /* 287 */ 12582912, 0, 65536, 0, 4194304, 8388608, 0, 268435456, 268435456, 0, 0, 0, 512, 6144, 12582912, 196608,
    /* 303 */ 1280, 268435456, 0, 196608, 268435456, 0, 33554436, 96, 268435520, 3734016, 15, 33554439, 1024, 0, 4,
    /* 318 */ 33554432, 512, 6144, 57344, 3670016, 15, 7, 33554432, 0, 0, 128, 131072, 1048576, 4194304, 8388608, 1024,
    /* 334 */ 0, 0, 0, 1024, 0, 49152, 1572864, 2097152, 7, 6144, 49152, 1572864, 2097152, 0, 262144, 67108864,
    /* 350 */ 134217728, 65536, 4194304, 8388608, 0, 0, 0, 6144, 32768, 1572864, 131072, 67108864, 134217728, 0, 0,
    /* 365 */ 8192, 8192, 0, 2048, 1572864, 2097152, 2, 67108864, 0, 0, 2, 2, 2048, 1048576, 2097152, 0, 0, 196608, 256,
    /* 384 */ 0, 0, 262144, 16777216, 67108864, 134217728
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
