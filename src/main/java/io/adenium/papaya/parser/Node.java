package io.adenium.papaya.parser;

import io.adenium.papaya.compiler.LineInfo;
import io.adenium.papaya.compiler.Token;

import java.util.*;

public class Node implements Iterable<Node> {
    private String tokenRule;
    private String ruleVariation;
    private final Token token;
    private final List<Node> children;

    public Node(String tokenRule, String variation) {
        this(tokenRule, variation, new Token("", "", new LineInfo(-2, -2)));
    }

    public Node(String tokenRule, String ruleVariation, Token token) {
        this(tokenRule, ruleVariation, token, new ArrayList<>());
    }

    public Node(String tokenRule, String ruleVariation, Token token, List<Node> children) {
        this.tokenRule = tokenRule;
        this.ruleVariation = ruleVariation;
        this.token = token;
        this.children = children;
    }

    public String getTokenRule() {
        return tokenRule;
    }

    public String getRuleVariation() {
        return ruleVariation;
    }

    public Token getToken() {
        return token;
    }

    public List<Node> getChildren() {
        return children;
    }

    public void add(Node token) {
        children.add(token);
    }

    public void toString(int indent, StringBuilder builder) {
        StringBuilder indentation = new StringBuilder();
        for (int i = 0; i < indent; i ++) {
            indentation.append("\t");
        }

        builder.append(indentation);
        builder.append(tokenRule);

        if (!ruleVariation.equals("default")) {
            builder.append(" ").append(ruleVariation);
        }
        if (!token.isEmpty()) {
            builder.append(" ").append(token.getTokenValue());
        }
        builder.append(":").append("\n");
        for (Node token : children) {
            token.toString(indent + 1, builder);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        toString(0, builder);

        return builder.toString();
    }

    public LineInfo getLineInfo() {
        return token.getLineInfo();
    }

    public void add(List<Node> list) {
        children.addAll(list);
    }

    public void cleanUp() {
        for (Node node : children) {
            node.cleanUp();
        }

        if (tokenRule.isEmpty() && children.size() == 1) {
            switchWithChild();
        }
    }

    private void switchWithChild() {
        Node child = children.get(0);
        tokenRule = child.tokenRule;
        ruleVariation = child.ruleVariation;
        List<Node> nodes = child.children;
        children.clear();
        children.addAll(nodes);
    }

    @Override
    public Iterator<Node> iterator() {
        return children.iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof String) {
            return tokenRule.equals(o);
        }

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return Objects.equals(tokenRule, node.tokenRule) && Objects.equals(ruleVariation, node.ruleVariation) && Objects.equals(token, node.token) && Objects.equals(children, node.children);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tokenRule, ruleVariation, token, children);
    }

    public boolean isNull() {
        return children.isEmpty() && tokenRule.isEmpty() && ruleVariation.isEmpty();
    }

    public Node get(String type) {
        for (Node node : this) {
            if (node.equals(type)) {
                return node;
            }
        }

        return new Node("", "");
    }

    public List<Node> getAll(String type) {
        List<Node> nodes = new ArrayList<>();
        for (Node node : this) {
            if (node.equals(type)) {
                nodes.add(node);
            }
        }

        return nodes;
    }

    public Node at(int index) {
        return children.get(index);
    }

    public Node getLast(String type) {
        Node last = null;

        for (Node node : this) {
            if (node.equals(type)) {
                last = node;
            }
        }

        return last;
    }

    public boolean isEmpty() {
        return tokenRule.equals("empty*");
    }
}
