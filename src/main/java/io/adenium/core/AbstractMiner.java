package io.adenium.core;

import io.adenium.core.consensus.CandidateBlock;
import io.adenium.core.consensus.MinedBlockCandidate;
import io.adenium.core.transactions.Transaction;
import io.adenium.exceptions.AdeniumException;
import io.adenium.network.messages.Inv;
import io.adenium.utils.ChainMath;

import java.util.ArrayList;
import java.util.Collection;

public abstract class AbstractMiner implements Runnable {
    private Address         miningAddress;

    public AbstractMiner(Address miningAddress) {
        this.miningAddress = miningAddress;
    }

    public abstract void mine(Block block) throws AdeniumException;

    @Override
    public void run() {
        while (Context.getInstance().isRunning()) {
            try {
                // get a reference parent block
                BlockIndex parent = Context.getInstance().getBlockChain().getBestBlock();
                // generate a new block
                BlockIndex block = Context.getInstance().getBlockChain().fork();
                // mint coins to our address
                block.getBlock().addTransaction(Transaction.newMintTransaction("", ChainMath.getReward(parent.getHeight()), miningAddress));
                // add transactions
                addTransactions(block.getBlock());

                // build the block and calculate all the remaining elements needed
                block.build();

                // mine the block
                mine(block.getBlock());

                if (block.getBlock().verifyProofOfWork()) {
                    // create a candidate
                    CandidateBlock candidateBlock = new MinedBlockCandidate(Context.getInstance(), block);

                    // submit the block
                    Context.getInstance().getBlockChain().suggest(candidateBlock);

                    // make a collection
                    Collection<byte[]> list = new ArrayList<>();
                    list.add(block.getHash());

                    // broadcast the block
                    Context.getInstance().getServer().broadcast(new Inv(Inv.Type.Block, list));
                }
            } catch (AdeniumException e) {
                e.printStackTrace();
            }
        }
    }

    protected void addTransactions(Block block) {
        Context.getInstance().getTransactionPool().fillBlock(block);
    }
}
