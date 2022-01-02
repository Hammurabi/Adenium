package io.adenium.network.messages;

import io.adenium.core.Block;
import io.adenium.core.Context;
import io.adenium.exceptions.WolkenException;
import io.adenium.network.Node;
import io.adenium.network.Server;
import io.adenium.serialization.SerializableI;
import io.adenium.utils.VarInt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class BlockList extends ResponseMessage {
    private Set<Block>      blocks;

    public BlockList(int version, Collection<Block> blocks, byte[] uniqueMessageIdentifier) {
        super(version, uniqueMessageIdentifier);
        this.blocks   = new LinkedHashSet<>(blocks);
    }

    @Override
    public void writeContents(OutputStream stream) throws IOException, WolkenException {
        VarInt.writeCompactUInt32(blocks.size(), false, stream);
        for (Block block : blocks) {
            block.write(stream);
        }
    }

    @Override
    public void readContents(InputStream stream) throws IOException, WolkenException {
        int length = VarInt.readCompactUInt32(false, stream);

        for (int i = 0; i < length; i ++)
        {
            try {
                Block block = Context.getInstance().getSerialFactory().fromStream(Context.getInstance().getSerialFactory().getSerialNumber(Block.class), stream);
                blocks.add(block);
            } catch (WolkenException e) {
                throw new IOException(e);
            }
        }
    }

    @Override
    public <Type> Type getPayload() {
        return (Type) blocks;
    }

    @Override
    public <Type extends SerializableI> Type newInstance(Object... object) throws WolkenException {
        return (Type) new BlockList(getVersion(), blocks, getUniqueMessageIdentifier());
    }

    @Override
    public int getSerialNumber() {
        return Context.getInstance().getSerialFactory().getSerialNumber(BlockList.class);
    }

    @Override
    public void execute(Server server, Node node) {
    }
}
