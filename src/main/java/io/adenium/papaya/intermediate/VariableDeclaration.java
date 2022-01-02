package io.adenium.papaya.intermediate;

import io.adenium.papaya.runtime.Scope;

public class VariableDeclaration implements Opcode {
    @Override
    public void execute(Scope scope) {
        scope.getStack().push(scope.getNullReference());
    }
}
