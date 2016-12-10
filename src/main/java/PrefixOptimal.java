import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by javlon on 30.11.16.
 */
public class PrefixOptimal extends Ranking {
    //allconnectedsg must be sorted. by subgraphs
    List<Pair<List<Integer>, Pair<List<Integer>, List<Integer>>>> connectedSGwithLinks;

    public PrefixOptimal(List<Vertex> graph, List<ProbOfGraph> list, List<Vertex> module, List<Pair<List<Integer>, Pair<List<Integer>, List<Integer>>>> connectedSGwithLinks) {
        super(graph, list, module);
        this.connectedSGwithLinks = connectedSGwithLinks;
    }



    @Override
    public void solve() {

        //TODO:recalculate list!

        List<ProbOfGraph> dp = IntStream.range(0, connectedSGwithLinks.size()).boxed().
                map(x -> new ProbOfGraph(new ArrayList<Integer>(), 0.0)).collect(Collectors.toList());
        for (int i = 0; i < connectedSGwithLinks.size(); i++) {
            List<Integer> curGraph = connectedSGwithLinks.get(i).getKey();
            List<Integer> links = connectedSGwithLinks.get(i).getValue().getValue();
            List<Integer> delVer = connectedSGwithLinks.get(i).getValue().getKey();
            //if liks.size() == 0 --> curGraph.size() == 1
            if (links.size() == 0){
                dp.set(i, new ProbOfGraph(curGraph, OptimalRange.getArea(list, new ArrayList<>(), curGraph.get(0), graph.size())));
            }
            for (int k = 0; k < links.size(); ++k) {
                int j = links.get(k);
                double value = dp.get(j).getProb() + OptimalRange.getArea(list, dp.get(j).getGraph(), delVer.get(k), graph.size());
                if (dp.get(i).getProb() < value) {
                    List<Integer> smth = new ArrayList<>(dp.get(j).getGraph());
                    smth.add(delVer.get(k));
                    dp.set(i, new ProbOfGraph(smth, value));
                }
            }
        }
        List<Integer> ans = dp.get(dp.size() -1).getGraph();
        ranking = ans.stream().map(graph::get).collect(Collectors.toList());

        calcAuc();
        calcMeanAuc();
    }

}
