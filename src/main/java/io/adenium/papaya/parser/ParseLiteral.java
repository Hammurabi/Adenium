package io.adenium.papaya.parser;

import io.adenium.papaya.compiler.Token;
import io.adenium.papaya.compiler.TokenStream;

public class ParseLiteral implements Rule {
    private final String string;

    public ParseLiteral(String string) {
        this.string = string;
    }

    @Override
    public Node parse(TokenStream stream, DynamicParser rules) {
        Token check = stream.next();
        if (check.getTokenValue().equals(string)) {
            return new Node(string, "default", check);
        }

        return null;
    }

    @Override
    public int length(DynamicParser parser) {
        return 1;
    }

    @Override
    public String toSimpleString(DynamicParser parser) {
        return string;
    }

    @Override
    public String toString() {
        return "ParseLiteral{" +
                "string:'" + string + '\'' +
                '}';
    }
}
