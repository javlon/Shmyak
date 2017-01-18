import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by javlon on 15.12.16.
 */
public class ShmyakCPLEX extends Ranking {
    double alpha;

    public ShmyakCPLEX(List<Vertex> graph, List<ProbOfGraph> list, List<Vertex> module, double alpha) {
        super(graph, list, module);
        this.alpha = alpha;
    }

    @Override
    public void solve() {
        //answer
        try {
            PrintWriter printer = new PrintWriter(new File("ovj.txt"));
            printer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        this.ranking = absorb(new ArrayList<>(), graph);
        calcAuc();
    }

    List<Vertex> absorb(List<Vertex> blackHole, List<Vertex> candidates) {
        List<Vertex> ranking = new ArrayList<>();
        int m = blackHole.size();
        int n = candidates.size();
        while (candidates.size() != 0) {
            List<Vertex> list = findMaxSG(blackHole, candidates);
            int l = list.size();
            System.err.println("l=" + l);
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

    class Edge {
        int from;
        int to;
        Edge twin;
        IloIntVar var;

        public Edge(int from, int to, Edge twin, IloIntVar var) {
            this.from = from;
            this.to = to;
            this.twin = twin;
            this.var = var;
        }

        public void setTwin(Edge twin) {
            this.twin = twin;
        }

        public IloIntVar getVar() {
            return var;
        }

        public IloIntVar getTwinVar() {
            return twin.var;
        }
    }

    // |candidates| >= 1 ? 2  &
    int i = 0;

    List<Vertex> findMaxSG(List<Vertex> must, List<Vertex> candidates) {
        try {
            ++i;
            PrintWriter pE = new PrintWriter(new File("instances/edges" + i + ".txt"));
            PrintWriter pN = new PrintWriter(new File("instances/nodes" + i + ".txt"));
            PrintWriter pC = new PrintWriter(new File("instances/comp" + i + ".txt"));
            List<Vertex> list = new ArrayList<>(must);
            list.addAll(candidates);
            for (Vertex v : list) {
                pN.println(v.getName() + "\t" + ((alpha - 1) * Math.log(v.getWeight()) + Math.log(alpha)));
            }
            pN.flush();
            pN.close();
            for (Vertex v : must){
                pC.println(v.getName());
            }
            pC.flush();
            pC.close();
            for (int i = 0; i < list.size(); ++i){
                Vertex v = list.get(i);
                for (int j = 0; j < v.getVertices().size(); j++) {
                    int ind = list.indexOf(v.getVertices().get(j));
                    if (ind >= i){
                        pE.println(v.getName() + "\t" + list.get(ind).getName());
                    }
                }
            }
            pE.flush();
            pE.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        if (candidates.size() == 1) {
            try {
                PrintWriter pW = new PrintWriter(new File("instances/weight" + i + ".txt"));
                pW.println("no");
                pW.flush();
                pW.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            return new ArrayList<>(candidates);
        }
        try {
            IloCplex cplex = new IloCplex();
            List<Vertex> list = new ArrayList<>(must);
            list.addAll(candidates);

            int mn = must.size();
            int cn = candidates.size();
            int n = mn + cn;
            System.err.println("N = " + n);
            IloIntVar[] ym = cplex.boolVarArray(mn);
            IloIntVar[] yc = cplex.boolVarArray(cn);
            IloIntVar[] y = new IloIntVar[n];
            System.arraycopy(ym, 0, y, 0, mn);
            System.arraycopy(yc, 0, y, mn, cn);
            IloIntVar[] r = cplex.boolVarArray(n);
            IloNumVar[] d = cplex.numVarArray(n, 1, n); // (3)
            int bs = 2 * n;

            List<List<Edge>> x = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                x.add(new ArrayList<>());
            }
            for (int i = 0; i < list.size(); i++) {
                for (int j = 0; j < list.get(i).getVertices().size(); j++) {
                    int ind = list.indexOf(list.get(i).getVertices().get(j));
                    if (ind >= i) {
                        Edge from = new Edge(ind, i, null, cplex.boolVar());
                        Edge to = new Edge(i, ind, from, cplex.boolVar());
                        from.setTwin(to);
                        x.get(i).add(from);
                        x.get(ind).add(to);
                        bs += 2;
                    }
                }
            }
            System.err.println("error:" + bs);
            //System.exit(1);
            // constraints
            // (2)
            cplex.addEq(cplex.sum(r), 1);
            // (4)
            for (int i = 0; i < n; i++) {
                IloIntVar[] sumx = x.get(i).stream().map(Edge::getVar).toArray(IloIntVar[]::new);
                cplex.addEq(cplex.sum(r[i], cplex.sum(sumx)), y[i]);
            }
            // (5)
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < x.get(i).size(); j++) {
                    int k = x.get(i).get(j).from;
                    if (k >= i) {
                        cplex.addLe(cplex.prod(2, cplex.sum(x.get(i).get(j).getVar(), x.get(i).get(j).getTwinVar())),
                                cplex.sum(y[i], y[k]));
                    }
                }
            }
            // (8)
            for (int i = 0; i < n; i++) {
                cplex.addLe(cplex.sum(d[i], cplex.prod(n, r[i])), n + 1);
            }
            // (9) (10)
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < x.get(i).size(); j++) {
                    int k = x.get(i).get(j).from;
                    cplex.addGe(cplex.sum(n, d[i]), cplex.sum(cplex.prod(n + 1, x.get(i).get(j).getVar()), d[k]));
                    cplex.addGe(cplex.sum(n, d[k]), cplex.sum(cplex.prod(n - 1, x.get(i).get(j).getVar()), d[i]));
                }
            }

            // (11)
            IloNumExpr rk = cplex.numExpr();
            for (int i = 0; i < n; i++) {
                cplex.addLe(cplex.sum(rk, y[i]), 1);
                rk = cplex.sum(rk, r[i]);
            }

            // (12)
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < x.get(i).size(); j++) {
                    int k = x.get(i).get(j).from;
                    cplex.addLe(cplex.sum(d[k], cplex.prod(n - 1, cplex.sum(y[i], y[k]))), cplex.sum(2 * n - 1, d[i]));
                }
            }

            // candidates and must
            if (mn > 0)
                cplex.addLe(1, cplex.sum(ym));
            cplex.addRange(1, cplex.sum(yc), cn - 1);


            double[] wm = must.stream().mapToDouble(Vertex::getWeight).map(xx -> (alpha - 1) * Math.log(xx) + Math.log(alpha)).toArray();
            double[] wc = candidates.stream().mapToDouble(Vertex::getWeight).map(xx -> (alpha - 1) * Math.log(xx) + Math.log(alpha)).toArray();
            double[] w = new double[wm.length + wc.length];
            System.arraycopy(wm, 0, w, 0, wm.length);
            System.arraycopy(wc, 0, w, wm.length, wc.length);

            //objective
            cplex.addMaximize(cplex.scalProd(y, w));

            cplex.solve();
            List<Vertex> ret = new ArrayList<>();
            double[] choosed = cplex.getValues(yc);
            PrintWriter printer = new PrintWriter(new FileOutputStream(new File("ovj.txt"), true));
            for (int i = 0; i < cn; i++) {
                if (choosed[i] >= 0.5) {
                    ret.add(candidates.get(i));
                    printer.print(candidates.get(i).getName() + " ");
                }
            }

            double objValue = cplex.getObjValue();
            printer.println("\n" + objValue);
            printer.flush();
            printer.close();
            cplex.end();


            try {
                PrintWriter pW = new PrintWriter(new File("instances/weight" + i + ".txt"));
                pW.println(objValue);
                pW.flush();
                pW.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            return ret;
        } catch (IloException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

}
