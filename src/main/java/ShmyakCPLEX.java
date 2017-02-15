import org.apache.commons.math3.util.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.*;

/**
 * Created by javlon on 15.12.16.
 */
public class ShmyakCPLEX extends Ranking {
    double alpha;
    int sizeAllGraph;
    String dirToCplex;

    public ShmyakCPLEX(List<Vertex> graph, List<ProbOfGraph> list, List<Vertex> module, double alpha, int sizeAllGraph, String dirToCplex) {
        super(graph, list, module);
        this.alpha = alpha;
        this.sizeAllGraph = sizeAllGraph;
        this.dirToCplex = dirToCplex;
    }

    @Override
    public void solve() {
        //answer
        this.ranking = absorb(new ArrayList<>(), new ArrayList<>(graph));
        calcAuc();
    }

    List<Vertex> absorb(List<Vertex> blackHole, List<Vertex> candidates) {
        List<Vertex> ranking = new ArrayList<>();
        int m = blackHole.size();
        int n = candidates.size();
        while (candidates.size() != 0) {
            List<Vertex> list = findMaxSG(blackHole, candidates);
            if (list.size() != 1) {
                list = absorb(blackHole, list);
            }
            for (Vertex v : list) {
                if (ranking.indexOf(v) < 0)
                    ranking.add(v);
                if (blackHole.indexOf(v) < 0)
                    blackHole.add(v);
                if (candidates.indexOf(v) >= 0)
                    candidates.remove(v);
            }
            /*ranking.addAll(list);
            blackHole.addAll(list);
            candidates.removeAll(list);
            */
            if (m + n != blackHole.size() + candidates.size()) {
                System.err.println("PROBLEMS!");
                System.exit(1);
            }
        }
        return ranking;
    }

    int i = 0;

    List<Vertex> findMaxSG(List<Vertex> must, List<Vertex> candidates) {
        DecimalFormat four = new DecimalFormat("#0.0000");

        long start = System.currentTimeMillis();
        if (candidates.size() == 1) {
            List<Vertex> list = new ArrayList<>();
            list.add(candidates.get(0));
            long end = System.currentTimeMillis();
            if (must.size() == 0)
                System.out.println(Thread.currentThread().getName() + " " + i + " : |G|=" + list.size() + ", |Must|=" + must.size() + ", |Cand|=" + candidates.size() + ", Time=" + ((end - start) / 1000.0));
            else
                System.out.println(Thread.currentThread().getName() + " " + i + " : |G|=" + list.size() + ", |Must|=" + must.size() + ", |Cand|=" + candidates.size() + ", AUC=" + four.format(Ranking.getAUCPub(Ranking.getRocListPub(module, must, sizeAllGraph))) + ", Time=" + ((end - start) / 1000.0));
            return list;
        }
        ++i;
        //directory
        String directory = "data/" + Thread.currentThread().getName();
        File nf = new File(directory);
        if (!nf.exists()) {
            nf.mkdirs();
        }
        String edges = directory + "/edges" + i + ".txt";
        String nodes = directory + "/nodes" + i + ".txt";
        String comp = directory + "/comp" + i + ".txt";
        String solution = directory + "/solution" + i + ".txt";
        try {
            PrintWriter pE = new PrintWriter(new File(edges));
            PrintWriter pN = new PrintWriter(new File(nodes));
            PrintWriter pC = new PrintWriter(new File(comp));
            List<Vertex> list = new ArrayList<>(must);
            list.addAll(candidates);
            Map<String, Integer> map = new HashMap<>(list.size());
            for (int j = 0; j < list.size(); j++) {
                map.put(list.get(j).getName(), j);
            }
            for (Vertex v : list) {
                pN.println(v.getName() + "\t" + ((alpha - 1) * Math.log(v.getWeight()) + Math.log(alpha)));
            }
            pN.flush();
            pN.close();
            for (Vertex v : must) {
                pC.println(v.getName());
            }
            pC.flush();
            pC.close();
            for (int i = 0; i < list.size(); ++i) {
                Vertex v = list.get(i);
                for (int j = 0; j < v.getVertices().size(); j++) {
                    int ind = list.indexOf(v.getVertices().get(j));
                    if (ind >= i) {
                        pE.println(v.getName() + "\t" + list.get(ind).getName());
                    }
                }
            }
            pE.flush();
            pE.close();

            List<Vertex> ret = new ArrayList<>();
            Pair<List<String>, Double> sol = cplexSolve(nodes, edges, comp, solution, candidates.size());
            for (String s : sol.getKey()) {
                int ind = map.get(s);
                if (ind >= must.size()) {
                    ret.add(list.get(ind));
                }
            }
            long end = System.currentTimeMillis();
            if (must.size() == 0)
                System.out.println(Thread.currentThread().getName() + " " + i + " : |G|=" + list.size() + ", |Must|=" + must.size() + ", |Cand|=" + candidates.size() + ", Time=" + ((end - start) / 1000.0));
            else
                System.out.println(Thread.currentThread().getName() + " " + i + " : |G|=" + list.size() + ", |Must|=" + must.size() + ", |Cand|=" + candidates.size() + ", AUC=" + four.format(Ranking.getAUCPub(Ranking.getRocListPub(module, must, sizeAllGraph))) + ", Time=" + ((end - start) / 1000.0));
            (new File(nodes)).delete();
            (new File(edges)).delete();
            (new File(comp)).delete();
            return ret;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    Pair<List<String>, Double> cplexSolve(String nodes, String edges, String comp, String solution, int size) {
        try {
            ProcessBuilder pb = new ProcessBuilder();
            if (size > 1900){
                pb.command("java", dirToCplex,
                        "-jar", "gmwcs.jar", "-m", "4", "-t", "300",
                        "-n", nodes, "-e", edges, "-s", solution, "-c", comp);
            }else{
                pb.command("java", dirToCplex,
                        "-jar", "gmwcs.jar", "-m", "4", "-t", "30",
                        "-n", nodes, "-e", edges, "-s", solution, "-c", comp);
            }
            Process process = pb.start();
            process.waitFor();
            process.destroy();

            List<String> names = new ArrayList<>();
            double objScore = 0;
            Scanner sc = new Scanner(new File(solution));
            while (sc.hasNext()) {
                String name = sc.next();
                if ('#' != name.charAt(0)) {
                    names.add(name);
                } else {
                    String score = sc.next();
                    objScore = Double.parseDouble(score);
                }
            }
            sc.close();
            (new File(solution)).delete();
            return new Pair<>(names, objScore);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
