import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by javlon on 24.11.16.
 */
public class Simple extends Ranking{
    public Simple(List<Vertex> graph, List<ProbOfGraph> list, List<Vertex> module) {
        super(graph, list, module);
    }

    @Override
    public void solve() {
        List<Vertex> out = new ArrayList<>(graph);
        Collections.sort(out, Comparator.comparing(Vertex::getWeight));
        ranking = out;
        calcAuc();
        //calcMeanAuc();
    }
}
