package io.adenium.network.messages;

import io.adenium.core.Block;
import io.adenium.core.BlockHeader;
import io.adenium.core.BlockIndex;
import io.adenium.core.Context;
import io.adenium.exceptions.WolkenException;
import io.adenium.network.Message;
import io.adenium.network.Node;
import io.adenium.network.ResponseMetadata;
import io.adenium.network.Server;
import io.adenium.serialization.SerializableI;
import io.adenium.utils.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public class RequestHeadersBefore extends Message {
    private byte        hash[];
    private int         count;

    // not serialized
    private BlockHeader checkAgainst;

    public RequestHeadersBefore(int version, byte hash[], int count, BlockHeader checkAgainst) {
        super(version, Flags.Request);
        this.hash   = hash;
        this.count  = count;
        this.checkAgainst = checkAgainst;
    }

    private static Set<byte[]> toSet(byte[] hash) {
        Set<byte[]> set = new LinkedHashSet<>();
        set.add(hash);

        return set;
    }

    @Override
    public void executePayload(Server server, Node node) {
        Set<BlockHeader> headers    = new LinkedHashSet<>();

        // fetch the block header
        BlockIndex index            = Context.getInstance().getDatabase().findBlock(hash);

        // if it doesn't exist then respond with an error
        if (index == null) {
            node.sendMessage(new FailedToRespondMessage(Context.getInstance().getContextParams().getVersion(), FailedToRespondMessage.ReasonFlags.CouldNotFindRequestedData, getUniqueMessageIdentifier()));
            return;
        }

        int earliestHeader          = Math.max(0, index.getHeight() - count);

        for (int i = earliestHeader; i < index.getHeight(); i ++) {
            BlockHeader header      = Context.getInstance().getDatabase().findBlockHeader(i);
            // database internal error
            // this should not happen
            if (header == null) {
                node.sendMessage(new FailedToRespondMessage(Context.getInstance().getContextParams().getVersion(), FailedToRespondMessage.ReasonFlags.CouldNotFindRequestedData, getUniqueMessageIdentifier()));
                return;
            }
        }

        // send the headers
        node.sendMessage(new HeaderList(Context.getInstance().getContextParams().getVersion(), headers, getUniqueMessageIdentifier()));
    }

    @Override
    public void writeContents(OutputStream stream) throws IOException {
        stream.write(hash);
        Utils.writeInt(count, stream);
    }

    @Override
    public void readContents(InputStream stream) throws IOException {
        checkFullyRead(stream.read(hash), Block.UniqueIdentifierLength);
        count = Utils.readInt(stream);
    }

    @Override
    public <Type> Type getPayload() {
        return null;
    }

    @Override
    public ResponseMetadata getResponseMetadata() {
        return (msg)->{
            boolean isCorrectType = msg instanceof HeaderList;

            if (!isCorrectType) {
                return ResponseMetadata.ValidationBits.InvalidResponse | ResponseMetadata.ValidationBits.SpamfulResponse;
            }

            int response    = 0;
            Collection<BlockHeader> headers = msg.getPayload();
            if (headers.size() > count || headers.isEmpty()) {
                response |= ResponseMetadata.ValidationBits.SpamfulResponse;
                response |= ResponseMetadata.ValidationBits.InvalidResponse;
            }

            Iterator<BlockHeader> iterator = new ArrayList<>(headers).iterator();
            if (iterator.hasNext()) {
                BlockHeader header = iterator.next();

                while (iterator.hasNext()) {
                    BlockHeader next = iterator.next();

                    if (!Utils.equals(header.getHashCode(), next.getParentHash())) {
                        return response | ResponseMetadata.ValidationBits.SpamfulResponse | ResponseMetadata.ValidationBits.InvalidResponse;
                    }

                    if (!iterator.hasNext()) {
                        if (!Utils.equals(header.getHashCode(), checkAgainst.getParentHash())) {
                            return response | ResponseMetadata.ValidationBits.SpamfulResponse | ResponseMetadata.ValidationBits.InvalidResponse;
                        }
                    }
                }
            }

            return response;
        };
    }

    @Override
    public <Type extends SerializableI> Type newInstance(Object... object) throws WolkenException {
        return (Type) new RequestHeadersBefore(getVersion(), new byte[Block.UniqueIdentifierLength], 0, new BlockHeader());
    }

    @Override
    public int getSerialNumber() {
        return Context.getInstance().getSerialFactory().getSerialNumber(RequestHeadersBefore.class);
    }
}
