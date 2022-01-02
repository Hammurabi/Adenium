package io.adenium.papaya.compiler;

import java.util.Set;

public class TokenBuilder {
    private StringBuilder builder;
    private int line;
    private int offset;
    private int whitespace;

    public TokenBuilder() {
        this("", 0, 0, 0);
    }

    public TokenBuilder(String string, int line, int offset, int whitespace) {
        this.builder = new StringBuilder(string);
        this.line = line;
        this.offset = offset;
        this.whitespace = whitespace;
    }

    public void append(char character) {
        builder.append(character);
    }

    public void append(String string) {
        builder.append(string);
    }

    @Override
    public String toString() {
        return builder.toString();
    }

    public int getLine() {
        return line;
    }

    public int getOffset() {
        return offset;
    }

    public int getWhitespace() {
        return whitespace;
    }

    public boolean isSymbol(Set<Character> symbolSet) {
        return symbolSet.contains(builder.charAt(0));
    }

    public boolean isEmpty() {
        return builder.length() == 0;
    }
}
