import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by javlon on 26.10.16.
 */
public class Shmyak extends Ranking {

    private int method;
    public Shmyak(List<Vertex> graph, List<ProbOfGraph> list, List<Vertex> module, int method) {
        super(graph, list, module);
        this.method = method;
    }

    //connected prefix
    @Override
    public void solve() {
        List<Integer> order = new ArrayList<>();
        List<Integer> allNodes = list.get(list.size() - 1).getGraph();

        addOneMore(list, allNodes, order);

        ranking = order.stream().map(graph::get).collect(Collectors.toList());
        calcAuc();
        calcMeanAuc();
    }


    //TODO: |connected component of graph| > 1;
    //method = {0,1}, 0 -> just shmyak, 1 -> full shmyak
    private void addOneMore(List<ProbOfGraph> list, List<Integer> graph, List<Integer> mustContain) {
        while (true) {
            ProbOfGraph s = null;
            for (ProbOfGraph p : list) {
                List<Integer> plist = p.getGraph();
                if (mustContain.size() == 0) {
                    if ((plist.size() < graph.size()) && (method == 0 || (method == 1 && difference(mustContain, intersection(plist, mustContain)).size() == 0))) {
                        if (s == null)
                            s = p;
                        else if (s.getProb() < p.getProb())
                            s = p;
                    }
                } else if (intersection(plist, mustContain).size() > 0 &&
                        difference(plist, mustContain).size() > 0 &&
                        union(plist, mustContain).size() < graph.size() &&
                        (method == 0 || (method == 1 && difference(mustContain, intersection(plist, mustContain)).size() == 0))) {
                    if (s == null)
                        s = p;
                    else if (s.getProb() < p.getProb())
                        s = p;
                }
            }
            if (s == null) {
                List<Integer> diff = difference(graph, mustContain);
                if (diff.size() == 0)
                    return;
                else if (diff.size() == 1) {
                    mustContain.add(diff.get(0));
                    return;
                } else {
                    System.err.println("Something wrong!!!!");
                    System.exit(1);
                }
            } else {
                List<Integer> newGraph = union(s.getGraph(), mustContain);
                List<ProbOfGraph> newList = new ArrayList<>();
                for (ProbOfGraph p : list) {
                    List<Integer> plist = p.getGraph();
                    if (union(plist, newGraph).size() == newGraph.size())
                        newList.add(p);
                }
                addOneMore(newList, newGraph, mustContain);
            }
        }
    }

    public static List<Integer> union(List<Integer> a, List<Integer> b) {
        List<Integer> list = new ArrayList<>();
        for (Integer i : a)
            list.add(i);
        for (Integer i : b)
            if (!list.contains(i))
                list.add(i);
        return list;
    }

    public static List<Integer> intersection(List<Integer> a, List<Integer> b) {
        List<Integer> list = new ArrayList<>();
        for (Integer i : a)
            for (Integer j : b)
                if (i.equals(j)) {
                    list.add(i);
                    break;
                }
        return list;
    }

    // difference(a, b) = A/B;
    public static List<Integer> difference(List<Integer> a, List<Integer> b) {
        List<Integer> list = new ArrayList<>();
        for (Integer i : a)
            if (!b.contains(i))
                list.add(i);
        return list;
    }

}
