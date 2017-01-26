import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by javlon on 20.12.16.
 */
public class ShmyakMain {
    static void dfs(int i, List<List<Integer>> links, boolean[] used, List<Integer> comp){
        used[i] = true;
        comp.add(i);
        for (int j = 0; j < links.get(i).size(); j++) {
            int to = links.get(i).get(j);
            if (!used[to]){
                dfs(to, links, used, comp);
            }
        }
    }

    static List<List<Vertex>> findComps(List<Vertex> list){
        Map<String, Integer> map = new HashMap<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            map.put(list.get(i).getName(), i);
        }

        List<List<Integer>> links = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            links.add(new ArrayList<>());
            Vertex v = list.get(i);
            for(Vertex u : v.getVertices()){
                links.get(i).add(map.get(u.getName()));
            }
        }

        List<List<Vertex>> ret = new ArrayList<>();
        boolean[] used = new boolean[list.size()];
        for (int i = 0; i < list.size(); i++) {
            if (!used[i]){
                List<Integer> comp = new ArrayList<>();
                dfs(i, links, used, comp);
                List<Vertex> vComp = comp.stream().map(list::get).collect(Collectors.toList());
                ret.add(vComp);
            }
        }
        return ret;
    }
    public static void main(String[] args) throws FileNotFoundException {
        double prop = Double.parseDouble(args[0]);
        double aalpha = Double.parseDouble(args[1]);
        for (int i = 0; i < 1; i++) {
            List<Vertex> graph = Main.read("in.txt", "weights.txt");
            List<List<Vertex>> comps = findComps(graph);
            Collections.sort(comps, new Comparator<List<Vertex>>() {
                @Override
                public int compare(List<Vertex> o1, List<Vertex> o2) {
                    return o2.size()-o1.size();
                }
            });

            UniformRealDistribution urd = new UniformRealDistribution(0.01, 0.5);
            double alpha = urd.sample();
            if (aalpha > 0)
                alpha = aalpha;
            System.out.println("alpha = " + alpha);
            BetaDistribution bd = new BetaDistribution(alpha, 1);
            BetaDistribution ud = new BetaDistribution(1, 1);

            int powerOfModule =(int) (prop * graph.size());
            List<Vertex> module = MyThread.generateRandSGFixedSize(graph, powerOfModule);
            PrintWriter printWriter = new PrintWriter(new File("module.txt"));
            for (Vertex v: module){
                printWriter.println(v.getName());
            }
            printWriter.flush();
            printWriter.close();

            PrintWriter printer = new PrintWriter(new File("weights.txt"));
            for (Vertex v : graph) {
                if (module.contains(v))
                    v.setWeight(bd.sample());
                else
                    v.setWeight(ud.sample());
                printer.println(v.getName() + " " + v.getWeight());
            }
            printer.flush();
            printer.close();

            List<Vertex> ranked = new ArrayList<>();
            for (int j = 0; j < comps.size(); ++j) {
                Ranking r = new ShmyakCPLEX(comps.get(j), null, module, alpha, graph.size());
                r.solve();
                ranked.addAll(r.getRanking());
            }
            System.out.println("\nAUC = " + Ranking.getAUC(Ranking.getRocList(module, ranked)) + "\n\n\n\n");
        }
    }
}