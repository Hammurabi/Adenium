package io.adenium.papaya.parser;

import io.adenium.papaya.compiler.TokenStream;
import io.adenium.exceptions.PapayaException;

public class ParseRuleReference implements Rule {
    private final String ruleName;

    public ParseRuleReference(String ruleName) {
        this.ruleName = ruleName;
    }

    @Override
    public Node parse(TokenStream stream, DynamicParser rules) throws PapayaException {
        return rules.getRule(ruleName).parse(stream, rules);
    }

    @Override
    public int length(DynamicParser parser) throws PapayaException {
        return parser.getRule(ruleName).length(parser);
    }

    @Override
    public String toSimpleString(DynamicParser parser) {
        try {
            return parser.getRule(ruleName).toSimpleString(parser);
        } catch (PapayaException e) {
            return ruleName;
        }
    }

    @Override
    public String toString() {
        return "ParseRuleReference{" +
                "rulename:'" + ruleName + '\'' +
                '}';
    }
}
