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
    List<List<Integer>> pow2graph;
    int powerOfModule;
    final int numberOfRuns;
    final int times;
    final String dataEdges;
    final String writeFile;

    public MyThread(List<List<Integer>> pow2graph, int powerOfModule, int numberOfRuns, int times, String dataEdges, String writeFile) {
        this.pow2graph = pow2graph;
        this.powerOfModule = powerOfModule;
        this.numberOfRuns = numberOfRuns;
        this.times = times;
        this.dataEdges = dataEdges;
        this.writeFile = writeFile;
    }

    @Override
    public void run() {
        UniformRealDistribution urd = new UniformRealDistribution(0.01, 0.5);

        List<Vertex> graph = read(dataEdges, null);

        Pair<List<List<Integer>>, List<Double>> we = distrOFSG(graph, powerOfModule);
        List<List<Integer>> Cnk = we.getKey();
        List<Double> dist = we.getValue();

        List<Pair<List<Integer>, Pair<List<Integer>, List<Integer>>>> connectedSGwithLinks = new ArrayList<>();
        init(pow2graph, connectedSGwithLinks);

        for (int i = 0; i < numberOfRuns; i++) {
            double alpha = urd.sample();
            System.out.println(Thread.currentThread().getName() + " |:| alpha =  " + alpha);

            int cMethod = 2;
            double[] rms = new double[2 * cMethod];

            graph = read(dataEdges, null);// reads graph -> weigths become zero
            List<Vertex> connected = generateRandSGFixedSize(graph, powerOfModule);
            powerOfModule = connected.size();

            for (int j = 0; j < times; j++) {
                System.out.println(Thread.currentThread().getName() + " |:| it = " + i + "." + j);
                BetaDistribution bd = new BetaDistribution(alpha, 1);
                BetaDistribution ud = new BetaDistribution(1, 1);

                for (Vertex v : graph) {
                    if (connected.contains(v)) {
                        v.setWeight(bd.sample());
                    } else {
                        v.setWeight(ud.sample());
                    }
                }

                List<ProbOfGraph> list = getSGwithDistr(graph, pow2graph, alpha, Cnk, dist);

                List<ProbOfGraph> notZeroProbSG = new ArrayList<>();
                for (ProbOfGraph p : list) {
                    if (p.getGraph().size() == powerOfModule)
                        notZeroProbSG.add(p);
                }

                List<Ranking> rankings = new ArrayList<>();
                rankings.add(new Shmyak(graph, list, connected, 0));
                rankings.add(new PrefixOptimal(graph, notZeroProbSG, connected, connectedSGwithLinks));

                for (int k = 0; k < cMethod; k++) {
                    rankings.get(k).solve();
                    rms[k] += Math.pow(rankings.get(k).getAuc(), 2);
                    rms[cMethod + k] += Math.pow(rankings.get(k).getMeanAuc(), 2);
                }

            }
            DecimalFormat four = new DecimalFormat("#0.0000");
            String scores = four.format(alpha) + " ";
            for (int j = 0; j < cMethod * 2; j++) {
                rms[j] = Math.sqrt(rms[j] / times);
                scores += four.format(rms[j]) + " ";
            }
            write(scores);
        }
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

    public static List<Vertex> read(String fileEdges, String fileNodes) {
        Scanner sc = null;
        try {
            sc = new Scanner(new File(fileEdges)).useDelimiter("(\\n|\\s+)");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Map<String, Vertex> vertexes = new HashMap<>();
        while (sc.hasNext()) {
            String left = sc.next();
            String right = sc.next();
            Vertex from = vertexes.get(left);
            Vertex to = vertexes.get(right);
            if (from == null) {
                Vertex v = new Vertex(left);
                vertexes.put(left, v);
                from = v;
            }
            if (to == null) {
                Vertex v = new Vertex(right);
                vertexes.put(right, v);
                to = v;
            }
            from.addVertex(to);
            to.addVertex(from);
        }
        if (fileNodes != null) {
            try {
                sc = new Scanner(new File(fileNodes)).useDelimiter("(\\n|\\s+)");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            while (sc.hasNext()) {
                String node = sc.next();
                String weight = sc.next();
                Vertex v = vertexes.get(node);
                v.setWeight(new Double(weight));
            }
        }
        List<Vertex> ret = new ArrayList<Vertex>(vertexes.values());
        Collections.sort(ret, new Comparator<Vertex>() {
            @Override
            public int compare(Vertex o1, Vertex o2) {
                return Integer.parseInt(o1.getName()) - Integer.parseInt(o2.getName());
            }
        });
        return ret;
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


    public static Pair distrOFSG(List<Vertex> graph, int countOfConnectedG) {
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
        int numbOfSamples = (int) 1e6;
        for (int i = 0; i < numbOfSamples; i++) {
            List<Vertex> module = generateRandSGFixedSize(graph, countOfConnectedG);
            List<Integer> indexes = module.stream().map(graph::indexOf).collect(Collectors.toList());
            Collections.sort(indexes);
            int ind = getIndex(subGraphs, indexes);
            dist.set(ind, dist.get(ind) + 1);
        }
        dist = dist.stream().map(x -> x / numbOfSamples).collect(Collectors.toList());
        return new Pair(subGraphs, dist);
    }

    public static List<List<Integer>> getConnectedSG(List<Vertex> graph) {
        List<List<Integer>> list = new ArrayList<>();
        for (int i = 1; i <= graph.size(); i++) {
            Combinations comb = new Combinations(graph.size(), i);
            for (int[] nextComb : comb) {
                List<Vertex> subG = Arrays.stream(nextComb).boxed().map(graph::get).collect(Collectors.toList());
                if (checkToConnected(subG)) {
                    list.add(Arrays.stream(nextComb).boxed().collect(Collectors.toList()));
                }
            }
        }
        return list;
    }

    public static List<List<Integer>> getConnectedSG(List<Vertex> graph, int k) {
        List<List<Integer>> list = new ArrayList<>();
        for (int i = 1; i <= k; i++) {
            Combinations comb = new Combinations(graph.size(), i);
            for (int[] nextComb : comb) {
                List<Vertex> subG = Arrays.stream(nextComb).boxed().map(graph::get).collect(Collectors.toList());
                if (checkToConnected(subG)) {
                    list.add(Arrays.stream(nextComb).boxed().collect(Collectors.toList()));
                }
            }
        }
        return list;
    }

    public static void getAllSG(List<List<Integer>> lists, List<Vertex> graph, List<Integer> cur) {
        if (cur.size() == 0) {
            return;
        }
        if (!checkToConnected(cur.stream().map(graph::get).collect(Collectors.toList()))) {
            return;
        }
        for (List<Integer> list : lists) {
            if (list.size() == cur.size()) {
                if (IntStream.range(0, cur.size()).filter(x -> list.get(x) != cur.get(x)).count() == 0) {
                    return;
                }
            }
        }
        for (int i = 0; i < cur.size(); i++) {
            int j = cur.get(i);
            cur.remove(i);
            getAllSG(lists, graph, cur);
            cur.add(i, j);
        }
        lists.add(new ArrayList<>(cur));
    }

    //returns collection of arrays  which sorted
    public static void generateConnectedSubgraphs(List<List<Integer>> ret, List<Vertex> graph, List<Integer> verticesNotYetConsidered,
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
            boolean contains = false;
            for (List<Integer> list : ret) {
                if (list.size() == smth.size()) {
                    if (IntStream.range(0, list.size()).filter(x -> list.get(x) != smth.get(x)).count() == 0) {
                        contains = true;
                    }
                }
            }
            if (!contains)
                ret.add(smth);
        } else {
            int chosenVertex = candidates.get(candidates.size() - 1);
            verticesNotYetConsidered.remove(Integer.valueOf(chosenVertex));
            generateConnectedSubgraphs(ret, graph, verticesNotYetConsidered, connected, neighbors);
            List<Integer> newNeighbors = Shmyak.union(neighbors, graph.get(chosenVertex).getVertices().stream().map(x -> graph.indexOf(x)).collect(Collectors.toList()));
            connected.add(chosenVertex);
            generateConnectedSubgraphs(ret, graph, verticesNotYetConsidered, connected, newNeighbors);
            connected.remove(connected.size() - 1);
            verticesNotYetConsidered.add(chosenVertex);
        }
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


    public static List<ProbOfGraph> getAllSG(List<Vertex> graph, List<List<Integer>> lists, double alpha) {
        List<ProbOfGraph> list = new ArrayList<>();
        double sumP = 0.0;
        for (List<Integer> p : lists) {
            List<Vertex> selection = p.stream().map(graph::get).collect(Collectors.toList());
            double prob = getProb(selection, alpha);
            list.add(new ProbOfGraph(p, prob));
            sumP += prob;
        }
        for (ProbOfGraph p : list) {
            p.setProb(p.getProb() / sumP);
        }
        return list;
    }

    //rewrite
    public static List<ProbOfGraph> getAllSGwithDistr(List<Vertex> graph, List<List<Integer>> lists, double alpha,
                                                      List<List<Integer>> subLists, List<Double> distr) {
        List<ProbOfGraph> subGs = new ArrayList<>();
        double sumP = 0.0;
        for (List<Integer> p : lists) {
            if (p.size() != subLists.get(0).size()) {
                subGs.add(new ProbOfGraph(p, 0));
                continue;
            }
            List<Vertex> selection = p.stream().map(graph::get).collect(Collectors.toList());
            int ind = getIndex(subLists, p);
            double prob = getProb(selection, alpha) * distr.get(ind);
            subGs.add(new ProbOfGraph(p, prob));
            sumP += prob;
        }
        for (ProbOfGraph p : subGs) {
            p.setProb(p.getProb() / sumP);
        }
        return subGs;
    }

    //|G|> 15
    public static List<ProbOfGraph> getSGwithDistr(List<Vertex> graph, List<List<Integer>> lists, double alpha,
                                                   List<List<Integer>> subLists, List<Double> distr) {
        List<ProbOfGraph> subGs = new ArrayList<>();
        double sumP = 0.0;
        for (List<Integer> p : lists) {
            if (p.size() != subLists.get(0).size()) {
                subGs.add(new ProbOfGraph(p, 0));
                continue;
            }
            List<Vertex> selection = p.stream().map(graph::get).collect(Collectors.toList());
            int ind = getIndex(subLists, p);
            double prob = getProb(selection, alpha) * distr.get(ind);
            subGs.add(new ProbOfGraph(p, prob));
            sumP += prob;
        }
        for (ProbOfGraph p : subGs) {
            p.setProb(p.getProb() / sumP);
        }
        return subGs;
    }

    public static int getIndex(List<List<Integer>> bigList, List<Integer> list) {
        for (int i = 0; i < bigList.size(); i++) {
            List<Integer> el = bigList.get(i);
            if (list.size() != el.size())
                continue;
            for (int j = 0; j < list.size(); j++) {
                if (list.get(j) != el.get(j)) {
                    break;
                } else if (j == list.size() - 1) {
                    return i;
                }
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