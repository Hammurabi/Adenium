package io.adenium.papaya.parser;

import io.adenium.papaya.compiler.AbstractSyntaxTree;
import io.adenium.papaya.compiler.TokenStream;
import io.adenium.exceptions.PapayaException;

public interface Parser {
    AbstractSyntaxTree parse(TokenStream stream) throws PapayaException;
}
