import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by javlon on 26.01.17.
 */
public class ShmyakThread implements Runnable {
    String writeFile;
    String dirToCplex;
    int runC;
    int from;
    int to;

    public ShmyakThread(String writeFile, String dirToCplex, int runC, int from, int to) {
        this.writeFile = writeFile;
        this.dirToCplex = dirToCplex;
        this.runC = runC;
        this.from = from;
        this.to = to;
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
        for (int i = 0; i < runC; i++) {
            try {
                System.out.println();
                callMe();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        System.err.println(Thread.currentThread().getName() + " done!");
    }

    private void callMe() throws FileNotFoundException {
        List<Vertex> graph = Main.read("in" + Thread.currentThread().getName() + ".txt", null);
        List<List<Vertex>> comps = findComps(graph);
        Collections.sort(comps, Comparator.comparingInt(x -> x.size()));
        Collections.reverse(comps);

        UniformRealDistribution urd = new UniformRealDistribution(0.01, 0.5);
        double alpha = urd.sample();

        System.out.println(Thread.currentThread().getName() + " alpha = " + alpha);
        BetaDistribution bd = new BetaDistribution(alpha, 1);
        BetaDistribution ud = new BetaDistribution(1, 1);

        Random rand = new Random();
        int powerOfModule = rand.nextInt(to - from + 1) + from;
        List<Vertex> module = MyThread.generateRandSGFixedSize(graph, powerOfModule);
        PrintWriter printWriter = new PrintWriter(new File("module" + Thread.currentThread().getName() + ".txt"));
        for (Vertex v : module) {
            printWriter.println(v.getName());
        }
        printWriter.flush();
        printWriter.close();

        PrintWriter printer = new PrintWriter(new File("weights" + Thread.currentThread().getName() + ".txt"));
        for (Vertex v : graph) {
            if (module.contains(v))
                v.setWeight(bd.sample());
            else
                v.setWeight(ud.sample());
            printer.println(v.getName() + " " + v.getWeight());
        }
        printer.flush();
        printer.close();


        List<List<Vertex>> ranked = new ArrayList<>();
        for (int j = 0; j < 3; j++) {
            ranked.add(new ArrayList<>());
        }
        long time = 0;

        PrintWriter pwS = new PrintWriter(new File("sOut" + Thread.currentThread().getName() + ".txt"));
        PrintWriter pwM = new PrintWriter(new File("mOut" + Thread.currentThread().getName() + ".txt"));
        PrintWriter pwSh = new PrintWriter(new File("shOut" + Thread.currentThread().getName() + ".txt"));
        for (int j = 0; j < comps.size(); ++j) {
            List<Ranking> rs = new ArrayList<>();
            rs.add(new Simple(comps.get(j), null, module));
            rs.add(new MWCS(comps.get(j), null, module));
            rs.add(new ShmyakCPLEX(comps.get(j), null, module, alpha, graph.size(), dirToCplex));

            rs.get(0).solve();
            for (Vertex v : rs.get(0).getRanking()) {
                pwS.println(v.getName());
            }
            pwS.flush();
            ranked.get(0).addAll(rs.get(0).getRanking());

            rs.get(1).solve();
            for (Vertex v : rs.get(1).getRanking()) {
                pwM.println(v.getName());
            }
            pwM.flush();
            ranked.get(1).addAll(rs.get(1).getRanking());

            long start = System.currentTimeMillis();
            rs.get(2).solve();
            time += System.currentTimeMillis() - start;
            for (Vertex v : rs.get(2).getRanking()) {
                pwSh.println(v.getName());
            }
            pwSh.flush();
            ranked.get(2).addAll(rs.get(2).getRanking());
        }
        DecimalFormat four = new DecimalFormat("#0.00000");
        String aucs = four.format(alpha);
        for (int j = 0; j < ranked.size(); j++) {
            aucs += " " + four.format(Ranking.getAUC(Ranking.getRocList(module, ranked.get(j))));
        }
        aucs += " " + time;
        write(aucs);
    }

    static void dfs(int i, List<List<Integer>> links, boolean[] used, List<Integer> comp) {
        used[i] = true;
        comp.add(i);
        for (int j = 0; j < links.get(i).size(); j++) {
            int to = links.get(i).get(j);
            if (!used[to]) {
                dfs(to, links, used, comp);
            }
        }
    }

    static List<List<Vertex>> findComps(List<Vertex> list) {
        Map<String, Integer> map = new HashMap<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            map.put(list.get(i).getName(), i);
        }

        List<List<Integer>> links = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            links.add(new ArrayList<>());
            Vertex v = list.get(i);
            for (Vertex u : v.getVertices()) {
                links.get(i).add(map.get(u.getName()));
            }
        }

        List<List<Vertex>> ret = new ArrayList<>();
        boolean[] used = new boolean[list.size()];
        for (int i = 0; i < list.size(); i++) {
            if (!used[i]) {
                List<Integer> comp = new ArrayList<>();
                dfs(i, links, used, comp);
                List<Vertex> vComp = comp.stream().map(list::get).collect(Collectors.toList());
                ret.add(vComp);
            }
        }
        return ret;
    }
}
