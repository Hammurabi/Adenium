package io.adenium.papaya.compiler.grammar;

import io.adenium.papaya.ParseRule;

import java.util.*;

public class GrammarRule {
    private String name;
    private Map<String, Object> map;
    private List<Object> array;

    GrammarRule(String name) {
        this.name = name;
        map = new HashMap<>();
        array = new ArrayList<>();
    }

    void add(String name, Object object) {
        map.put(name, object);
    }

    void add(Object object) {
        array.add(object);
    }

    public Object get(String name) {
        return map.get(name);
    }

    public String getString(String name) {
        return Objects.toString(map.get(name));
    }

    public GrammarRule getObject(String name) {
        return (GrammarRule) map.get(name);
    }

    public List<Object> asArray() {
            return array;
        }

    public Map<String, Object> asMap() {
        return map;
    }

    @Override
    public String toString() {
        return "GrammarRule{" +
                "map=" + map +
                ", array=" + array +
                '}';
    }

    public ParseRule asParseRule() {
        return new ParseRule(name, array);
    }
}