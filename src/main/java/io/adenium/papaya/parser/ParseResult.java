package io.adenium.papaya.parser;

import io.adenium.papaya.compiler.Token;
import io.adenium.papaya.compiler.TokenStream;
import io.adenium.papaya.ParseRule;

public class ParseResult implements Comparable<ParseResult> {
    private final ParseRule subrule;
    private int parsedRules;
    private Rule expected;
    private ParseRule expected2;
    private Token invalidToken;
    private Node parseResult;
    private TokenStream stream;
    private int mark;

    public ParseResult(ParseRule subrule) {
        this.subrule = subrule;
        this.parsedRules = 0;
        this.expected = null;
    }

    @Override
    public int compareTo(ParseResult o) {
        return parsedRules > o.parsedRules ? -1 : 1;
    }

    public ParseRule getRule() {
        return subrule;
    }

    public void add(int i) {
        parsedRules += i;
    }

    public void setInvalidToken(Token token) {
        this.invalidToken = token;
    }

    public Token getInvalidToken() {
        return invalidToken;
    }

    public void expect(Rule expected) {
        this.expected = expected;
    }

    public void expectRule(ParseRule expected) {
        this.expected2 = expected;
    }

    public String getExpected(DynamicParser parser) {
        if (expected != null) {
            return expected.toSimpleString(parser);
        }

        if (expected2 != null) {
            return expected2.toSimpleString(parser);
        }

        return "null{}";
    }

    public int getValidCount() {
        return parsedRules;
    }

    public void add(ParseResult result) {
        this.parsedRules += result.parsedRules;
        this.invalidToken = result.invalidToken;
        this.expected = result.expected;
        this.expected2 = result.expected2;
    }

    public void setResult(Node token) {
        this.parseResult = token;
    }

    public Node getParseResult() {
        return parseResult;
    }

    public void jump(TokenStream stream, int mark) {
        this.stream = stream;
        this.mark = mark;
    }

    public void jump() {
        stream.jump(mark, "");
    }
}
