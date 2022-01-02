package io.adenium.papaya.runtime;

import io.adenium.papaya.compiler.AccessModifier;
import io.adenium.utils.ByteArray;

public class Member {
    private final ByteArray         identifier;
    private final AccessModifier accessModifier;
    private final boolean           isStatic;

    public Member(ByteArray identifier, AccessModifier accessModifier, boolean isStatic) {
        this.identifier = identifier;
        this.accessModifier = accessModifier;
        this.isStatic = isStatic;
    }
}
