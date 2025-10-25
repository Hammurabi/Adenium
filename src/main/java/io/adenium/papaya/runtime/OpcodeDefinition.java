package io.adenium.papaya.runtime;

import io.adenium.exceptions.InvalidTransactionException;
import io.adenium.exceptions.PapayaException;
import io.adenium.utils.VoidCallableThrowsTY;

public class OpcodeDefinition {
    private String          name;
    private String          desc;
    private String          usage;
    private int             identifier;
    private int             numArgs;
    private boolean         vararg;
    private VoidCallableThrowsTY<Scope, PapayaException, InvalidTransactionException> callable;
    private long            weight;

    public OpcodeDefinition(String name, String desc, String usage, int identifier, boolean vararg, int numArgs, VoidCallableThrowsTY<Scope, PapayaException, InvalidTransactionException> callable, long weight) {
        this.name = name;
        this.desc = desc;
        this.usage= usage;
        this.vararg= vararg;
        this.numArgs= numArgs;
        this.callable= callable;
        this.identifier= identifier;
        this.weight = weight;
    }

    public void execute(Scope scope) throws PapayaException, InvalidTransactionException {
        callable.call(scope);
    }

    public OpcodeDefinition makeCopy() {
        return new OpcodeDefinition(name, desc, usage, identifier, vararg, numArgs, callable, weight);
    }

    protected void setIdentifier(int id) {
        this.identifier = id;
    }

    public String getName() {
        return name;
    }

    public int getIdentifier() {
        return identifier;
    }

    public String getDesc() {
        return desc;
    }

    public String getUsage() {
        return usage;
    }

    public boolean hasVarargs() {
        return vararg;
    }

    public int getNumArgs() {
        return numArgs;
    }

    public long getWeight() {
        return weight;
    }
}
