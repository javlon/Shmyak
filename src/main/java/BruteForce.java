import org.apache.commons.math3.util.Combinations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by javlon on 26.10.16.
 */
public class BruteForce extends Ranking {
    int countOfVSG;
    double alpha;
    public BruteForce(List<Vertex> graph, List<ProbOfGraph> list, List<Vertex> module, int countOfVSG, double alpha) {
        super(graph, list, module);
        this.countOfVSG = countOfVSG;
        this.alpha = alpha;
    }

    @Override
    public void solve() {
        List<ProbOfGraph> list = new ArrayList<>();
        Combinations comb = new Combinations(graph.size(), countOfVSG);
        for (int[] subGraph : comb) {
            List<Vertex> selection = Arrays.stream(subGraph).boxed().map(graph::get).collect(Collectors.toList());
            if (MyThread.checkToConnected(selection)) {
                list.add(new ProbOfGraph(subGraph, MyThread.getProb(selection, alpha)));
            }
        }
        //prob - probability of Vertices
        double[] prob = new double[graph.size()];
        for (ProbOfGraph pg : list) {
            for (int i = 0; i < pg.getGraph().size(); i++) {
                prob[pg.getGraph().get(i)] += pg.getProb();
            }
        }

        //sort and get numbers
        //TODO: rewrite
        double[][] sort = new double[graph.size()][2];
        for (int i = 0; i < graph.size(); i++) {
            sort[i][0] = prob[i];
            sort[i][1] = i;
        }
        Arrays.sort(sort, new Comparator<double[]>() {
            @Override
            public int compare(double[] o1, double[] o2) {
                if (o1[0] < o2[0])
                    return 1;
                else if (o1[0] == o2[0])
                    return 0;
                else
                    return -1;
            }
        });

        List<Vertex> ret = new ArrayList<>();
        for (int i = 0; i < graph.size(); i++) {
            ret.add(graph.get((int) sort[i][1]));
        }

        ranking = ret;
        calcAuc();
        calcMeanAuc();
    }

}
