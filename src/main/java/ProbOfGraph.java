import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by javlon on 18.10.16.
 */
public class ProbOfGraph {
    private List<Integer> graph;
    private double prob;

    public ProbOfGraph(int[] graph, double prob) {
        this.graph = Arrays.stream(graph).boxed().collect(Collectors.toList());
        this.prob = prob;
    }

    @Override
    public String toString() {
        return "ProbOfGraph{" +
                "graph=" + graph +
                ", prob=" + prob +
                '}';
    }

    public ProbOfGraph(List<Integer> graph, double prob) {
        this.graph = graph;
        this.prob = prob;
    }

    public void setProb(double prob) {
        this.prob = prob;
    }

    public void setGraph(List<Integer> graph) {
        this.graph = graph;
    }

    public List<Integer> getGraph() {
        return graph;
    }

    public double getProb() {
        return prob;
    }

}
