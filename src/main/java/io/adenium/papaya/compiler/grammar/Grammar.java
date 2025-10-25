package io.adenium.papaya.compiler.grammar;

import io.adenium.exceptions.PapayaException;
import io.adenium.papaya.ParseRule;
import io.adenium.papaya.parser.ParseLiteral;
import io.adenium.papaya.parser.ParseRuleReference;
import io.adenium.papaya.parser.Rule;
import io.adenium.papaya.parser.Subrule;

import java.util.*;

public class Grammar {
    private Map<String, Object> map;

    public Grammar(String cfg) throws PapayaException {
        map = new HashMap<>();
        Stack<Map<String, Object>> stack = new Stack<>();
        stack.push(map);
        int ws = 0;
        String split[] = cfg.split("\n");
        GrammarRule rule = null;

        for (String line : split) {
            int whiteSpace = line.indexOf(line.trim()) / 2;

            if (whiteSpace < ws && stack.size() > 1) {
                int dif = ws - whiteSpace;
                for (int i = 0; i < dif; i ++) {
                    stack.pop();
                }

                ws = whiteSpace;
            }

            String no_whitespace = line.trim();

            if (no_whitespace.endsWith(":")) {
                GrammarRule object = new GrammarRule(no_whitespace.substring(0, no_whitespace.length() - 1));
                stack.peek()
                        .put(no_whitespace.substring(0, no_whitespace.length() - 1), object);
                stack.push(object.asMap());
                ws ++;
                rule = object;
                continue;
            }

            String words[] = split(line.trim());
            rule.add(parseRules(words, line, whiteSpace));
        }
    }

    private String[] split(String trim) {
        List<String> strings = new ArrayList<>();
        strings.add("");
        for (int i = 0; i < trim.length(); i ++) {
            if (trim.charAt(i) == ' ') {
                strings.add("");
            } else if (trim.charAt(i) == '(' && !strings.get(strings.size() - 1).endsWith("'")) {
                int open    = 1;
                int close   = 0;
                for (int x = i + 1; x < trim.length(); x ++) {
                    i = x;
                    if (trim.charAt(x) == '(') open ++;
                    else if (trim.charAt(x) == ')') close ++;

                    if (open == close) {
                        break;
                    }

                    strings.set(strings.size() - 1, strings.get(strings.size() - 1) + trim.charAt(x));
                }

                strings.set(strings.size() - 1, "(" + strings.get(strings.size() - 1) + ")");
            } else {
                strings.set(strings.size() - 1, strings.get(strings.size() - 1) + trim.charAt(i));
            }
        }

        String[] split = new String[strings.size()];
        return strings.toArray(split);
    }

    private Rule wrap(Rule rule, boolean one_or_more, boolean zero_or_more) {
        if (one_or_more) {
            return new OneOrMore(rule);
        }

        if (zero_or_more) {
            return new ZeroOrMore(rule);
        }

        return rule;
    }

    private Rule subRule(String rule, boolean one_or_more, boolean zero_or_more, String line, int whiteSpace) throws PapayaException {
        String ruleString = rule.substring(1, rule.lastIndexOf(")"));
        return wrap(new Subrule("", "", parseRules(split(ruleString), line, whiteSpace)), one_or_more, zero_or_more);
    }

    private List<Rule> parseRules(String[] words, String line, int whiteSpace) throws PapayaException {
        List<List<Rule>> list = new ArrayList<>();
        list.add(new ArrayList<>());
        for (int i = 0; i < words.length; i ++) {
            String string = words[i];

            if (whiteSpace == 0) {
                throw new PapayaException("invalid grammar file indentation: " + line);
            }

            if (string.isEmpty()) continue;

            boolean one_or_more = string.endsWith("+");
            boolean zero_or_more = string.endsWith("*");
            if (one_or_more || zero_or_more) {
                string = string.substring(0, string.length() - 1);
            }

            if (string.startsWith(":0")) {
                break;
            } else if (string.equals("|")) {
                list.add(new ArrayList<>());
            } else if (string.startsWith("(") && string.endsWith(")")) {
                list.get(list.size() - 1).add(subRule(string, one_or_more, zero_or_more, line, whiteSpace));
            } else if (string.startsWith("'") && string.endsWith("'")) {
                String actual_string = string.substring(1, string.length() - 1);
                list.get(list.size() - 1).add(string(actual_string, one_or_more, zero_or_more));
            } else {
                list.get(list.size() - 1).add(rule(string, one_or_more, zero_or_more));
            }
        }

        List<Rule> options = new ArrayList<>();
        if (list.size() == 1) {
            return list.get(0);
        }

        ParseRule rule = new ParseRule("");
        rule.clearOptions();
        for (List<Rule> option : list) {
            rule.addOptions(new Subrule("", "", option));
        }
        options.add(rule);

        return options;
    }

    private Rule rule(String string, boolean one_or_more, boolean zero_or_more) {
        Rule rule = new ParseRuleReference(string);

        if (one_or_more) {
            return new OneOrMore(rule);
        }

        if (zero_or_more) {
            return new ZeroOrMore(rule);
        }

        return rule;
    }

    private Rule string(String actual_string, boolean one_or_more, boolean zero_or_more) {
        Rule rule = new ParseLiteral(actual_string);

        if (one_or_more) {
            return new OneOrMore(rule);
        }

        if (zero_or_more) {
            return new ZeroOrMore(rule);
        }

        return rule;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (String key : map.keySet()) {
            builder.append(key).append(" ").append(map.get(key)).append("\n");
        }

        return builder.toString();
    }

    public void getRules(Map<String, ParseRule> references) {
        for (String key : map.keySet()) {
            references.put(key, ((GrammarRule) map.get(key)).asParseRule());
        }
    }
}
