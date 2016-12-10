import org.apache.commons.math3.util.Combinations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by javlon on 07.11.16.
 */
public class OptimalRange extends Ranking {

    private int mode;
    // if mode=1 -> prefix is connected,
    public OptimalRange(List<Vertex> graph, List<ProbOfGraph> list, List<Vertex> module, int mode) {
        super(graph, list, module);
        this.mode = mode;
    }

    public static double getArea(List<ProbOfGraph> p, List<Integer> vi, int v, int n) {
        double mean = 0;
        for (ProbOfGraph it : p) {
            if (!it.getGraph().contains(v) || it.getGraph().size() == n) {
                continue;
            }
            double d = it.getProb() / (it.getGraph().size() * (n - it.getGraph().size()));
            int fp = 0;
            for (Integer i : vi) {
                if (!it.getGraph().contains(i)) {
                    ++fp;
                }
            }
            d *= (n - it.getGraph().size() - fp);
            mean += d;
        }
        return mean;
    }

    @Override
    public void solve() {
        int n = graph.size();
        int p2n = 1 << n;

        List<ProbOfGraph> dp = new ArrayList<>(p2n);
        dp.add(new ProbOfGraph(new ArrayList<Integer>(), 0));
        for (int i = 1; i < p2n; i++) {
            dp.add(null);
        }
        for (int i = 1; i <= n; i++) {
            Combinations comb = new Combinations(n, i);
            for (int[] subGraph : comb) {
                List<Integer> sub = Arrays.stream(subGraph).boxed().collect(Collectors.toList());
                int ind = 0;
                for (int x : subGraph) {
                    ind += (1 << x);
                }
                List<Vertex> bla = sub.stream().map(graph::get).collect(Collectors.toList());

                if (mode == 1 && !MyThread.checkToConnected(bla)) {
                    dp.set(ind, new ProbOfGraph(sub, 0));
                    continue;
                }

                for (int j = 0; j < subGraph.length; ++j) {
                    Integer del = sub.remove(0);
                    ProbOfGraph prev = dp.get(ind - (1 << del));
                    double value = prev.getProb() + getArea(list, sub, del, n);
                    List<Integer> smth = new ArrayList<>(prev.getGraph());
                    smth.add(del);
                    if (dp.get(ind) == null) {
                        dp.set(ind, new ProbOfGraph(smth, value));
                    } else if (dp.get(ind).getProb() < value) {
                        dp.set(ind, new ProbOfGraph(smth, value));
                    }
                    sub.add(del);
                }
            }
        }
        List<Integer> ans = dp.get(p2n - 1).getGraph();
        ranking = ans.stream().map(graph::get).collect(Collectors.toList());

        calcAuc();
        calcMeanAuc();
    }
}
