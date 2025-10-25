package io.adenium.core.consensus;

import io.adenium.core.Context;
import io.adenium.core.Event;

import java.util.List;

public class ChainFork {
    private List<byte[]> chain;

    public ChainFork(List<byte[]> chain) {
        this.chain = chain;
    }

    // undo the changes this chain has made to the global state.
    public void undoChanges(Context context) {
        for (byte[] hash : chain) {
            // get the state change of this block.
            List<Event> events = context.getDatabase().getBlockEvents(hash);
            // apply the state changes of this block to the main chain.
            events.forEach(Event::undo);
        }
    }

    // redo the changes this chain has made to the global state.
    public void redoChanges(Context context) {
        for (byte[] hash : chain) {
            // get the state change of this block.
            List<Event> events = context.getDatabase().getBlockEvents(hash);
            // apply the state changes of this block to the main chain.
            events.forEach(Event::apply);
        }
    }

    public void staleBlocks(AbstractBlockChain target) {
        for (byte hash[] : chain) {
            target.queueStale(hash);
        }
    }

    public void merge(AbstractBlockChain target, int height) {
        for (byte[] hash : chain) {
            target.setBlock(++ height, target.getBlock(hash));
        }
    }

    public byte[] getHash() {
        return chain.get(chain.size() - 1);
    }

    public void deleteBlocks() {
        for (byte[] hash : chain) {
            Context.getInstance().getBlockChain().removeBlock(hash);
        }
    }
}
