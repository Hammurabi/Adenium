package io.adenium.core;

import io.adenium.core.events.*;
import io.adenium.serialization.SerializableI;
import io.adenium.serialization.SerializationFactory;
import io.adenium.utils.HashUtil;
import org.json.JSONObject;

public abstract class Event extends SerializableI {
    public static void register(SerializationFactory serializationFactory) {
        serializationFactory.registerClass(NewAccountEvent.class, new NewAccountEvent(new byte[20]));
        serializationFactory.registerClass(MintRewardEvent.class, new MintRewardEvent(new byte[20], 0));
        serializationFactory.registerClass(DepositFundsEvent.class, new DepositFundsEvent(new byte[20], 0));
        serializationFactory.registerClass(WithdrawFundsEvent.class, new WithdrawFundsEvent(new byte[20], 0));
        serializationFactory.registerClass(DepositFeesEvent.class, new DepositFeesEvent(new byte[20], 0));
        serializationFactory.registerClass(RegisterAliasEvent.class, new RegisterAliasEvent(new byte[20], 0));
    }

    public abstract void apply();
    public abstract void undo();
    public abstract byte[] getEventBytes();
    public byte[] eventId() {
        return HashUtil.sha256d(getEventBytes());
    }

    public abstract JSONObject toJson();
}
