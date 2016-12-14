import java.io.File;
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
    static boolean useSamplingDistib;

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
        useSamplingDistib = Boolean.parseBoolean(args[6]);
    }

    public static void main(String[] args) throws FileNotFoundException {
        parseArgs(args);

        PrintWriter printer = new PrintWriter(writeFile);
        printer.println("#alpha shmyak optimPrefixRank meanSh meanOPR");
        printer.flush();
        printer.close();

        List<List<Integer>> connectedSubgraphs = sortedConnectedSubgraphs();
        System.out.println("Count of connected sub graphs: " + connectedSubgraphs.size());

        for (int i = 0; i < numberOfThreads; i++) {
            Thread t = new Thread(new MyThread(connectedSubgraphs, powerOfModule, numberOfRuns, times, dataEdges, writeFile, useSamplingDistib), "Thread " + i);
            t.start();
        }
    }

    static List<List<Integer>> sortedConnectedSubgraphs() {
        List<Vertex> graph = read(dataEdges, null);
        Set<List<Integer>> pow2graphSet = new HashSet<>();
        generateConnectedSubgraphsSet(pow2graphSet, graph, IntStream.range(0, graph.size()).boxed().collect(Collectors.toList()), new ArrayList<>(), new ArrayList<>());
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

    public static void generateConnectedSubgraphsSet(Set<List<Integer>> ret, List<Vertex> graph, List<Integer> verticesNotYetConsidered,
                                                     List<Integer> connected, List<Integer> neighbors) {
        List<Integer> candidates = null;
        if (connected.size() == 0) {
            candidates = new ArrayList<>(verticesNotYetConsidered);
        } else {
            candidates = Shmyak.intersection(verticesNotYetConsidered, neighbors);
        }
        if (candidates.size() == 0) {
            if (connected.size() == 0)
                return;
            List<Integer> smth = new ArrayList<>(connected);
            Collections.sort(smth);
            if (!ret.contains(smth))
                ret.add(smth);
        } else {
            int chosenVertex = candidates.get(candidates.size() - 1);
            verticesNotYetConsidered.remove(Integer.valueOf(chosenVertex));
            generateConnectedSubgraphsSet(ret, graph, verticesNotYetConsidered, connected, neighbors);
            List<Integer> newNeighbors = Shmyak.union(neighbors, graph.get(chosenVertex).getVertices().stream().map(x -> graph.indexOf(x)).collect(Collectors.toList()));
            connected.add(chosenVertex);
            generateConnectedSubgraphsSet(ret, graph, verticesNotYetConsidered, connected, newNeighbors);
            connected.remove(connected.size() - 1);
            verticesNotYetConsidered.add(chosenVertex);
        }
    }

    public static List<Vertex> read(String fileEdges, String fileNodes) {
        Scanner sc = null;
        try {
            sc = new Scanner(new File(fileEdges)).useDelimiter("(\\n|\\s+)");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Map<String, Vertex> vertexes = new HashMap<>();
        while (sc.hasNext()) {
            String left = sc.next();
            String right = sc.next();
            Vertex from = vertexes.get(left);
            Vertex to = vertexes.get(right);
            if (from == null) {
                Vertex v = new Vertex(left);
                vertexes.put(left, v);
                from = v;
            }
            if (to == null) {
                Vertex v = new Vertex(right);
                vertexes.put(right, v);
                to = v;
            }
            from.addVertex(to);
            to.addVertex(from);
        }
        if (fileNodes != null) {
            try {
                sc = new Scanner(new File(fileNodes)).useDelimiter("(\\n|\\s+)");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            while (sc.hasNext()) {
                String node = sc.next();
                String weight = sc.next();
                Vertex v = vertexes.get(node);
                v.setWeight(new Double(weight));
            }
        }
        List<Vertex> ret = new ArrayList<Vertex>(vertexes.values());
        Collections.sort(ret, (o1, o2) -> Integer.parseInt(o1.getName()) - Integer.parseInt(o2.getName()));
        return ret;
    }

}
