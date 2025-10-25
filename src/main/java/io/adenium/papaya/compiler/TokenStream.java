package io.adenium.papaya.compiler;

import java.util.*;

public class TokenStream implements Iterable<Token> {
    private List<Token> tokenList;
    private int index;
    private Queue<TokenPath> paths;

    public TokenStream() {
        this.tokenList = new ArrayList<>();
        this.paths = new PriorityQueue<>();
    }

    public TokenStream(List<Token> tokens, int index) {
        this.tokenList = tokens;
        this.index = index;
        this.paths = new PriorityQueue<>();
    }

    public void add(Token token) {
        tokenList.add(token);
    }

    @Override
    public String toString() {
        return tokenList.toString();
    }

    public boolean hasNext() {
        return index < tokenList.size();
    }

    public boolean hasPrevious() {
        return index >= 0;
    }

    public Token next() {
        return tokenList.get(index ++);
    }

    public Token peek() {
        return tokenList.get(index);
    }

    public Token previous() {
        return tokenList.get(index - 1);
    }

    public boolean matches(String ...pattern) {
        if (index + pattern.length <= tokenList.size()) {
            for (int i = 0; i < pattern.length; i ++) {
                if (!tokenList.get(i + index).getTokenType().equals(pattern[i])) {
                    return false;
                }
            }

            return true;
        }

        return false;
    }

    public boolean isEmpty() {
        return tokenList.isEmpty();
    }

    public void jump(int mark, String parseRule) {
        paths.add(new TokenPath(mark, get(mark), parseRule));
        this.index = mark;
    }

    public int mark() {
        return index;
    }

    @Override
    public Iterator<Token> iterator() {
        return tokenList.iterator();
    }

    public Token get(int index) {
        return tokenList.get(index);
    }

    public TokenPath getLongestPath() {
        TokenPath token = paths.poll();
        paths.clear();
        return token;
    }

    public class TokenPath implements Comparable<TokenPath> {
        private final int mark;
        private final Token token;
        private final String rule;

        private TokenPath(int mark, Token token, String rule) {
            this.mark = mark;
            this.token = token;
            this.rule = rule;
        }

        public Token getToken() {
            return token;
        }

        public String getRule() {
            return rule;
        }

        @Override
        public int compareTo(TokenStream.TokenPath o) {
            return mark > o.mark ? -1 : 1;
        }
    }
}
