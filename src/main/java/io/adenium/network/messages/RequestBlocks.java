package io.adenium.network.messages;

import io.adenium.core.Block;
import io.adenium.core.BlockIndex;
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

public class RequestBlocks extends Message {
    private Set<byte[]> blocks;

    public RequestBlocks(int version, Collection<byte[]> blocks) {
        super(version, Flags.Request);
        this.blocks = new LinkedHashSet<>(blocks);
    }

    public RequestBlocks(int version, byte[] hash) {
        this(version, toSet(hash));
    }

    private static Set<byte[]> toSet(byte[] hash) {
        Set<byte[]> set = new LinkedHashSet<>();
        set.add(hash);

        return set;
    }

    @Override
    public void executePayload(Server server, Node node) {
        Set<Block> blocks = new LinkedHashSet<>();
        for (byte[] hash : this.blocks) {
            BlockIndex block    = Context.getInstance().getDatabase().findBlock(hash);

            if (block != null) {
                blocks.add(block.getBlock());
            }
        }

        // send the blocks
        node.sendMessage(new BlockList(Context.getInstance().getContextParams().getVersion(), blocks, getUniqueMessageIdentifier()));
    }

    @Override
    public void writeContents(OutputStream stream) throws IOException {
        VarInt.writeCompactUInt32(blocks.size(), false, stream);
        for (byte[] hash : blocks)
        {
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

            blocks.add(hash);
        }
    }

    @Override
    public <Type> Type getPayload() {
        return (Type) blocks;
    }

    @Override
    public ResponseMetadata getResponseMetadata() {
        return (msg)->{
            boolean isCorrectType = msg instanceof BlockList;

            if (!isCorrectType) {
                return ResponseMetadata.ValidationBits.InvalidResponse | ResponseMetadata.ValidationBits.SpamfulResponse;
            }

            int response = 0;
            Collection<BlockIndex> blocks = msg.getPayload();

            int checked = 0;
            for (BlockIndex block : blocks) {
                if (this.blocks.contains(block.getHash())) {
                    checked ++;
                }
            }

            if (blocks.size() > this.blocks.size()) {
                response |= ResponseMetadata.ValidationBits.SpamfulResponse;
                response |= ResponseMetadata.ValidationBits.InvalidResponse;
            }

            if (checked != this.blocks.size()) {
                response |= ResponseMetadata.ValidationBits.PartialResponse;
                response |= ResponseMetadata.ValidationBits.InvalidResponse;
            }

            if (checked == this.blocks.size() && response != 0) {
                response |= ResponseMetadata.ValidationBits.EntireResponse;
            }

            return response;
        };
    }

    @Override
    public <Type extends SerializableI> Type newInstance(Object... object) throws WolkenException {
        return (Type) new RequestBlocks(getVersion(), blocks);
    }

    @Override
    public int getSerialNumber() {
        return Context.getInstance().getSerialFactory().getSerialNumber(RequestBlocks.class);
    }
}
