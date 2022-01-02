package io.adenium.papaya.parser;

import io.adenium.papaya.compiler.TokenStream;
import io.adenium.exceptions.PapayaException;

public interface Rule {
    Node parse(final TokenStream stream, DynamicParser rules) throws PapayaException;
    int length(DynamicParser parser) throws PapayaException;

    String toSimpleString(DynamicParser parser);
}
