package io.adenium.core.consensus;

import io.adenium.core.*;
import io.adenium.network.messages.Inv;
import io.adenium.network.messages.RequestBlocks;
import io.adenium.network.messages.RequestHeadersBefore;
import io.adenium.exceptions.AdeniumTimeoutException;
import io.adenium.network.CheckedResponse;
import io.adenium.network.Message;
import io.adenium.network.Node;

import java.math.BigInteger;
import java.util.*;

public class PeerBlockCandidate extends CandidateBlock {
    private List<byte[]>        staleChain;
    private List<BlockHeader>   chain;
    private BlockHeader         header;
    private Node                sender;

    public PeerBlockCandidate(Context context, Node sender, BlockHeader header) {
        super(context);
        this.chain  = new ArrayList<>();
        this.sender = sender;
        this.header = header;
    }

    @Override
    public boolean verify() {
        // verify the header.
        if (!header.verifyProofOfWork()) return false;
        // get all block headers excluding the most recent common ancestor and verify.
        List<BlockHeader> ancestors = findCommonAncestors(getContext(), sender, header);
        // check that we received the headers.
        if (ancestors == null) return false;
        // pop the most recent common ancestor.
        byte commonAncestor[] = ancestors.get(0).getHashCode();
        // check if the ancestor is stale.
        if (getContext().getBlockChain().isBlockStale(commonAncestor)) {
            staleChain = getContext().getBlockChain().getStaleChain(commonAncestor);
            if (staleChain == null) {
                // propagate to the network so long as the block-header is valid.
                broadcast();
                return false;
            }
        }
        chain = ancestors;
        chain.add(header);
        // get all blocks.
        if (!downloadBlocks()) return false;
        // propagate to the network so long as the block is valid.
        broadcast();

        return true;
    }

    private void broadcast() {
        Message notify = new Inv(getContext().getContextParams().getVersion(), Inv.Type.Block, header.getHashCode());
        getContext().getServer().broadcast(notify, sender);
    }

    @Override
    public void merge(AbstractBlockChain target) {
        target.setChainReorg(true);
        byte mostRecentCommonAncestor[] = chain.get(chain.size() - 1).getParentHash();
        BlockMetadata commonAncestor    = getContext().getDatabase().findBlockMetaData(mostRecentCommonAncestor);
        int height                      = commonAncestor.getHeight();
        BlockHeader parent              = commonAncestor.getBlockHeader();
        BigInteger work                 = commonAncestor.getPreviousChainWork().add(commonAncestor.getBlockHeader().getWork());

        ChainFork previousChain         = target.getFork(mostRecentCommonAncestor);
        previousChain.undoChanges(target.getContext());

        for (int i = 0; i < chain.size(); i ++) {
            // increment the height.
            ++height;
            // get the block header.
            BlockHeader header = chain.get(i);
            // get the block from temp storage.
            Block block = getContext().getDatabase().findTempBlock(header.getHashCode());
            // get the ancestor of the last difficulty change.
            BlockHeader lastDiffCalc = null;
            // check if difficulty needs to be recalculated for this block.
            if (height > 0 && height % getContext().getContextParams().getDifficultyAdjustmentThreshold() == 0) {
                // determine the height of the last block that recalculated the difficulty.
                int heightAtLastDifficultyCalc = Math.max(0, height - getContext().getContextParams().getDifficultyAdjustmentThreshold());
                // determine whether or not this block is inside the main chain.
                if (heightAtLastDifficultyCalc <= commonAncestor.getHeight()) {
                    lastDiffCalc = getContext().getDatabase().findBlockHeader(target.getBlockHash(heightAtLastDifficultyCalc));
                } else {
                    lastDiffCalc = chain.get(heightAtLastDifficultyCalc - commonAncestor.getHeight());
                }
            }
            // verify the block is valid.
            if (block.verify(lastDiffCalc, parent, height)) {
                // apply the state change to the global state.
                for (Event event : block.getStateChange().getTransactionEvents()) {
                    event.apply();
                }
                // update the block parent.
                parent = block.getBlockHeader();
            } else {
                // invalidate the entire chain starting at block of index 'i'.
                invalidate(getContext(), i, chain);
                // close the connection with the peer.
                closeConnection();
                // loop backwards and undo all block changes.
                for (int j = i; j >= 0; j --) {
                    // get a list of all block events.
                    List<Event> events = getContext().getDatabase().getBlockEvents(chain.get(j).getHashCode());
                    // undo all the block state changes.
                    for (Event event : events) {
                        event.undo();
                    }
                    // remove the block from the chain.
                    target.removeBlock(chain.get(j).getHashCode());
                }
                // reapply the original chain.
                previousChain.redoChanges(getContext());
                // merge the original chain.
                previousChain.merge(target, commonAncestor.getHeight());
                return;
            }

            // make all the blocks 'stale'.
            previousChain.staleBlocks(target);
            // set the block to the new block index.
            target.setBlock(height, new BlockIndex(block, new BlockMetadata(header, 0, height, block.getTransactionCount(), block.getEventCount(), block.getTotalValue(), block.getFees(), work)));
            // add the block's work to the total work.
            work = work.add(block.getWork());
        }

        target.setChainReorg(false);
    }

    private boolean downloadBlocks() {
        // parent metadata.
        BlockMetadata metadata  = getContext().getDatabase().findBlockMetaData(chain.get(chain.size() - 1).getParentHash());

        // get the height.
        int height              = metadata.getHeight();

        // this should give us 8 blocks per message at (8mb).
        int blocksPerMessage    = getContext().getContextParams().getMaxMessageContentSize() / getContext().getContextParams().getMaxBlockSize();

        BlockHeader parent      = metadata.getBlockHeader();

        // loop all headers and download the blocks.
        for (int i = 0; i < chain.size(); i += blocksPerMessage) {
            List<byte[]> blocks = new ArrayList<>();
            for (int j = 0; j < blocksPerMessage; j ++) {
                if (j + i >= chain.size()) {
                    break;
                }

                blocks.add(chain.get(j + i).getHashCode());
            }

            Message request = new RequestBlocks(getContext().getContextParams().getVersion(), blocks);
            CheckedResponse response = null;

            try {
                response = sender.getResponse(request, getContext().getContextParams().getMessageTimeout(blocks.size() * getContext().getContextParams().getMaxBlockSize()));

                if (response.noErrors()) {
                    Collection<Block> bl = response.getMessage().getPayload();
                    for (Block block : bl) {
                        getContext().getDatabase().tempStoreBlock(block);
                    }
                } else {
                    closeConnection();
                    return false;
                }
            } catch (AdeniumTimeoutException e) {
                return false;
            }
        }

        return true;
    }

    private void invalidate(Context context, int inclusiveIndex, List<BlockHeader> chain) {
        for (int i = inclusiveIndex; i < chain.size(); i ++) {
            context.getDatabase().markRejected(chain.get(i).getHashCode());
        }
    }

    @Override
    public List<BlockHeader> getChain() {
        return chain;
    }

    @Override
    public boolean isChainAvailable() {
        return chain != null;
    }

    @Override
    public boolean destroy() {
        for (BlockHeader header : chain) {
            getContext().getDatabase().deleteTempBlock(header.getHashCode());
        }

        chain.clear();

        for (byte stale[] : staleChain) {
            getContext().getBlockChain().queueStale(stale);
        }

        return true;
    }

    @Override
    public byte[] getHash() {
        return header.getHashCode();
    }

    private void closeConnection() {
        try {
            sender.close();
        } catch (Exception e) {
        }
    }

    public static List<BlockHeader> findCommonAncestors(Context context, Node sender, BlockHeader best) {
        List<BlockHeader> ancestors = new ArrayList<>();

        if (context.getBlockChain().isRejected(best.getParentHash())) {
            context.getBlockChain().markRejected(best.getHashCode());
            return null;
        }

        if (context.getDatabase().checkBlockExists(best.getParentHash())) {
            return ancestors;
        }

        // request block headers
        Message request = new RequestHeadersBefore(context.getContextParams().getVersion(), best.getHashCode(), 1024, best);
        CheckedResponse response = null;

        try {
            response = sender.getResponse(request, context.getContextParams().getMessageTimeout(1024 * BlockHeader.Size));
            if (!response.noErrors()) {
                return null;
            }
        } catch (AdeniumTimeoutException e) {
            e.printStackTrace();
        }

        Stack<Collection<BlockHeader>> ancestorRequests = new Stack<>();

        if (response != null) {
            Collection<BlockHeader> headerCollection = response.getMessage().getPayload();
            List<BlockHeader> headers = new ArrayList<>(headerCollection);

            while (headers != null) {
                int lastCommonAncestor = -1;

                for (int i = 0; i < headers.size(); i ++) {
                    // if an ancestor of this block is rejected, then we reject it and all of it's successors.
                    if (context.getBlockChain().isRejected(headers.get(i).getHashCode())) {
                        context.getBlockChain().markRejected(headers.get(i).getHashCode());

                        // reject the rest of the blocks we collected.
                        while (!ancestorRequests.isEmpty()) {
                            for (BlockHeader header : ancestorRequests.pop()) {
                                context.getBlockChain().markRejected(header.getHashCode());
                            }
                        }

                        return null;
                    }

                    if (isCommonAncestor(context, headers.get(i))) {
                        lastCommonAncestor = i;
                    }

                    if (!headers.get(i).verifyProofOfWork()) {
                        // set the best block as rejected.
                        context.getBlockChain().markRejected(best.getHashCode());
                        // set the current block as rejected.
                        context.getBlockChain().markRejected(headers.get(i).getHashCode());

                        // reject all blocks that come after it.
                        for (int j = i + 1; j < headers.size(); j ++) {
                            context.getBlockChain().markRejected(headers.get(j).getHashCode());
                        }

                        // reject the rest of the blocks we collected.
                        while (!ancestorRequests.isEmpty()) {
                            for (BlockHeader header : ancestorRequests.pop()) {
                                context.getBlockChain().markRejected(header.getHashCode());
                            }
                        }

                        return null;
                    }
                }

                if (lastCommonAncestor != -1) {
                    for (int i = lastCommonAncestor + 1; i < headers.size(); i ++) {
                        ancestors.add(headers.get(i));
                    }

                    while (!ancestorRequests.isEmpty()) {
                        ancestors.addAll(ancestorRequests.pop());
                    }

                    return ancestors;
                }

                // push the header collection into the stack.
                ancestorRequests.push(headers);

                // find older ancestor
                request = new RequestHeadersBefore(context.getContextParams().getVersion(), headers.get(headers.size() - 1).getHashCode(), 4096, headers.get(headers.size() - 1));
                response = null;

                try {
                    response = sender.getResponse(request, context.getContextParams().getMessageTimeout(4096 * BlockHeader.Size));
                    if (!response.noErrors()) {
                        return null;
                    }
                } catch (AdeniumTimeoutException e) {
                    e.printStackTrace();
                }

                if (response != null) {
                    headerCollection = response.getMessage().getPayload();
                    headers = new ArrayList<>(headerCollection);
                }
            }
        }

        return null;
    }

    private static boolean isCommonAncestor(Context context, BlockHeader blockHeader) {
        return context.getBlockChain().containsBlock(blockHeader.getHashCode());
    }
}
