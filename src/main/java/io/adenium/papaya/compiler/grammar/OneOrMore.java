package io.adenium.papaya.compiler.grammar;

import io.adenium.exceptions.PapayaException;
import io.adenium.papaya.compiler.TokenStream;
import io.adenium.papaya.parser.DynamicParser;
import io.adenium.papaya.parser.Node;
import io.adenium.papaya.parser.Rule;

import java.util.ArrayList;
import java.util.List;

public class OneOrMore implements Rule {
    private final Rule rule;

    public OneOrMore(Rule rule) {
        this.rule = rule;
    }

    @Override
    public Node parse(TokenStream stream, DynamicParser rules) throws PapayaException {
        List<Node> list = new ArrayList<>();
        Node node = null;

        do {
            node = rule.parse(stream, rules);
            if (node != null) {
                list.add(node);
            }
        } while (node != null);
        if (!list.isEmpty()) {
            String name = list.get(0).getTokenRule();
            Node oom = new Node(name + "+", "");
            oom.add(list);
            return oom;
        }

        return null;
    }

    @Override
    public int length(DynamicParser parser) throws PapayaException {
        return 0;
    }

    @Override
    public String toSimpleString(DynamicParser parser) {
        return rule.toSimpleString(parser) + "+";
    }

    @Override
    public String toString() {
        return "OneOrMore{" +
                "rule=" + rule +
                '}';
    }
}
