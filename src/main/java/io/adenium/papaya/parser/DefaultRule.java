package io.adenium.papaya.parser;

import io.adenium.papaya.compiler.Token;
import io.adenium.papaya.compiler.TokenStream;
import io.adenium.exceptions.PapayaException;
import io.adenium.papaya.ParseRule;

public class DefaultRule extends ParseRule {
    public DefaultRule(String nameType) {
        super(nameType);
    }

    @Override
    public Node parse(TokenStream stream, DynamicParser rules) throws PapayaException {
        int mark = stream.mark();
        if (!stream.hasNext()) {
            return null;
        }

        Token token = stream.next();
        if (token.getTokenType().equals(getName())) {
            return new Node(getName(), "default", token);
        }

        stream.jump(mark, getName());
        return null;
    }

    @Override
    public String toString() {
        return "DefaultRule{" +
                "nameType:'" + getName() + '\'' +
                '}';
    }

    @Override
    public String toSimpleString(DynamicParser parser) {
        return getName();
    }
}
