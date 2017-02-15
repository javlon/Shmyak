import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Created by javlon on 19.01.17.
 */
public class Smth {
    public static void main(String[] args) throws FileNotFoundException {
        List<Vertex> graph = Main.read("edges.txt", "nodes.txt");
        Map<String, Integer> map = new HashMap<String, Integer>(graph.size());
        for (int i = 0; i < graph.size(); ++i){
            map.put(graph.get(i).getName(), i);
        }
        Scanner sc = new Scanner(new File("comp.txt"));
        List<Vertex> module = new ArrayList<>();
        while (sc.hasNext()){
            String name  =  sc.next();
            module.add(graph.get(map.get(name)));
        }

        Ranking r = new MWCS(graph, null, module);
        PrintWriter pw = new PrintWriter(new File("mwcs.txt"));
        pw.flush();
        pw.close();
        r.solve();

        PrintWriter printWriter = new PrintWriter(new File("mwcsR.txt"));
        for (Vertex v : r.getRanking()){
            printWriter.println(v.getName());
        }
        printWriter.flush();
        printWriter.close();
        System.out.println(r.getAuc());
        //Collections.sort(graph, Comparator.comparingDouble(Vertex::getWeight));

        //System.out.println(Ranking.getAUC(Ranking.getRocList(module, graph)));
    }
}
