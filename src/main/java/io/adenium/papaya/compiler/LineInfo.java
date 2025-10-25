package io.adenium.papaya.compiler;

public class LineInfo {
    private final int line;
    private final int offset;

    public LineInfo(int line, int offset) {
        this.line = line;
        this.offset = offset;
    }

    public int getLine() {
        return line;
    }

    public int getOffset() {
        return offset;
    }

    @Override
    public String toString() {
        return "{" +
                "line=" + line +
                ", offset=" + offset +
                '}';
    }

    public boolean isNull() {
        return line < 0;
    }
}
