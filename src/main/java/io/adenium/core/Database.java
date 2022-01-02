package io.adenium.core;

import io.adenium.core.transactions.Transaction;
import io.adenium.encoders.Base16;
import io.adenium.exceptions.WolkenException;
import io.adenium.serialization.SerializableI;
import io.adenium.utils.FileService;
import io.adenium.utils.Utils;
import io.adenium.wallet.Wallet;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.impl.Iq80DBFactory;

import java.io.*;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class Database {
    private DB              database;
    private FileService location;
    private ReentrantLock   mutex;

    private final static byte[]
    AccountPrefix           = new byte[] { 'a' },
    AliasPrefix             = new byte[] { 'A' },
    ChainTipPrefix          = new byte[] { 'q' },
    BlockPrefix             = new byte[] { 'b' },
    FullBlockPrefix         = new byte[] { 'c' },
    PrunedBlockPrefix       = new byte[] { 'p' },
    CompressedBlockPrefix   = new byte[] { 's' },
    BlockIndexPrefix        = new byte[] { 'i' },
    BlockFile               = new byte[] { 'f' },
    TransactionPrefix       = new byte[] { 't' },
    RejectedBlockPrefix     = new byte[] { 'r' },
    WalletPrefix            = new byte[] { 'w' },
    TempStorage             = new byte[] { 'l' };

    public Database(FileService location) throws IOException {
        database= Iq80DBFactory.factory.open(location.newFile(".db").file(), new Options());
        this.location = location;
        mutex   = new ReentrantLock();
    }

    public void setTip(BlockIndex block) {
        put(ChainTipPrefix, Utils.concatenate(Utils.concatenate(block.getHash(), Utils.takeApart(block.getHeight()))));
    }

    public boolean checkBlockExists(byte[] hash) {
        return location.newFile(".chain").newFile(Base16.encode(hash)).exists();
    }

    public boolean checkBlockExists(int height) {
        byte hash[] = get(Utils.concatenate(BlockIndexPrefix, Utils.takeApart(height)));

        return hash != null;
    }

    public void storeTransaction(byte[] hash, Transaction transaction, int block) {
        // store the transaction metadata
        put(Utils.concatenate(TransactionPrefix, hash), Utils.concatenate(
                                                                Utils.takeApart(transaction.getVersion()),
                                                                Utils.takeApart(transaction.getTransactionValue()),
                                                                Utils.takeApart(transaction.getTransactionFee()),
                                                                Utils.takeApart(block)));
    }

    public Transaction findTransaction(byte[] hash) {
        byte bytes[] = get(Utils.concatenate(TransactionPrefix, hash));

        if (bytes == null) {
            return null;
        }

        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);

            return Context.getInstance().getSerialFactory().fromStream(inputStream);
        } catch (WolkenException | IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public BlockIndex findBlock(byte[] hash) {
        BlockMetadata metadata = findBlockMetaData(hash);

        if (metadata == null) {
            return null;
        }

        byte compressed[] = get(Utils.concatenate(CompressedBlockPrefix, hash));

        if (compressed == null) {
            return null;
        }

        Block block = null;
        try {
            block = new Block().fromBytes(compressed);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (WolkenException e) {
            e.printStackTrace();
        }

        if (block == null) {
            return null;
        }

        return new BlockIndex(block, metadata);
    }

    public List<Event> getBlockEvents(byte[] hash) {
        BlockIndex block = findBlock(hash);
        if (block != null) {
            return block.getStateChange().getTransactionEvents();
        }

        return null;
    }

    public void storePrunedBlock(int height, BlockIndex block) {
        // get a reference of the hash since we will keep reusing it.
        byte hash[]   = block.getHash();

        // 80 bytes representing the block header.
        byte header[] = block.getBlock().getHeaderBytes();

        // 28 bytes representing the height, number of transactions, number of events total value, and total fees.
        byte metadt[] = Utils.concatenate(
                Utils.takeApart(height),
                Utils.takeApart(block.getBlock().getTransactionCount()),
                Utils.takeApart(block.getBlock().getEventCount()),
                Utils.takeApartLong(block.getBlock().getTotalValue()),
                Utils.takeApartLong(block.getBlock().getFees())
        );

        try {
            // get the raw uncompressed block as byte array.
            byte pruned[] = block.getPruned().asByteArray();

            // store the info that block of height 'height' is block of hash 'hash'.
            put(Utils.concatenate(BlockIndexPrefix, Utils.takeApart(height)), hash);

            // store the header along with the height and number of transactions and number of events.
            put(Utils.concatenate(BlockPrefix, hash), Utils.concatenate(header, metadt));

            // store the actual compressed block data.
            put(Utils.concatenate(PrunedBlockPrefix, hash), pruned);
        } catch (WolkenException e) {
            e.printStackTrace();
        }
    }

    public void storeBlock(int height, BlockIndex block) {
        // get a reference of the hash since we will keep reusing it.
        byte hash[]   = block.getHash();

        try {
            // 80 bytes representing the block header.
            // 28 bytes representing the height, number of transactions, number of events total value, and total fees.
            // 1 + 0-32 bytes representing the total chainwork leading up to the block.
            BlockMetadata blockMeta = block.getMetadata();

            // prepare a byte array output stream for quickly serializing the block structure to a byte array.
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            // we serialize the block into a deflater output stream with BEST_COMPRESSION, which is slow
            // but according to benchmarks, it should take around 7ms per block to deflate, and around 14ms
            // to inflate.
//            DeflaterOutputStream outputStream = new DeflaterOutputStream(byteArrayOutputStream, new Deflater(Deflater.BEST_COMPRESSION));

            // write the block (LOCALLY) to the output stream.
            block.getBlock().write(outputStream, true);

//            outputStream.flush();
//            outputStream.close();

//            // store the actual transactions associated with this block
//            for (Transaction transaction : block.getBlock()) {
//                storeTransaction(transaction.getHash(), transaction, block.getHeight());
//            }

            // store the info that block of height 'height' is block of hash 'hash'.
            put(Utils.concatenate(BlockIndexPrefix, Utils.takeApart(height)), hash);

            // store the header along with the height and number of transactions and number of events.
            put(Utils.concatenate(BlockPrefix, hash), blockMeta);

            // store the actual compressed block data.
            put(Utils.concatenate(CompressedBlockPrefix, hash), outputStream.toByteArray());
        } catch (WolkenException | IOException e) {
            e.printStackTrace();
        }
    }

    private BlockStore findBlockStore(int blockStore) {
        byte bytes[] = get(Utils.concatenate(BlockFile, Utils.takeApart(blockStore)));
        if (bytes == null) {
            return null;
        }

        return new BlockStore(bytes);
    }

    private boolean checkBlockFileExists(int blockFile) {
        return false;
    }

    public BlockIndex findBlock(int height) {
        byte hash[] = get(Utils.concatenate(BlockIndexPrefix, Utils.takeApart(height)));

        if (hash == null) {
            return null;
        }

        return findBlock(hash);
    }

    public void deleteBlock(int height) {
        byte key[]  = Utils.concatenate(BlockIndexPrefix, Utils.takeApart(height));
        byte hash[] = get(key);

        if (hash != null) {
            deleteBlock(hash);
        }
    }

    public void deleteBlock(byte hash[]) {
        BlockMetadata metadata = findBlockMetaData(hash);
        if (metadata != null) {
            remove(Utils.concatenate(BlockIndexPrefix, Utils.takeApart(metadata.getHeight())));
            remove(Utils.concatenate(BlockPrefix, hash));
            remove(Utils.concatenate(CompressedBlockPrefix, hash));
        }
    }

    public <Type extends SerializableI> Type get(byte[] k, Class<?> theClass) {
        byte bytes[] = get(k);
        if (bytes == null) {
            return null;
        }

        try {
            return Context.getInstance().getSerialFactory().fromBytes(bytes, theClass);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (WolkenException e) {
            e.printStackTrace();
        }

        return null;
    }

    public byte[] get(byte[] k) {
        mutex.lock();
        try {
            return database.get(k);
        } finally {
            mutex.unlock();
        }
    }

    public void put(byte[] k, byte[] v) {
        mutex.lock();
        try {
            database.put(k, v);
        } finally {
            mutex.unlock();
        }
    }

    public void put(byte[] k, SerializableI v) {
        mutex.lock();
        try {
            database.put(k, v.asByteArray());
        } finally {
            mutex.unlock();
        }
    }

    public void remove(byte[] k) {
        mutex.lock();
        try {
            database.delete(k);
        } finally {
            mutex.unlock();
        }
    }

    public BlockIndex findTip() {
        byte tip[] = get(ChainTipPrefix);

        if (tip != null) {
            return findBlock(Utils.trim(tip, 0, 32));
        }

        return null;
    }

    public byte[] findBlockHash(int height) {
        return get(Utils.concatenate(BlockIndexPrefix, Utils.takeApart(height)));
    }

    public BlockHeader findBlockHeader(byte[] hash) {
        BlockMetadata metadata = findBlockMetaData(hash);
        if (metadata != null) {
            return metadata.getBlockHeader();
        }

        return null;
    }

    public BlockHeader findBlockHeader(int height) {
        byte hash[] = findBlockHash(height);
        if (hash != null) {
            return findBlockHeader(hash);
        }

        return null;
    }

    public void storeAccount(byte[] address, Account account) {
        put(Utils.concatenate(AccountPrefix, address), account.asByteArray());
    }

    public void newAccount(byte[] address) {
        put(Utils.concatenate(AccountPrefix, address), new Account().asByteArray());
    }

    public void removeAccount(byte[] address) {
        Account account = findAccount(address);
        remove(Utils.concatenate(AccountPrefix, address));
        if (account != null && account.hasAlias()) {
            remove(Utils.concatenate(AliasPrefix, Utils.takeApartLong(account.getAlias())));
        }
    }

    public Account findAccount(long alias) {
        byte address[] = findAccountHolder(alias);
        if (address != null) {
            return findAccount(address);
        }

        return null;
    }

    public byte[] findAccountHolder(long alias) {
        return get(Utils.concatenate(AliasPrefix, Utils.takeApartLong(alias)));
    }

    public Account findAccount(byte address[]) {
        byte data[] = get(Utils.concatenate(AccountPrefix, address));

        if (data != null) {
            InputStream inputStream = new ByteArrayInputStream(data);
            Account account = new Account();
            try {
                account.read(inputStream);
                inputStream.close();
            } catch (IOException | WolkenException e) {
                e.printStackTrace();
                return null;
            }

            return account;
        }

        return null;
    }

    public Address getAddressFromAlias(long alias) {
        return null;
    }

    public void storeContract(Address contractAddress, byte contract[]) throws WolkenException {
        mutex.lock();
        try {
            OutputStream outputStream = location.newFile(".contracts").newFile(Base16.encode(contractAddress.getRaw())).openFileOutputStream();
            outputStream.write(contract);
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            throw new WolkenException(e);
        } finally {
            mutex.unlock();
        }
    }

    public boolean checkTransactionExists(byte[] txid) {
        return get(Utils.concatenate(TransactionPrefix, txid)) != null;
    }

    public boolean checkAccountExists(long alias) {
        return get(Utils.concatenate(AliasPrefix, Utils.takeApart(alias))) != null;
    }

    public boolean checkAccountExists(byte[] address) {
        return get(Utils.concatenate(AccountPrefix, address)) != null;
    }

    public void markRejected(byte[] hash) {
        put(Utils.concatenate(RejectedBlockPrefix, hash), new byte[] { 1 });
    }

    public boolean isRejected(byte[] hash) {
        return get(Utils.concatenate(RejectedBlockPrefix, hash)) != null;
    }

    public boolean checkWalletExists(String name) {
        return get(Utils.concatenate(WalletPrefix, name.getBytes())) != null;
    }

    public void storeWallet(Wallet wallet) {
        put(Utils.concatenate(WalletPrefix, wallet.getName().getBytes()), wallet.asByteArray());
    }

    public Wallet getWallet(String name) {
        return Wallet.fromBytes(name, get(Utils.concatenate(WalletPrefix, name.getBytes())));
    }

    public void registerAlias(byte[] address, long alias) {
        put(Utils.concatenate(AliasPrefix, Utils.takeApartLong(alias)), address);
    }

    public void removeAlias(long alias) {
        remove(Utils.concatenate(AliasPrefix, Utils.takeApartLong(alias)));
    }

    public BlockMetadata findBlockMetaData(byte[] hash) {
        return get(Utils.concatenate(BlockPrefix, hash), BlockMetadata.class);
    }

    public void tempStoreBlock(Block block) {
        put(Utils.concatenate(TempStorage, block.getHashCode()), block);
    }

    public void deleteTempBlock(byte[] hashCode) {
        remove(Utils.concatenate(TempStorage, hashCode));
    }

    public Block findTempBlock(byte[] hashCode) {
        return get(Utils.concatenate(TempStorage, hashCode), Block.class);
    }
}
