package io.adenium.papaya.parser;

import io.adenium.papaya.compiler.AbstractSyntaxTree;
import io.adenium.papaya.compiler.TokenStream;
import io.adenium.papaya.compiler.grammar.Grammar;
import org.json.JSONArray;
import org.json.JSONObject;
import io.adenium.exceptions.PapayaException;
import io.adenium.papaya.ParseRule;

import java.util.*;

public class DynamicParser implements Parser {
    private final List<ParseRule>           parseRules;
    private final Map<String, ParseRule>    references;

    public DynamicParser(JSONObject rules, JSONArray tokens) throws PapayaException {
        this.parseRules = new ArrayList<>();
        this.references = new HashMap<>();

        JSONArray parseRuleArray    = rules.getJSONArray("parse");
        JSONArray subs              = rules.getJSONArray("rule");

        for (int i = 0; i < parseRuleArray.length(); i ++) {
            ParseRule rule = new ParseRule(parseRuleArray.getJSONObject(i));
            parseRules.add(rule);
            references.put(rule.getName(), rule);
        }

        for (int i = 0; i < subs.length(); i ++) {
            ParseRule rule = new ParseRule(subs.getJSONObject(i));
            references.put(rule.getName(), rule);
        }

        for (int i = 0; i < tokens.length(); i ++) {
            String nameType = tokens.getJSONObject(i).keys().next();
            references.put(nameType, new DefaultRule(nameType));
        }

        references.put("string", new DefaultRule("string"));
    }

    public DynamicParser(Grammar grammar, JSONArray tokens) {
        this.parseRules = new ArrayList<>();
        this.references = new HashMap<>();

        grammar.getRules(references);

        for (int i = 0; i < tokens.length(); i ++) {
            String nameType = tokens.getJSONObject(i).keys().next();
            references.put(nameType, new DefaultRule(nameType));
        }

        parseRules.add(references.get("module_declaration"));
        parseRules.add(references.get("structure"));
        parseRules.add(references.get("field"));
        parseRules.add(references.get("method"));

        references.put("string", new DefaultRule("string"));
    }

    public ParseRule getRule(String name) throws PapayaException {
        if (references.containsKey(name)) {
            return references.get(name);
        }

        throw new PapayaException("rule '"+name+"' not found.");
    }

    @Override
    public AbstractSyntaxTree parse(TokenStream stream) throws PapayaException {
        AbstractSyntaxTree abstractSyntaxTree = new AbstractSyntaxTree();
        while (stream.hasNext()) {
            Node node = null;

            for (ParseRule rule : parseRules) {
                node = rule.parse(stream, this);
                if (node != null) {
                    break;
                }
            }

            if (node == null) {
                throwException(node, stream);
            }

            node.cleanUp();
            abstractSyntaxTree.add(node);
        }

        return abstractSyntaxTree;
    }

    public void throwException(Node node, TokenStream stream) throws PapayaException {
        TokenStream.TokenPath unexpected = stream.getLongestPath();
        String msg = "unexpected token '" + unexpected.getToken().getTokenValue() + "' expected "+goodGrammar(unexpected.getRule())+" '" + unexpected.getRule() + "' " + unexpected.getToken().getLineInfo();

        throw new PapayaException(msg);
    }

    private String goodGrammar(String word) {
        if (word.isEmpty()) return "a";
        switch (word.toLowerCase().charAt(0)) {
            case 'a':
            case 'o':
            case 'u':
            case 'i':
            case 'e':
                return "an";
            default:
                return "a";
        }
    }

    public Collection<ParseRule> getRules() {
        return references.values();
    }
}
