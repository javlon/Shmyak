import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by javlon on 24.11.16.
 */
public abstract class Ranking {
    final List<Vertex> graph;
    final List<ProbOfGraph> list;
    final List<Vertex> module;

    List<Vertex> ranking;
    private double auc;
    double meanAuc;

    public Ranking(List<Vertex> graph, List<ProbOfGraph> list, List<Vertex> module) {
        this.graph = graph;
        this.list = list;
        this.module = module;
        this.ranking = new ArrayList<>();
    }

    public List<Vertex> getRanking() {
        return ranking;
    }

    public double getAuc() {
        return auc;
    }

    public double getMeanAuc() {
        return meanAuc;
    }

    public abstract void solve();

    public void calcAuc() {
        auc = getAUC(getRocList(module, ranking));
    }

    public void calcMeanAuc() {
        meanAuc = meanAUC(graph, list, ranking);
    }

    private static double getAUC(List<Point> points) {
        double area = 0;
        for (int i = 1; i < points.size(); i++) {
            area += (points.get(i).getX() - points.get(i - 1).getX()) *
                    (points.get(i).getY() + points.get(i - 1).getY()) / 2;
        }
        return area;
    }

    private double meanAUC(List<Vertex> graph, List<ProbOfGraph> p, List<Vertex> ranking) {
        double mean = 0;
        for (ProbOfGraph pb : p) {
            List<Vertex> connected = pb.getGraph().stream().map(graph::get).collect(Collectors.toList());
            mean += pb.getProb() * getAUC(getRocList(connected, ranking));
        }
        return mean;
    }

    private static List<Point> getPrecisionRecallList(List<Vertex> module, List<Vertex> ranking) {
        List<Point> list = new ArrayList<>();
        int tpAndFn = module.size();
        for (int i = 0, tp = 0; i < ranking.size(); i++) {
            if (module.contains(ranking.get(i)))
                tp++;
            list.add(new Point((double) tp / tpAndFn, (double) tp / (i + 1)));
        }
        return list;
    }

    private static List<Point> getRocList(List<Vertex> module, List<Vertex> ranking) {
        List<Point> list = new ArrayList<>();
        int tpr = 0, fpr = 0;
        if (module.size() == 0 || ranking.size() == module.size()) {
            list.add(new Point(0, 1));
            list.add(new Point(1, 1));
            return list;
        }
        for (int i = 0; i < ranking.size(); i++) {
            if (module.contains(ranking.get(i)))
                tpr++;
            else
                fpr++;
            list.add(new Point((double) fpr / (ranking.size() - module.size()), (double) tpr / module.size()));
        }
        //TODO: here we have a bug: |alfa -> 0|
        if (list.get(0).getX() != 0.0 && list.get(0).getY() != 0.0)
            System.err.println(list);


        return list;
    }
}
