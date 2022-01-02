package io.adenium.core;

import io.adenium.core.consensus.AbstractBlockChain;
import io.adenium.exceptions.WolkenException;
import io.adenium.network.IpAddressList;
import io.adenium.network.NetAddress;
import io.adenium.network.Server;
import io.adenium.papaya.runtime.OpcodeRegister;
import io.adenium.serialization.SerializationFactory;
import io.adenium.utils.FileService;
import org.wolkenproject.network.*;
import org.wolkenproject.papaya.runtime.*;
import io.adenium.rpc.RpcServer;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class Context {
    private static Context instance;

    private Database                database;
    private ContextParams           contextParams;
    private ExecutorService         threadPool;
    private AtomicBoolean           isRunning;
    private IpAddressList ipAddressList;
    private SerializationFactory serializationFactory;
    private TransactionPool         transactionPool;
    private Server server;
    private Address                 payList[];
    private AbstractBlockChain blockChain;
    private OpcodeRegister opcodeRegister;
    private ResourceManager         resourceManager;
    private RpcServer               rpcServer;
    private TaskScheduler           scheduler;
    private FileService fileService;
    private CompressionEngine       compressionEngine;

    public Context(FileService service, int rpcPort, boolean testNet, Address[] payList, Set<NetAddress> forceConnections, boolean pruned, int verbosity) throws WolkenException, IOException {
        Context.instance = this;
        this.scheduler = new TaskScheduler();
        this.database = new Database(service);
        this.contextParams = new ContextParams(testNet, verbosity);
        this.threadPool = Executors.newFixedThreadPool(3);
        this.isRunning = new AtomicBoolean(true);
        this.ipAddressList = new IpAddressList(service.newFile("peers"));
        this.serializationFactory = new SerializationFactory();
        this.transactionPool = new TransactionPool();
        this.payList = payList;
        this.fileService = service;
        this.opcodeRegister = new OpcodeRegister();
        this.resourceManager = new ResourceManager();
        this.compressionEngine = new CompressionEngine();

        SerializationFactory.register(serializationFactory);
        OpcodeRegister.register(opcodeRegister);

        this.blockChain = AbstractBlockChain.create(pruned);
        this.server = new Server(forceConnections);

        getThreadPool().execute(scheduler);
        getThreadPool().execute(server);
        getThreadPool().execute(blockChain);

        this.rpcServer = new RpcServer(this, rpcPort);
        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            if (Context.getInstance().isRunning()) {
                Context.getInstance().shutDown();
            }
        }));
    }

    public void shutDown() {
        isRunning.set(false);
        server.shutdown();
        rpcServer.stop();
        try {
            ipAddressList.save();
        } catch (IOException e) {
            e.printStackTrace();
        }

        threadPool.shutdownNow();
    }

    public Database getDatabase() {
        return database;
    }

    public ContextParams getContextParams() {
        return contextParams;
    }

    public static Context getInstance() {
        return instance;
    }

    public ExecutorService getThreadPool() {
        return threadPool;
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    public IpAddressList getIpAddressList() {
        return ipAddressList;
    }

    public SerializationFactory getSerialFactory() {
        return serializationFactory;
    }

    public TransactionPool getTransactionPool() {
        return transactionPool;
    }

    public Address[] getPayList() {
        return payList;
    }

    public Server getServer() {
        return server;
    }

    public AbstractBlockChain getBlockChain() {
        return blockChain;
    }

    public OpcodeRegister getOpcodeRegister() {
        return opcodeRegister;
    }

    public ResourceManager getResourceManager() {
        return resourceManager;
    }

    public RpcServer getRPCServer() {
        return rpcServer;
    }

    public CompressionEngine getCompressionEngine() {
        return compressionEngine;
    }

    public TaskScheduler getScheduler() {
        return scheduler;
    }
}