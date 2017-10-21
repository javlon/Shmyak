import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;

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
    static String dirToCplex;

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
        dirToCplex = args[6];
    }

    public static void main(String[] args) throws FileNotFoundException {
        parseArgs(args);

        PrintWriter printer = new PrintWriter(writeFile);
        printer.println("#alpha | simple mwcs   shmyak uoptim doptim | meansimple meanmwcs meanshmyak meanuoptim meandoptim ");
        printer.flush();
        printer.close();


        for (int i = 0; i < numberOfThreads; i++) {
            Thread t = new Thread(new MyThread(powerOfModule, numberOfRuns, times, "inT"+i+".txt", writeFile, dirToCplex), "T" + i);
            t.start();
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
            String nothing = sc.next();
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
                if (v != null)
                    v.setWeight(new Double(weight));
            }
        }
        List<Vertex> ret = new ArrayList<Vertex>(vertexes.values());
        Collections.sort(ret, (o1, o2) -> Integer.parseInt(o1.getName()) - Integer.parseInt(o2.getName()));
        return ret;
    }

}
