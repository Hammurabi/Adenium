package io.adenium.papaya.compiler;

import java.util.ArrayList;
import java.util.List;

public class ParseToken {
    private final String            tokenValue;
    private final LineInfo          lineInfo;
    private final List<ParseToken>  children;

    public ParseToken(String value, LineInfo info) {
        this.tokenValue = value;
        this.lineInfo = info;
        this.children = new ArrayList<>();
    }
}
