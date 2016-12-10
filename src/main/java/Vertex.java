import java.util.ArrayList;
import java.util.List;

/**
 * Created by javlon on 11.10.16.
 */
public class Vertex {
    private String name;
    private List<Vertex> vertices;
    private double weight;
    public Vertex(String name){
        this.name = name;
        this.vertices =  new ArrayList<>();
    }
    public boolean addVertex(Vertex v){
        return this.vertices.add(v);
    }

    public String getName() {
        return name;
    }

    public List<Vertex> getVertices() {
        return vertices;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public double getWeight() {
        return weight;
    }
}
