package io.adenium.network.messages;

import io.adenium.core.Block;
import io.adenium.core.BlockHeader;
import io.adenium.core.Context;
import io.adenium.exceptions.WolkenException;
import io.adenium.network.Message;
import io.adenium.network.Node;
import io.adenium.network.ResponseMetadata;
import io.adenium.network.Server;
import io.adenium.serialization.SerializableI;
import io.adenium.utils.VarInt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class RequestHeaders extends Message {
    private Set<byte[]> headers;

    public RequestHeaders(int version, Collection<byte[]> headers) {
        super(version, Flags.Request);
        this.headers    = new LinkedHashSet<>(headers);
    }

    private static Set<byte[]> toSet(byte[] hash) {
        Set<byte[]> set = new LinkedHashSet<>();
        set.add(hash);

        return set;
    }

    @Override
    public void executePayload(Server server, Node node) {
        Set<BlockHeader> headers = new LinkedHashSet<>();
        for (byte[] hash : this.headers) {
            BlockHeader header  = Context.getInstance().getDatabase().findBlockHeader(hash);

            if (header != null) {
                headers.add(header);
            }
        }

        // send the headers
        node.sendMessage(new HeaderList(Context.getInstance().getContextParams().getVersion(), headers, getUniqueMessageIdentifier()));
    }

    @Override
    public void writeContents(OutputStream stream) throws IOException {
        VarInt.writeCompactUInt32(headers.size(), false, stream);
        for (byte[] hash : headers) {
            stream.write(hash);
        }
    }

    @Override
    public void readContents(InputStream stream) throws IOException {
        int length = VarInt.readCompactUInt32(false, stream);

        for (int i = 0; i < length; i ++)
        {
            byte hash[] = new byte[Block.UniqueIdentifierLength];
            stream.read(hash);

            headers.add(hash);
        }
    }

    @Override
    public <Type> Type getPayload() {
        return (Type) headers;
    }

    @Override
    public ResponseMetadata getResponseMetadata() {
        return (msg)->{
            boolean isCorrectType = msg instanceof HeaderList;

            if (!isCorrectType) {
                return ResponseMetadata.ValidationBits.InvalidResponse | ResponseMetadata.ValidationBits.SpamfulResponse;
            }

            int response = 0;
            Collection<BlockHeader> headers = msg.getPayload();

            int checked = 0;
            for (BlockHeader header : headers) {
                if (this.headers.contains(header.getHashCode())) {
                    checked ++;
                }
            }

            if (headers.size() > this.headers.size()) {
                response |= ResponseMetadata.ValidationBits.SpamfulResponse;
                response |= ResponseMetadata.ValidationBits.InvalidResponse;
            }

            if (checked != this.headers.size()) {
                response |= ResponseMetadata.ValidationBits.PartialResponse;
                response |= ResponseMetadata.ValidationBits.InvalidResponse;
            }

            if (checked == this.headers.size() && response != 0) {
                response |= ResponseMetadata.ValidationBits.EntireResponse;
            }

            return response;
        };
    }

    @Override
    public <Type extends SerializableI> Type newInstance(Object... object) throws WolkenException {
        return (Type) new RequestHeaders(getVersion(), headers);
    }

    @Override
    public int getSerialNumber() {
        return Context.getInstance().getSerialFactory().getSerialNumber(RequestHeaders.class);
    }
}
