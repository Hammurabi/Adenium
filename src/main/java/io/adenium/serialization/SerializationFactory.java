package io.adenium.serialization;

import io.adenium.core.*;
import io.adenium.core.transactions.Transaction;
import io.adenium.crypto.ec.RecoverableSignature;
import io.adenium.network.messages.*;
import io.adenium.core.assets.Asset;
import io.adenium.exceptions.InvalidSerialNumberException;
import io.adenium.exceptions.AdeniumException;
import io.adenium.network.AddressList;
import io.adenium.network.Message;
import io.adenium.network.NetAddress;
import io.adenium.network.VersionInformation;
import io.adenium.utils.VarInt;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

public class SerializationFactory {
    private Map<Class<?>, Integer>      classMagicReferences;
    private Map<Integer, SerializableI> magicReferences;

    public SerializationFactory()
    {
        classMagicReferences = new HashMap<>();
        magicReferences = new HashMap<>();
    }

    public static void register(SerializationFactory serializationFactory) throws UnknownHostException, AdeniumException {
        Transaction.register(serializationFactory);
        Asset.register(serializationFactory);

        serializationFactory.registerClass(BlockHeader.class, new BlockHeader());
        serializationFactory.registerClass(Block.class, new Block());
        serializationFactory.registerClass(BlockIndex.class, new BlockIndex());
        serializationFactory.registerClass(PrunedBlock.class, new PrunedBlock());
        serializationFactory.registerClass(BlockMetadata.class, new BlockMetadata());

        Event.register(serializationFactory);

        // messages
        serializationFactory.registerClass(NetAddress.class, new NetAddress(InetAddress.getLocalHost(), 0, 0));
        serializationFactory.registerClass(VersionMessage.class, new VersionMessage());
        serializationFactory.registerClass(VerackMessage.class, new VerackMessage());
        serializationFactory.registerClass(VersionInformation.class, new VersionInformation());
        serializationFactory.registerClass(CheckoutMessage.class, new CheckoutMessage(0));

        serializationFactory.registerClass(BlockList.class, new BlockList(0, new LinkedHashSet<>(), new byte[Message.UniqueIdentifierLength]));
        serializationFactory.registerClass(FailedToRespondMessage.class, new FailedToRespondMessage(0, 0, new byte[Message.UniqueIdentifierLength]));
        serializationFactory.registerClass(FoundCommonAncestor.class, new FoundCommonAncestor(new byte[Block.UniqueIdentifierLength], new byte[Message.UniqueIdentifierLength]));
        serializationFactory.registerClass(HeaderList.class, new HeaderList(0, new LinkedHashSet<>(), new byte[Message.UniqueIdentifierLength]));
        serializationFactory.registerClass(Inv.class, new Inv(0, 0, new LinkedHashSet<>()));
        serializationFactory.registerClass(RequestBlocks.class, new RequestBlocks(0, new LinkedHashSet<>()));
        serializationFactory.registerClass(RequestCommonAncestorChain.class, new RequestCommonAncestorChain(0, new Ancestors(new byte[Block.UniqueIdentifierLength])));
        serializationFactory.registerClass(RequestHeaders.class, new RequestHeaders(0, new LinkedHashSet<>()));
        serializationFactory.registerClass(RequestHeadersBefore.class, new RequestHeadersBefore(0, new byte[Block.UniqueIdentifierLength], 0, new BlockHeader()));
        serializationFactory.registerClass(RequestInv.class, new RequestInv(0));
        serializationFactory.registerClass(RequestTransactions.class, new RequestTransactions(0, new LinkedHashSet<>()));
        serializationFactory.registerClass(TransactionList.class, new TransactionList(0, new LinkedHashSet<>(), new byte[Message.UniqueIdentifierLength]));
        serializationFactory.registerClass(AddressList.class, new AddressList(0, new LinkedHashSet<>()));

        serializationFactory.registerClass(Account.class, new Account());
        serializationFactory.registerClass(RecoverableSignature.class, new RecoverableSignature());
        serializationFactory.registerClass(Ancestors.class, new Ancestors(new byte[Block.UniqueIdentifierLength]));
    }

    /*
        register an instance of serializable object.
     */
    public void registerClass(Class<?> classType, SerializableI serializableInstance)
    {
        int magic = 1 + classMagicReferences.size();
        classMagicReferences.put(classType, magic);
        magicReferences.put(magic, serializableInstance);
    }

    public <Type extends SerializableI> Type fromStream(Class<?> classType, InputStream stream) throws IOException, AdeniumException {
        return fromStream(getSerialNumber(classType), stream);
    }

    public <Type extends SerializableI> Type fromStream(InputStream stream) throws IOException, AdeniumException {
        int magic = VarInt.readCompactUInt32(false, stream);

        return fromStream(magic, stream);
    }

    public <Type extends SerializableI> Type fromStream(int magic, InputStream stream) throws IOException, AdeniumException {
        SerializableI serializable  = magicReferences.get(validateMagicNumber(magic));

        Type result                 = serializable.newInstance();
        result.read(stream);

        return result;
    }

    private int validateMagicNumber(int magic) throws InvalidSerialNumberException {
        if (magicReferences.containsKey(magic))
        {
            return magic;
        }

        throw new InvalidSerialNumberException("'" + magic + "' is an invalid serial number.");
    }

    public int getSerialNumber(Class<?> classType) {
        return classMagicReferences.get(classType);
    }

    public <Type extends SerializableI> Type fromBytes(byte[] bytes, Class<?> theClass) throws IOException, AdeniumException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        try {
            return fromStream(theClass, inputStream);
        } finally {
            inputStream.close();
        }
    }
}
