package io.adenium.core.consensus;

import io.adenium.core.BlockIndex;
import io.adenium.core.Context;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public abstract class AbstractBlockChain implements Runnable {
    private final Context       context;
    private final ReentrantLock mutex;
    private AtomicBoolean       bIsReorg;

    public AbstractBlockChain(Context context) {
        this.context    = context;
        this.mutex      = new ReentrantLock();
        this.bIsReorg   = new AtomicBoolean(false);
    }

    public static AbstractBlockChain create(boolean pruned) {
        return null;
    }

    // return the context.
    public Context getContext() {
        return context;
    }
    // return the mutex.
    public ReentrantLock getMutex() {
        return mutex;
    }

    // attempt to make the 'candidate' into the best block, returns true if the operation is successful.
    protected abstract void makeBest(CandidateBlock candidate);
    // broadcast the chain information to all peers.
    protected abstract void broadcastChain();
    // returns the best block of this chain.
    public abstract BlockIndex getBestBlock();
    // returns true if the block is better than our current best block.
    protected abstract boolean isBetterBlock(CandidateBlock candidate);
    // get the next block from the block pool.
    protected abstract CandidateBlock getCandidate();
    // must be called before starting the main loop.
    protected abstract void initialize();
    // load the last saved block, or re-generate a genesis block if this is a fresh chain.
    protected abstract void loadBestBlock();
    // save a recovery checkpoint.
    protected abstract void setBestBlock(BlockIndex block);
    // set the block at 'height' to 'block', if a previous block exists then it should be replaced.
    protected abstract void setBlock(int height, BlockIndex block);
    // return true if the chain height is larger than or equal to 'height'.
    protected abstract boolean containsBlock(int height);
    // return true if block with hash 'hash' exists.
    public boolean containsBlock(byte hash[]) { return containsBlock(hash, false); }
    // return true if block with hash 'hash' exists.
    public abstract boolean containsBlock(byte hash[], boolean includeOrphans);
    // return the hash of block at height 'height'.
    protected abstract byte[] getBlockHash(int height);
    // remove the block with hash 'hash'.
    protected abstract void removeBlock(byte hash[]);
    // mark block as rejected, this block or it's children will never be considered valid.
    protected abstract void markRejected(byte hash[]);
    // returns true if this block has been rejected.
    protected abstract boolean isRejected(byte[] hash);
    // makes the block an orphan, meaning it does not have any ancestors.
    protected abstract void addOrphan(CandidateBlock block);
    // suggest these blocks and add them to the pool.
    public abstract void suggest(Set<CandidateBlock> blocks);
    // suggest this block and add it to the pool.
    public abstract void suggest(CandidateBlock block);
    // return the current chain height.
    public abstract int getHeight();
    // stales and returns all children of block with hash 'hash'.
    public abstract ChainFork getFork(byte[] hash);
    // generates a new block where height is 'bestblock.height + 1'
    public abstract BlockIndex fork();
    // sets "reorg" to true.
    public void setChainReorg(boolean isReorg) {
        bIsReorg.set(true);
    }
    // returns true if the chain is being reorganized.
    public boolean isChainReorganizing() {
        return bIsReorg.get();
    }
    // makes the block 'stale'.
    public abstract void queueStale(byte hash[]);
    // makes the block not 'stale'.
    public abstract void undoStale(byte hash[]);
    // return block by it's hash.
    public abstract BlockIndex getBlock(byte[] hash);
    // return the genesis block of this chain.
    public abstract BlockIndex getGenesisBlock();
    // return true if the block is stale.
    public abstract boolean isBlockStale(byte[] hash);
    // returns a block and all of it's parent's hashes that are considered 'stale'.
    public abstract List<byte[]> getStaleChain(byte[] hash);
}
