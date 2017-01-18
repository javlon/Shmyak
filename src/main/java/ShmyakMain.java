import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;

import java.io.FileNotFoundException;
import java.util.List;

/**
 * Created by javlon on 20.12.16.
 */
public class ShmyakMain {
    public static void main(String[] args) throws FileNotFoundException {
        for (int i = 0; i < 1; i++) {
            List<Vertex> graph = Main.read("in.txt", "weights.txt");
            UniformRealDistribution urd = new UniformRealDistribution(0.01, 0.5);
            double alpha = urd.sample();
            BetaDistribution bd = new BetaDistribution(alpha, 1);
            BetaDistribution ud = new BetaDistribution(1, 1);

            int powerOfModule = (int) (0.3 * graph.size());
            List<Vertex> module = MyThread.generateRandSGFixedSize(graph, powerOfModule);

            /*
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
            */
            Ranking r = new ShmyakCPLEX(graph, null, module, alpha);
            r.solve();
            List<Vertex> list = r.getRanking();
            for (Vertex v : list) {
                System.out.println(v.getName() + " " + v.getWeight());
            }
            System.out.println("\n\nalpha = " + alpha);
            for (Vertex  v : module){
                System.out.print(v.getName() + " ");
            }
            System.out.println();
            for (Vertex v : list){
                System.out.print(v.getName() + " ");
            }
            System.out.println("\n\nAUC = " + r.getAuc());
        }
    }
}
