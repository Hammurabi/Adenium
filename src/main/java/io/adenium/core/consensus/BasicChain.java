package io.adenium.core.consensus;

import io.adenium.core.Block;
import io.adenium.core.BlockHeader;
import io.adenium.core.BlockIndex;
import io.adenium.core.Context;
import io.adenium.core.transactions.Transaction;
import io.adenium.network.messages.Inv;
import io.adenium.utils.*;
import io.adenium.network.Message;

import java.math.BigInteger;
import java.util.*;

public class BasicChain extends AbstractBlockChain {
    protected static final int                MaximumCandidateQueueSize     = 156_250;
    protected static final int                MaximumOrphanBlockQueueSize   = 156_250;
    protected static final int                MaximumStaleBlockQueueSize    = 375_000;

    // the current higest block in the chain
    private BlockIndex bestBlock;
    // contains blocks sent from peers.
    private HashQueue<CandidateBlock> candidateQueue;
    // contains blocks that are valid.
    private HashQueue<CandidateBlock>   orphanPool;
    // contains blocks that are valid.
    private Set<ByteArray>              staleBlocks;
    // keep track of broadcasted blocks.
    private byte                        lastBroadcast[];

    public BasicChain(Context context) {
        super(context);
        this.lastBroadcast  = new byte[32];
        this.candidateQueue = new PriorityHashQueue<>();
        this.orphanPool     = new LinkedHashQueue<>();
        this.staleBlocks    = new LinkedHashSet<>();
    }

    @Override
    protected final void setBlock(int height, BlockIndex block) {
        getContext().getDatabase().storeBlock(height, block);
    }

    @Override
    protected boolean containsBlock(int height) {
        BlockIndex bestBlock = getBestBlock();
        if (bestBlock == null) {
            return height == 0;
        }

        return bestBlock.getHeight() >= height;
    }

    @Override
    public boolean containsBlock(byte[] hash, boolean includeOprhans) {
        ByteArray array = ByteArray.wrap(hash);
        return getContext().getDatabase().checkBlockExists(hash) || staleBlocks.contains(array) || (includeOprhans && orphanPool.containsKey(array));
    }

    @Override
    protected byte[] getBlockHash(int height) {
        return getContext().getDatabase().findBlockHash(height);
    }

    @Override
    protected void removeBlock(byte[] hash) {
        getContext().getDatabase().deleteBlock(hash);
    }

    @Override
    protected void markRejected(byte[] hash) {
        getContext().getDatabase().markRejected(hash);
    }

    @Override
    protected boolean isRejected(byte[] hash) {
        return getContext().getDatabase().isRejected(hash);
    }

    @Override
    protected void addOrphan(CandidateBlock block) {
        getMutex().lock();
        try {
            orphanPool.add(block, block.getHash());
            orphanPool.removeTails(MaximumOrphanBlockQueueSize, (orphan, hash)->{
                orphan.destroy();
            });
        } finally {
            getMutex().unlock();
        }
    }

    @Override
    protected void initialize() {
        // load the last checkpoint.
        loadBestBlock();
    }

    @Override
    protected void loadBestBlock() {
        BlockIndex bestBlock = getContext().getDatabase().findTip();
        if (bestBlock == null) {
            bestBlock = getGenesisBlock();
            setBestBlock(bestBlock);
        } else {
            setBestBlockMember(bestBlock);
        }
    }

    private void setBestBlockMember(BlockIndex block) {
        getMutex().lock();
        try {
            this.bestBlock = block;
        } finally {
            getMutex().unlock();
        }
    }

    @Override
    protected void setBestBlock(BlockIndex block) {
        setBestBlockMember(bestBlock);
        getContext().getDatabase().setTip(block);
    }

    @Override
    protected boolean isBetterBlock(CandidateBlock candidate) {
        return candidate.getTotalChainWork().compareTo(getBestBlock().getTotalChainWork()) > 0;
    }

    @Override
    protected CandidateBlock getCandidate() {
        getMutex().lock();
        try {
            if (candidateQueue.hasElements()) {
                return candidateQueue.poll();
            }

            return null;
        } finally {
            getMutex().unlock();
        }
    }

    @Override
    public int getHeight() {
        BlockIndex best = getBestBlock();
        if (best == null) {
            return 0;
        }

        return best.getHeight();
    }

    @Override
    public ChainFork getFork(byte[] hash) {
        if (getContext().getDatabase().checkBlockExists(hash)) {
            // get the best block to identify the latest block in the chain.
            BlockIndex currentblock = getBestBlock();

            getMutex().lock();
            try {
                List<byte[]> hashes = new ArrayList<>();
                while (!Utils.equals(currentblock.getHash(), hash)) {
                    hashes.add(currentblock.getHash());
                    // load the previous block of the chain.
                    currentblock = currentblock.previousBlock();
                }

                // reverse the order of the list.
                Collections.reverse(hashes);

                // return a new chain-fork.
                return new ChainFork(hashes);
            } finally {
                getMutex().unlock();
            }
        }

        return null;
    }

    @Override
    public BlockIndex fork() {
        BlockIndex bestBlock = getBestBlock();
        if (bestBlock == null) {
            return null;
        }

        return bestBlock.generateNextBlock();
    }

    @Override
    public void queueStale(byte hash[]) {
        getMutex().lock();
        try {
            staleBlocks.add(ByteArray.wrap(hash));
            if (staleBlocks.size() > MaximumStaleBlockQueueSize) {
                Iterator<ByteArray> iterator = staleBlocks.iterator();
                int toRemove    = staleBlocks.size() - MaximumStaleBlockQueueSize;
                int removed     = 0;

                while (iterator.hasNext() && removed < toRemove) {
                    ByteArray oldHash = iterator.next();
                    iterator.remove();
                    removed ++;
                    getContext().getDatabase().deleteBlock(oldHash.getArray());
                }
            }
        } finally {
            getMutex().unlock();
        }
    }

    @Override
    public void undoStale(byte[] hash) {
        getMutex().lock();
        try {
            staleBlocks.remove(ByteArray.wrap(hash));
        } finally {
            getMutex().unlock();
        }
    }

    @Override
    public BlockIndex getBlock(byte[] hash) {
        return getContext().getDatabase().findBlock(hash);
    }

    @Override
    public BlockIndex getGenesisBlock() {
        Block genesisBlock = new Block(new byte[Block.UniqueIdentifierLength], getContext().getContextParams().getDefaultBits());
        genesisBlock.addTransaction(Transaction.newMintTransaction("", 1, null));
        BlockIndex genesisBlockIndex = new BlockIndex(genesisBlock, BigInteger.ZERO, 0);
        return genesisBlockIndex;
    }

    @Override
    public boolean isBlockStale(byte[] hash) {
        getMutex().lock();
        try {
            staleBlocks.contains(ByteArray.wrap(hash));
        } finally {
            getMutex().unlock();
        }

        return false;
    }

    @Override
    public List<byte[]> getStaleChain(byte[] hash) {
        getMutex().lock();
        try {
            List<byte[]> staleChain = new ArrayList<>();
            while (staleBlocks.contains(ByteArray.wrap(hash))) {
                if (getContext().getDatabase().checkBlockExists(hash)) {
                    BlockHeader header = getContext().getDatabase().findBlockHeader(hash);
                    staleChain.add(hash);
                    hash = header.getParentHash();
                } else {
                    return null;
                }
            }

            Collections.reverse(staleChain);
            for (byte staleHash[] : staleChain) {
                staleBlocks.remove(ByteArray.wrap(staleHash));
            }

            return staleChain;
        } finally {
            getMutex().unlock();
        }
    }

    @Override
    public void suggest(Set<CandidateBlock> blocks) {
        for (CandidateBlock block : blocks) {
            suggest(block);
        }
    }

    @Override
    public void suggest(CandidateBlock block) {
        getMutex().lock();
        try {
            candidateQueue.add(block, block.getHash());
            candidateQueue.removeTails(MaximumCandidateQueueSize, (candidateBlock, hash)->{
                candidateBlock.destroy();
            });
        } finally {
            getMutex().unlock();
        }
    }

    @Override
    protected void makeBest(CandidateBlock candidate) {
        // merge the block candidate.
        candidate.merge(this);
        // destroy this block candidate.
        candidate.destroy();
    }

    @Override
    protected void broadcastChain() {
        getMutex().lock();
        try {
            // retrieve the best block.
            BlockIndex bestBlock = getBestBlock();

            // nullpointer checks.
            if (bestBlock == null) {
                return;
            }

            // check that we did not broadcast this block before.
            if (Arrays.equals(lastBroadcast, bestBlock.getHash())) {
                return;
            }

            // make an inventory message.
            Message inv = new Inv(getContext().getContextParams().getVersion(), Inv.Type.Block, bestBlock.getHash());

            // broadcast the inventory message.
            getContext().getServer().broadcast(inv);

            // set the last broadcast block to the newly broadcast block hash.
            lastBroadcast = Arrays.copyOf(bestBlock.getHash(), lastBroadcast.length);
        } finally {
            getMutex().unlock();
        }
    }

    @Override
    public BlockIndex getBestBlock() {
        getMutex().lock();
        try {
            return bestBlock;
        } finally {
            getMutex().unlock();
        }
    }

    @Override
    public void run() {
        // initialize the chain.
        initialize();

        // to keep track of time.
        long lastCheck = System.currentTimeMillis();

        // enter the main loop.
        while ( getContext().isRunning() ) {
            // get a candidate block from the block pool.
            CandidateBlock candidate = getCandidate();

            // candidate could be null.
            if (candidate == null) {
                continue;
            }

            // check if the candidate is better than our block.
            if (isBetterBlock(candidate)) {
                // make the candidate our best block.
                makeBest(candidate);
            }

            if (System.currentTimeMillis() - lastCheck > 5000L) {
                broadcastChain();
                lastCheck = System.currentTimeMillis();
            }
        }
    }
}
