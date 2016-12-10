import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by javlon on 11.10.16.
 */
public class Main {
    public static String WEIGHTS;
    public static String NODES_G;
    public static String WRIE_HEINZ_RESULTS;

    static String dataEdges;
    static String writeFile;
    static int powerOfModule;
    static int numberOfRuns;
    static int times;
    static int numberOfThreads;


    static void parseArgs(String[] args) {
        if (args.length < 6){
            System.err.println("more args!");
            System.exit(1);
        }
        dataEdges = args[0];
        writeFile = args[1];
        powerOfModule = Integer.parseInt(args[2]);
        numberOfRuns = Integer.parseInt(args[3]);
        times = Integer.parseInt(args[4]);
        numberOfThreads = Integer.parseInt(args[5]);
    }

    public static void main(String[] args) throws FileNotFoundException {
        parseArgs(args);

        PrintWriter printer = new PrintWriter(writeFile);
        printer.println("#alpha shmyak optimPrefixRank meanSh meanOPR");
        printer.flush();
        printer.close();

        List<List<Integer>> pow2graph = sortedConnectedSubgraphs();
        System.out.println("Count of connected sub graphs: " + pow2graph.size());

        for (int i = 0; i < numberOfThreads; i++) {
            Thread t = new Thread(new MyThread(pow2graph, powerOfModule, numberOfRuns, times, dataEdges, writeFile), "Thread " + i);
            t.start();
        }
    }

    static List<List<Integer>> sortedConnectedSubgraphs() {
        List<Vertex> graph = MyThread.read(dataEdges, null);
        Set<List<Integer>> pow2graphSet = new HashSet<>();
        MyThread.generateConnectedSubgraphsSet(pow2graphSet, graph, IntStream.range(0, graph.size()).boxed().collect(Collectors.toList()), new ArrayList<>(), new ArrayList<>());
        List<List<Integer>> pow2graph = new ArrayList<>(pow2graphSet);
        Collections.sort(pow2graph, new Comparator<List<Integer>>() {
            @Override
            public int compare(List<Integer> o1, List<Integer> o2) {
                if (o1.size() < o2.size()) {
                    return -1;
                } else if (o1.size() > o2.size()) {
                    return 1;
                } else {
                    for (int i = 0; i < o1.size(); i++) {
                        if (o1.get(i) < o2.get(i)) {
                            return -1;
                        } else if (o1.get(i) > o2.get(i))
                            return 1;
                    }
                    return 0;
                }
            }
        });
        return pow2graph;
    }
}
