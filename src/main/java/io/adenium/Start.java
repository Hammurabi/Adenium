package io.adenium;

import io.adenium.core.Address;
import io.adenium.core.Context;
import io.adenium.core.transactions.Transaction;
import io.adenium.crypto.CryptoLib;
import io.adenium.crypto.ec.ECKeypair;
import io.adenium.encoders.Base16;
import io.adenium.encoders.Base58;
import io.adenium.exceptions.AdeniumException;
import io.adenium.network.NetAddress;
import io.adenium.utils.FileService;
import io.adenium.utils.Logger;
import io.adenium.wallet.BasicWallet;
import org.apache.commons.cli.*;
import org.fusesource.jansi.AnsiConsole;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

public class Start {
    public static void main(String args[]) throws ParseException, AdeniumException, IOException {
        CryptoLib.getInstance();
        AnsiConsole.systemInstall();

        Options options = new Options();
        options.addOption("dir", true, "set the main directory for Adenium, otherwise uses the default application directory of the system.");
        options.addOption("enable_fullnode", true, "set the node to a full node.");
        options.addOption("enable_verbose", true, "enable verbose mode to get data logged to the console.");
        options.addOption("enable_testnet", true, "set the testnet to enabled/disabled.");
        options.addOption("enable_mining", true, "set the node to a mining node.");
        options.addOption("enable_storage", true, "act as a storage node.");
        options.addOption("enable_seeding", false, "act as a seeding node.");
        options.addOption("force_connect", true, "force a connection to an array of {ip:port}.");
        //-quicksend to amount fee wallet pass
        options.addOption("quick_sign", true, "quickly make a transaction and sign it.");
        options.addOption("broadcast_tx", true, "broadcast a transaction to the network.");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        FileService mainDirectory = FileService.appDir();
        if (cmd.hasOption("dir")) {
            FileService dir = new FileService(cmd.getOptionValue("dir"));
            if (dir.exists()) {
                mainDirectory = dir;
            } else {
                Logger.faterr("provided directory '" + cmd.getOptionValue("dir") + "' does not exist.");
                return;
            }
        }

        boolean isTestNet = false;
        if (cmd.hasOption("enable_testnet")) {
            String value = cmd.getOptionValue("enable_testnet").toLowerCase();
            // we could parse into a boolean here
            if (value.equals("true")) {
                isTestNet = true;
            } else if (value.equals("false")) {
                isTestNet = false;
            } else {
                Logger.faterr("provided argument '-enable_testnet " + cmd.getOptionValue("enable_testnet") + "' is invalid.");
                return;
            }
        }

        if (cmd.hasOption("quick_sign")) {
            String qsArgs[] = cmd.getOptionValues("quick_sign");
            if (qsArgs.length != 5) {
                throw new AdeniumException("quicksend expects 5 arguments, '"+qsArgs.length+"' provided.");
            }

            BasicWallet wallet = new BasicWallet(mainDirectory.newFile(qsArgs[3]));

            if (!Address.isValidAddress(Base58.decode(qsArgs[0]))) {
                throw new AdeniumException("address '" + qsArgs[0] + "' is invalid.");
            }

            long amount = Long.parseLong(qsArgs[1]);
            long fee    = Long.parseLong(qsArgs[2]);

            Address recipient = Address.fromFormatted(Base58.decode(qsArgs[0]));
            Transaction transaction = Transaction.newTransfer(recipient, amount, fee, wallet.getNonce() + 1);
            transaction = transaction.sign(new ECKeypair(wallet.getPrivateKey(qsArgs[4].toCharArray())));

            Logger.alert("transaction signed successfully ${t}", Logger.Levels.NotificationMessage, Base16.encode(transaction.asSerializedArray()));
            System.exit(0);
        }

        mainDirectory = mainDirectory.newFile("Adenium");
        if (!mainDirectory.exists())
        {
            mainDirectory.makeDirectory();
        }

        Address address[] = null;

        if (cmd.hasOption("enable_mining")) {
            String value = cmd.getOptionValue("enable_mining").toLowerCase();
            value = value.substring(1, value.length() - 1);

            String addresses[] = value.split(",");
            address = new Address[addresses.length];

            int i = 0;
            for (String b58 : addresses) {
                byte bytes[] = Base58.decode(b58);

                if (!Address.isValidAddress(bytes)) {
                    throw new AdeniumException("invalid address '" + b58 + "' provided.");
                }

                address[i ++] = Address.fromFormatted(bytes);
            }
        }

        Set<NetAddress> connectionList = new HashSet<>();

        if (cmd.hasOption("force_connect")) {
            String value = cmd.getOptionValue("force_connect");
            String ips[] = value.substring(1, value.length() - 1).split(",");
            for (String ipInfo : ips) {
                String ip[] = ipInfo.split(":");
                connectionList.add(new NetAddress(InetAddress.getByName(ip[0]), Integer.parseInt(ip[1]), 0));
            }

            Logger.alert("force connections ${list}", Logger.Levels.NotificationMessage, connectionList);
        }

        int verbosity = 0;

        if (cmd.hasOption("enable_verbose")) {
            if (!cmd.getOptionValue("enable_verbose").matches("[1-5]")) {
                throw new AdeniumException("'enable_verbose' expected a number between '1-5'.");
            }

            verbosity = Integer.parseInt(cmd.getOptionValue("enable_verbosity"));
        }

        int rpcPort = 12560;
        Context context = new Context(mainDirectory, rpcPort, isTestNet, address, connectionList, false, verbosity);
    }
}
