package io.adenium.papaya.intermediate;

import io.adenium.exceptions.PapayaException;
import io.adenium.papaya.compiler.Struct;
import io.adenium.papaya.runtime.Scope;

public class Add implements Opcode {
    private final Opcode a;
    private final Opcode b;

    public Add(Opcode a, Opcode b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public void execute(Scope scope) throws PapayaException {
        b.execute(scope);
        a.execute(scope);
        scope.callOperator(Struct.Operator.Add);
    }
}
