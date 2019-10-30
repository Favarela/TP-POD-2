package ar.edu.itba.pod.client;

import ar.edu.itba.pod.client.queries.Query;
import ar.edu.itba.pod.client.queries.Query1;
import ar.edu.itba.pod.client.queries.Query3;
import ar.edu.itba.pod.client.utils.AirportCsvParser;
import ar.edu.itba.pod.client.utils.CsvParser;
import ar.edu.itba.pod.client.utils.MovementCsvParser;
import ar.edu.itba.pod.client.utils.PrintResult;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.ClientNetworkConfig;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IList;
import model.Airport;
import model.Movement;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;

public class Client {
    private static Logger logger = LoggerFactory.getLogger(Client.class);

    private static final String QUERY_OPTION_NAME = "query";
    private static final String QUERY_OPTION_DESCRIPTION = "todo";

    private static final String ADDRESSES_NAME = "addresses";
    private static final String ADDRESSES_DESCRIPTION = "todo";

    private static final String IN_PATH_NAME = "inPath";
    private static final String IN_PATH_DESCRIPTION = "todo";

    private static final String OUT_PATH_NAME = "outPath";
    private static final String OUT_PATH_DESCRIPTION = "todo";

    private static final String N_NAME = "n";
    private static final String N_DESCRIPTION = "todo";

    private static final String OACI_NAME = "oaci";
    private static final String OACI_DESCRIPTION = "todo";

    private static final String MIN_NAME = "min";
    private static final String MIN_DESCRIPTION = "todo";
//    TODO add descriptions


    public static void main( String[] args ) throws ExecutionException, InterruptedException, IOException {
        Options options;
        options = new Options();
        addOption(options, QUERY_OPTION_NAME, QUERY_OPTION_DESCRIPTION, true, true);
        addOption(options, ADDRESSES_NAME, ADDRESSES_DESCRIPTION, true, true);
        addOption(options, IN_PATH_NAME, IN_PATH_DESCRIPTION, true, true);
        addOption(options, OUT_PATH_NAME, OUT_PATH_DESCRIPTION, true, true);
        addOption(options, N_NAME, N_DESCRIPTION, true, false);
        addOption(options, OACI_NAME, OACI_DESCRIPTION, true, false);
        addOption(options, MIN_NAME, MIN_DESCRIPTION, true, false);

        /* Retrieve arguments info */
        CommandLine commandLine = parse(args, options);

        /* Set configuration for Hazelcast client */
        ClientConfig clientConfig = getConfig(commandLine);

        int queryNumber = Integer.parseInt(commandLine.getOptionValue(QUERY_OPTION_NAME));

        logger.info("Client starting ...");

        /* Hazelcast client instance*/
        HazelcastInstance client = HazelcastClient.newHazelcastClient(clientConfig);

        /* Class info for time file */
        String className = Client.class.getSimpleName();
        String methodName = new Exception().getStackTrace()[0].getMethodName();

        /* Result & Time file */
        PrintResult printResult = new PrintResult(commandLine.getOptionValue("outPath") + "query"+queryNumber+".csv");
        PrintResult printTime = new PrintResult(commandLine.getOptionValue("outPath") + "query"+queryNumber+".txt");

        printTime.appendTimeOf(methodName, className, new Exception().getStackTrace()[0].getLineNumber(),
                "Inicio de la lectura del archivo de entrada");

        /* Read airports file */
        IList<Airport> airportsHz = client.getList("airports");
        CsvParser airportCsvParser = new AirportCsvParser(airportsHz);
        Path airportsPath = Paths.get(commandLine.getOptionValue(IN_PATH_NAME) + "/aeropuertos.csv");
        airportCsvParser.loadData(airportsPath);

        /* Read movements file */
        IList<Movement> movementsHz = client.getList("movements");
        CsvParser movementCsvParser = new MovementCsvParser(movementsHz);
        Path movementsPath = Paths.get(commandLine.getOptionValue(IN_PATH_NAME) + "/movimientos.csv");
        movementCsvParser.loadData(movementsPath);

        printTime.appendTimeOf(methodName, className, new Exception().getStackTrace()[0].getLineNumber(),
                "Fin de lectura del archivo de entrada ");

        logger.info("Running Query #{}", queryNumber);

        /* Create query */
        Query runner = runQuery(queryNumber, airportsHz, movementsHz, client, commandLine, printResult);
        printTime.appendTimeOf(methodName, className, new Exception().getStackTrace()[0].getLineNumber(),
                "Inicio de un trabajo MapReduce");

        /* Run query */
        runner.run();
        printTime.appendTimeOf(methodName, className, new Exception().getStackTrace()[0].getLineNumber(),
                "Fin de un trabajo MapReduce");
//        runner.writeResult();

        /* End client */
        logger.info("Client shutting down ...");
        client.shutdown();

        /* Close files */
        printResult.close();
        printTime.close();
    }

    private static ClientConfig getConfig(CommandLine commandLine) {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setGroupConfig(new GroupConfig("g3", "g3"));
        String addresses = commandLine.getOptionValue("addresses");
        String addressesList[] = addresses.split(";");
        ClientNetworkConfig networkConfig = clientConfig.getNetworkConfig();
        for(String address : addressesList) {
            networkConfig.addAddress(address);
        }
        return clientConfig;
    }

    public static CommandLine parse(String[] args, Options options){
        CommandLineParser parser = new DefaultParser();

        try {
            return parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println( "Parsing failed.  Reason: " + e.getMessage());
            System.exit(1);
            return null;
        }
    }

    public static void addOption(Options options, final String name, final String description, final boolean hasArguments, final boolean required) {
        Option op = new Option(name, name, hasArguments, description);
        op.setRequired(required);
        options.addOption(op);
    }

    private static Query runQuery(int queryNumber, IList<Airport> airports, IList<Movement> movements, HazelcastInstance hazelcastInstance, CommandLine arguments,
                                  PrintResult printResult) {
        switch(queryNumber) {
            case 1:
                return new Query1(airports, movements, hazelcastInstance, arguments, printResult);
            case 3:
                return new Query3(movements, hazelcastInstance, arguments, printResult);
            default:
                throw new IllegalArgumentException("Invalid query number " + queryNumber + ". Insert a value from 1 to 6.");
        }
    }

}
