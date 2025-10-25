package io.adenium.papaya;

import io.adenium.exceptions.PapayaException;
import io.adenium.papaya.compiler.TokenStream;
import io.adenium.papaya.compiler.grammar.GrammarRule;
import io.adenium.papaya.parser.DynamicParser;
import io.adenium.papaya.parser.Node;
import io.adenium.papaya.parser.Rule;
import io.adenium.papaya.parser.Subrule;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class ParseRule implements Rule {
    private final String        ruleName;
    private final List<Rule>    options;

    public ParseRule(String name) {
        this.ruleName = name;
        this.options = new ArrayList<>();
    }

    public ParseRule(String name, List<Object> options) {
        this.ruleName = name;
        this.options = new ArrayList<>();
        for (Object option : options) {
            if (option instanceof List) {
                this.options.add(new Subrule(name, "", (List<Rule>) option));
            } else if (option instanceof GrammarRule) {
                ParseRule parseRule = ((GrammarRule) option).asParseRule();
                this.options.add(parseRule);
            } else {
                throw new RuntimeException("invalid object '" + option + "' provided.");
            }
        }
    }

    public void clearOptions() {
        this.options.clear();
    }

    public void addOptions(Subrule... options) {
        for (Subrule option : options) {
            this.options.add(option);
        }
    }

    public void addOptions(List<Subrule> options) {
        this.options.clear();;
        this.options.addAll(options);
    }

    public ParseRule(JSONObject rule) {
        this.ruleName = rule.keys().next();
        this.options  = new ArrayList<>();

        for (int i = 0; i < rule.getJSONArray(ruleName).length(); i ++) {
            JSONArray array = rule.getJSONArray(ruleName).getJSONArray(i);
            options.add(new Subrule(ruleName, "[" + i + "]", array));
        }
    }

//    public ParseResult parse(TokenStream stream, DynamicParser rules, Queue<ParseResult> results) throws PapayaException {
//        for (Subrule option : options) {
//            int mark = stream.mark();
//            ParseResult res = new ParseResult(this);
//            Node token = option.parse(stream, rules);
//            res.setResult(token);
//            res.jump(stream, mark);
//            results.add(res);
//
//            if (token != null) {
//                return res;
//            }
//
//            stream.jump(mark, getName());
//        }
//
//        return null;
//    }

    @Override
    public Node parse(TokenStream stream, DynamicParser rules) throws PapayaException {
        int highestJump = 0;
        for (Rule option : options) {
            int mark = stream.mark();
            highestJump = Math.max(mark, highestJump);
            Node node = option.parse(stream, rules);

            if (node == null) {
                String name = getName();
                if (name.isEmpty()) {
                    name = option.toString();
                }
                stream.jump(mark, name);
            } else {
                return node;
            }
        }

        return null;
    }

    public String getName() {
        return ruleName;
    }

    @Override
    public String toString() {
        return "ParseRule{" +
                "ruleName:'" + ruleName + '\'' +
                ", options:" + options +
                '}';
    }

    public int length(DynamicParser parser) throws PapayaException {
        int length = 0;
        if (options == null) return 0;

        for (Rule rule : options) {
            length += rule.length(parser);
        }

        return length;
    }

    public void sort() {
//        Collections.sort(options);
    }

    public String toSimpleString(DynamicParser parser) {
        return toString();
    }
}
