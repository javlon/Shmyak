import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Created by javlon on 08.11.16.
 */
public class MWCS extends Ranking {
    public MWCS(List<Vertex> graph, List<ProbOfGraph> list, List<Vertex> module) {
        super(graph, list, module);
    }

    private static List<Vertex> readHeinz(String nameFile, List<Vertex> graph) {
        List<Vertex> ans = new ArrayList<>();
        try {
            Scanner sc = new Scanner(new File(nameFile));
            while (sc.hasNextLine()) {
                String s = sc.nextLine();
                int begin = s.indexOf("[label=\"");
                int end = s.indexOf("\\n");
                if (begin < 0 || end < 0 || end < begin) {
                    continue;
                }
                String res = s.substring(begin + 8, end);
                ans.addAll(graph.stream().filter(v -> v.getName().equals(res)).collect(Collectors.toList()));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return ans;
    }

    @Override
    public void solve() {
        int times = 10;
        double[] weights = new double[graph.size()];
        double max = -1;
        for (int i = 0; i < graph.size(); ++i) {
            weights[i] = -Math.log(graph.get(i).getWeight());
            max = max < weights[i] ? weights[i] : max;
        }

        for (int i = 0; i < graph.size(); i++) {
            weights[i] -= max;
        }
        double t = max / times; //threshold
        List<Vertex> heinzRanking = new ArrayList<>();
        for (int i = 0; i < times; ++i) {
            runHeinz(t * i, weights, graph);
            List<Vertex> heinzList = readHeinz(Main.WRIE_HEINZ_RESULTS, graph);
            for (Vertex v : heinzList) {
                if (!heinzRanking.contains(v))
                    heinzRanking.add(v);
            }
        }
        for (Vertex v : graph) {
            if (!heinzRanking.contains(v))
                heinzRanking.add(v);
        }
        ranking = heinzRanking;
        calcAuc();
        calcMeanAuc();
    }

    private static void runHeinz(double t, double[] weights, List<Vertex> graph) {
        try {
            PrintWriter printer = new PrintWriter(Main.NODES_G);
            for (int i = 0; i < weights.length; i++) {
                printer.println(graph.get(i).getName() + " " + (weights[i] + t));
            }
            printer.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            ProcessBuilder pb = new ProcessBuilder();
            pb.command("./heinz2.1", "-e", Main.dataEdges, "-n", Main.NODES_G, "-m", "4", "-v", "0");
            pb.redirectOutput(new File(Main.WRIE_HEINZ_RESULTS));
            Process process = pb.start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

}
