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
    final String dirToCplex;

    public MyThread(int powerOfModule, int numberOfRuns, int times,
                    String dataEdges, String writeFile, String dirToCplex) {
        this.powerOfModule = powerOfModule;
        this.numberOfRuns = numberOfRuns;
        this.times = times;
        this.dataEdges = dataEdges;
        this.writeFile = writeFile;
        this.dirToCplex = dirToCplex;
        if (powerOfModule > 0)
            this.fixedSizeModule = true;
        else {
            this.fixedSizeModule = false;
        }
        this.connectedSubgraphs = sortedConnectedSubgraphs();
        System.out.println("Count of connected sub graphs: " + connectedSubgraphs.size());
    }

    private synchronized void write(String s) {
        PrintWriter printer = null;
        try {
            printer = new PrintWriter(new FileOutputStream(new File(writeFile), true));
            printer.println(s);
            printer.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (printer != null) {
                printer.close();
            }
        }
    }
    public void setWeights(List<Vertex> graph, List<Vertex> module, double alpha){
        BetaDistribution bd = new BetaDistribution(alpha, 1);
        BetaDistribution ud = new BetaDistribution(1, 1);
        for (Vertex v : graph) {
            if (module.contains(v)) {
                v.setWeight(bd.sample());
            } else {
                v.setWeight(ud.sample());
            }
        }
    }

    @Override
    public void run() {
        UniformRealDistribution urd = new UniformRealDistribution(0.01, 0.5);

        for (int i = 0; i < numberOfRuns; i++) {
            double alpha = urd.sample();
            System.out.println(Thread.currentThread().getName() + " |:| alpha =  " + alpha);

            int cMethod = 5;
            double[] rms = new double[2 * cMethod];

            for (int j = 0; j < times; j++) {

                List<Vertex> graph = Main.read(dataEdges, null);
                if (!fixedSizeModule) {
                    powerOfModule = new Random().nextInt(graph.size() / 2) + 1;
                }

                boolean isUniform = false;
                List<Vertex> module = isUniform ? genUniformRand(graph, connectedSubgraphs, powerOfModule, powerOfModule)
                                                : generateRandSGFixedSize(graph, powerOfModule);
                System.out.println(Thread.currentThread().getName() + " |:| it = " + i + "." + j + ",  |module| = " + powerOfModule);

                setWeights(graph, module, alpha);

                List<Pair<List<Integer>, Pair<List<Integer>, List<Integer>>>> connectedSGwithLinks = new ArrayList<>();
                init(connectedSubgraphs, connectedSGwithLinks);

                List<ProbOfGraph> probList = null;
                //if (isUniform){
                    probList = genUniformProb(graph, connectedSubgraphs, alpha);
                //}else {
                    Pair<List<List<Integer>>, List<Double>> we = distributionOfSG(graph, powerOfModule);
                    List<List<Integer>> Cnk = we.getKey();
                    List<Double> dist = we.getValue();

                    List<ProbOfGraph> distrProbList = generateProbWithDistr(graph, connectedSubgraphs, alpha, Cnk, dist);
                    List<ProbOfGraph> distrProbListOp= new ArrayList<>(distrProbList).
                            stream().filter(x -> x.getGraph().size() == powerOfModule).collect(Collectors.toList());
                //}

                List<Ranking> rankings = new ArrayList<>();
                rankings.add(new Simple(graph, probList, module));
                rankings.add(new MWCS(graph, probList, module));
                rankings.add(new ShmyakCPLEX(graph, probList, module, alpha, graph.size(), dirToCplex));
                rankings.add(new PrefixOptimal(graph, probList, module, connectedSGwithLinks));
                rankings.add(new PrefixOptimal(graph, distrProbListOp, module, connectedSGwithLinks));

                long start = System.currentTimeMillis();
                for (int k = 0; k < cMethod; k++) {
                    rankings.get(k).solve();
                    rankings.get(k).calcMeanAuc();
                    rms[k] += Math.pow(rankings.get(k).getAuc(), 2);
                    rms[cMethod + k] += Math.pow(rankings.get(k).getMeanAuc(), 2);
                }
                System.out.println(System.currentTimeMillis() - start);

            }
            DecimalFormat four = new DecimalFormat("#0.0000");
            String scores = four.format(alpha) + " | ";
            for (int j = 0; j < cMethod; j++) {
                rms[j] = Math.sqrt(rms[j] / times);
                scores += four.format(rms[j]) + " ";
            }
            scores += " | ";
            for (int j = 0; j < cMethod; j++) {
                rms[cMethod + j] = Math.sqrt(rms[cMethod + j] / times);
                scores += four.format(rms[cMethod + j]) + " ";
            }
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

    public static List<ProbOfGraph> genUniformProb(List<Vertex> graph, List<List<Integer>> lists, double alpha){
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

    public static List<Vertex> genUniformRand(List<Vertex> graph, List<List<Integer>> connectedGraphs, int fromSize, int toSize){
        List<Integer> set = new ArrayList<>();
        for (int i = 0; i < connectedGraphs.size(); i++) {
            int size = connectedGraphs.get(i).size();
            if (size >= fromSize && size <= toSize)
                set.add(i);
        }
        Random rand = new Random();
        int ind = rand.nextInt(set.size());
        return connectedGraphs.get(ind).stream().map(graph::get).collect(Collectors.toList());
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

    public List<List<Integer>> sortedConnectedSubgraphs() {
        List<Vertex> graph = Main.read(dataEdges, null);
        Set<List<Integer>> pow2graphSet = new HashSet<>();
        generateConnectedSubgraphsSet(pow2graphSet, graph, IntStream.range(0, graph.size()).boxed().collect(Collectors.toList()), new ArrayList<>(), new ArrayList<>());
        List<List<Integer>> pow2graph = new ArrayList<>(pow2graphSet);
        Collections.sort(pow2graph, new Comparator<List<Integer>>() {
            @Override
            public int compare(List<Integer> o1, List<Integer> o2) {
                if (o1.size() < o2.size()) {
                    return -1;
                } else if (o1.size() > o2.size()) {
                    return 1;
                } else {
                    for (int i = 0; i < o1.size(); i++) {
                        if (o1.get(i) < o2.get(i)) {
                            return -1;
                        } else if (o1.get(i) > o2.get(i))
                            return 1;
                    }
                    return 0;
                }
            }
        });
        return pow2graph;
    }

    public static void generateConnectedSubgraphsSet(Set<List<Integer>> ret, List<Vertex> graph, List<Integer> verticesNotYetConsidered,
                                                     List<Integer> connected, List<Integer> neighbors) {
        List<Integer> candidates = null;
        if (connected.size() == 0) {
            candidates = new ArrayList<>(verticesNotYetConsidered);
        } else {
            candidates = Shmyak.intersection(verticesNotYetConsidered, neighbors);
        }
        if (candidates.size() == 0) {
            if (connected.size() == 0)
                return;
            List<Integer> smth = new ArrayList<>(connected);
            Collections.sort(smth);
            if (!ret.contains(smth))
                ret.add(smth);
        } else {
            int chosenVertex = candidates.get(candidates.size() - 1);
            verticesNotYetConsidered.remove(Integer.valueOf(chosenVertex));
            generateConnectedSubgraphsSet(ret, graph, verticesNotYetConsidered, connected, neighbors);
            List<Integer> newNeighbors = Shmyak.union(neighbors, graph.get(chosenVertex).getVertices().stream().map(x -> graph.indexOf(x)).collect(Collectors.toList()));
            connected.add(chosenVertex);
            generateConnectedSubgraphsSet(ret, graph, verticesNotYetConsidered, connected, newNeighbors);
            connected.remove(connected.size() - 1);
            verticesNotYetConsidered.add(chosenVertex);
        }
    }


}