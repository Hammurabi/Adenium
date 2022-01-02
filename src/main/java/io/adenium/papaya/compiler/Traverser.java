package io.adenium.papaya.compiler;

import io.adenium.exceptions.PapayaException;
import io.adenium.papaya.parser.Node;

public interface Traverser {
    void onEnter(Node node, FunctionScope functionScope, CompilationScope compilationScope) throws PapayaException;
}