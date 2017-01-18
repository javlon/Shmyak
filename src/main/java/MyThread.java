import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.commons.math3.util.Combinations;
import org.apache.commons.math3.util.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by javlon on 09.12.16.
 */
public class MyThread implements Runnable {
    List<List<Integer>> connectedSubgraphs;
    int powerOfModule;
    final int numberOfRuns;
    final int times;
    final String dataEdges;
    final String writeFile;
    final boolean fixedSizeModule;
    final boolean useSamplingDistrib;

    public MyThread(List<List<Integer>> connectedSubgraphs, int powerOfModule, int numberOfRuns, int times,
                    String dataEdges, String writeFile, boolean useSamplingDistrib) {
        this.connectedSubgraphs = connectedSubgraphs;
        this.powerOfModule = powerOfModule;
        this.numberOfRuns = numberOfRuns;
        this.times = times;
        this.dataEdges = dataEdges;
        this.writeFile = writeFile;
        if (powerOfModule > 0)
            this.fixedSizeModule = true;
        else {
            this.fixedSizeModule = false;
        }
        this.useSamplingDistrib = useSamplingDistrib;
    }

    private synchronized void write(String s) {
        PrintWriter printer = null;
        try {
            printer = new PrintWriter(new FileOutputStream(new File(writeFile), true));
            printer.println(s);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (printer != null) {
                printer.close();
            }
        }
    }

    @Override
    public void run() {
        UniformRealDistribution urd = new UniformRealDistribution(0.01, 0.5);

        for (int i = 0; i < numberOfRuns; i++) {
            double alpha = urd.sample();
            System.out.println(Thread.currentThread().getName() + " |:| alpha =  " + alpha);

            int cMethod = 3;
            double[] rms = new double[2 * cMethod];

            for (int j = 0; j < times; j++) {
                BetaDistribution bd = new BetaDistribution(alpha, 1);
                BetaDistribution ud = new BetaDistribution(1, 1);

                List<Vertex> graph = Main.read(dataEdges, null);
                if (!fixedSizeModule) {
                    powerOfModule = new Random().nextInt(graph.size() / 2) + 1;
                }

                List<Vertex> module = generateRandSGFixedSize(graph, powerOfModule);

                System.out.println(Thread.currentThread().getName() + " |:| it = " + i + "." + j + ",  |module| = " + powerOfModule);
                for (Vertex v : graph) {
                    if (module.contains(v)) {
                        v.setWeight(bd.sample());
                    } else {
                        v.setWeight(ud.sample());
                    }
                }

                List<Pair<List<Integer>, Pair<List<Integer>, List<Integer>>>> connectedSGwithLinks = new ArrayList<>();
                init(connectedSubgraphs, connectedSGwithLinks);

                List<ProbOfGraph> probList = null;
                List<ProbOfGraph> probListPrOp = null;
                if (useSamplingDistrib) {
                    Pair<List<List<Integer>>, List<Double>> we = distributionOfSG(graph, powerOfModule);
                    List<List<Integer>> Cnk = we.getKey();
                    List<Double> dist = we.getValue();

                    probList = generateProbWithDistr(graph, connectedSubgraphs, alpha, Cnk, dist);
                    probListPrOp = new ArrayList<>(probList).
                            stream().filter(x -> x.getGraph().size() == powerOfModule).collect(Collectors.toList());
                } else {
                    probList = generateProb(graph, connectedSubgraphs, alpha);
                    probListPrOp = probList;
                }

                List<Ranking> rankings = new ArrayList<>();
                rankings.add(new Shmyak(graph, probList, module, 0));
                rankings.add(new PrefixOptimal(graph, probListPrOp, module, connectedSGwithLinks));
                rankings.add(new ShmyakCPLEX(graph, probList, module, alpha));

                rankings.get(0).solve();
                rankings.get(2).solve();
                List<Vertex> r0 = rankings.get(0).getRanking();
                List<Vertex> r2 = rankings.get(2).getRanking();
                try {
                    PrintWriter pr = new PrintWriter(new FileOutputStream(new File("outp.txt"), true));
                    for (Vertex v: module) {
                        pr.print(v.getName() + " ");
                    }
                    pr.println(" : module");
                    for (Vertex v : r0){
                        pr.print(v.getName() + " ");
                    }
                    pr.println(" : shmyak, auc=" + rankings.get(0).getAuc());
                    for (Vertex v : r2){
                        pr.print(v.getName() + " ");
                    }
                    pr.println(" : shmyak with cplex, auc=" + rankings.get(2).getAuc());
                    for (int k = 0; k < r0.size(); k++) {
                        if (!r0.get(k).equals(r2.get(k))){
                            System.err.println("mismatched in pos: k=" + k + ", r0[k]=" + r0.get(k).getName() + ", r2[k]=" + r2.get(k).getName());
                            System.exit(1);
                        }
                    }
                    pr.println("OK!");
                    pr.flush();
                    pr.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
               /* for (int k = 0; k < cMethod; k++) {
                    rankings.get(k).solve();
                    rms[k] += Math.pow(rankings.get(k).getAuc(), 2);
                    rms[cMethod + k] += Math.pow(rankings.get(k).getMeanAuc(), 2);
                }*/

            }
            DecimalFormat four = new DecimalFormat("#0.0000");
            String scores = four.format(alpha) + " ";
            /*for (int j = 0; j < 2 * cMethod; j++) {
                rms[j] = Math.sqrt(rms[j] / times);
                scores += four.format(rms[j]) + " ";
            }*/
            write(scores);
        }
    }


    public static void init(List<List<Integer>> allConnectedSG, List<Pair<List<Integer>, Pair<List<Integer>, List<Integer>>>> connected) {
        for (int i = 0; i < allConnectedSG.size(); i++) {
            List<Integer> l = allConnectedSG.get(i);
            List<Integer> delVer = new ArrayList<>();
            List<Integer> links = new ArrayList<>();
            List<Integer> possibleSubg = new ArrayList<>(l);
            for (int j = 0; j < l.size(); j++) {
                int ver = possibleSubg.get(j);
                possibleSubg.remove(j);
                int pos = Collections.binarySearch(allConnectedSG, possibleSubg, new Comparator<List<Integer>>() {
                    @Override
                    public int compare(List<Integer> o1, List<Integer> o2) {
                        if (o1.size() < o2.size()) {
                            return -1;
                        } else if (o1.size() > o2.size()) {
                            return 1;
                        } else {
                            for (int k = 0; k < o1.size(); k++) {
                                if (o1.get(k) < o2.get(k)) {
                                    return -1;
                                } else if (o1.get(k) > o2.get(k)) {
                                    return 1;
                                }
                            }
                            return 0;
                        }
                    }
                });
                if (pos >= 0) {
                    delVer.add(ver);
                    links.add(pos);
                }
                possibleSubg.add(j, ver);
            }
            connected.add(new Pair<>(l, new Pair<List<Integer>, List<Integer>>(delVer, links)));
        }
    }


    //distribution of subgraphes by generatedRandSGFixedSize()
    public static Pair distributionOfSG(List<Vertex> graph, int countOfConnectedG) {
        List<List<Integer>> subGraphs = new ArrayList<>();
        List<Double> dist = new ArrayList<>();
        Combinations comb = new Combinations(graph.size(), countOfConnectedG);
        for (int[] nextComb : comb) {
            List<Vertex> subG = Arrays.stream(nextComb).boxed().map(graph::get).collect(Collectors.toList());
            if (checkToConnected(subG)) {
                subGraphs.add(Arrays.stream(nextComb).boxed().collect(Collectors.toList()));
                dist.add(0.0);
            }
        }
        int numbOfSamples = (int) 1e5;
        for (int i = 0; i < numbOfSamples; i++) {
            List<Vertex> module = generateRandSGFixedSize(graph, countOfConnectedG);
            List<Integer> indexes = module.stream().map(graph::indexOf).collect(Collectors.toList());
            Collections.sort(indexes);
            int ind = indexOf(subGraphs, indexes);
            dist.set(ind, dist.get(ind) + 1);
        }
        dist = dist.stream().map(x -> x / numbOfSamples).collect(Collectors.toList());
        return new Pair(subGraphs, dist);
    }

    //rewrite
    public static List<ProbOfGraph> generateProb(List<Vertex> graph, List<List<Integer>> lists, double alpha) {
        List<ProbOfGraph> subGs = new ArrayList<>();
        double sumP = 0.0;
        for (List<Integer> p : lists) {
            List<Vertex> selection = p.stream().map(graph::get).collect(Collectors.toList());
            double prob = getProb(selection, alpha);
            subGs.add(new ProbOfGraph(p, prob));
            sumP += prob;
        }
        for (ProbOfGraph p : subGs) {
            p.setProb(p.getProb() / sumP);
        }
        return subGs;
    }

    //|G|> 15  ???
    public static List<ProbOfGraph> generateProbWithDistr(List<Vertex> graph, List<List<Integer>> lists, double alpha,
                                                          List<List<Integer>> subLists, List<Double> distr) {
        List<ProbOfGraph> subGs = new ArrayList<>();
        double sumP = 0.0;
        for (List<Integer> p : lists) {
            if (p.size() != subLists.get(0).size()) {
                subGs.add(new ProbOfGraph(p, 0));
                continue;
            }
            List<Vertex> selection = p.stream().map(graph::get).collect(Collectors.toList());
            int ind = indexOf(subLists, p);
            double prob = getProb(selection, alpha) * distr.get(ind);
            subGs.add(new ProbOfGraph(p, prob));
            sumP += prob;
        }
        for (ProbOfGraph p : subGs) {
            p.setProb(p.getProb() / sumP);
        }
        return subGs;
    }

    public static int indexOf(List<List<Integer>> list, List<Integer> elem) {
        for (int i = 0; i < list.size(); i++) {
            List<Integer> el = list.get(i);
            if (elem.size() == el.size() && IntStream.range(0, elem.size()).filter(x -> el.get(x).equals(elem.get(x))).count() == 0) {
                return i;
            }
        }
        System.err.println("Couldn't find appropriate instance!");
        return -1;
    }

    //TODO: rewrite with log
    public static double getProb(List<Vertex> subGraph, double alpha) {
        return subGraph.stream().mapToDouble(x -> alpha * Math.pow(x.getWeight(), alpha - 1)).reduce(1, (x, y) -> x * y);
    }

    //TODO: rewrite
    public static boolean checkToConnected(List<Vertex> graph) {
        List<Vertex> list = new ArrayList<>(graph);
        List<Vertex> connectedPart = new ArrayList<>();
        connectedPart.add(list.get(0));
        list.remove(0);
        for (int i = 0; i < connectedPart.size(); i++) {
            for (Vertex v : connectedPart.get(i).getVertices()) {
                if (list.contains(v)) {
                    list.remove(v);
                    connectedPart.add(v);
                }
            }
        }
        return list.size() == 0;
    }

    //Generate random connected sub graph
    public static List<Vertex> generateRandSGUniformDistr(List<List<Integer>> pow2graph, List<Vertex> graph) {
        Random rand = new Random();
        return pow2graph.get(rand.nextInt(pow2graph.size())).stream().map(graph::get).collect(Collectors.toList());
    }

    public static List<Vertex> generateRandSGFixedSize(List<Vertex> graph, int number) {
        if (graph.size() < number || number < 0) {
            throw new IllegalArgumentException("Size of list less than number");
        }
        List<Vertex> list = new ArrayList<>(graph);
        Random rand = new Random();
        while (true) {
            Collections.shuffle(list);
            List<Vertex> connected = new ArrayList<>();
            List<Vertex> canAdd = new ArrayList<>();
            int addMe = rand.nextInt(list.size());
            connected.add(list.get(addMe));
            canAdd.addAll(list.get(addMe).getVertices());
            while (connected.size() < number) {
                if (canAdd.size() == 0)
                    break;
                addMe = rand.nextInt(canAdd.size());
                Vertex v = canAdd.get(addMe);
                connected.add(v);
                canAdd.remove(v);
                v.getVertices().stream().filter(nv -> !(connected.contains(nv) || canAdd.contains(nv))).forEach(canAdd::add);
            }
            if (connected.size() == number) {
                return connected;
            }
        }
    }
}