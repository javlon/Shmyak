import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

/**
 * Created by javlon on 20.12.16.
 */
public class ShmyakMain {
    static String dirToCplex;
    public static void main(String[] args) throws FileNotFoundException {
        dirToCplex = args[0];
        int time = Integer.parseInt(args[1]);
        int nThread = Integer.parseInt(args[2]);
        int from = Integer.parseInt(args[3]);
        int to = Integer.parseInt(args[4]);

        String writeAUC = "AUC.txt";
        PrintWriter pw = new PrintWriter(new File(writeAUC));
        pw.println("#alpha baseline mwcs    shmyak");
        pw.flush();
        pw.close();
        for (int i = 0; i < nThread; i++) {
            Thread t = new Thread(new ShmyakThread(writeAUC, dirToCplex, time, from, to), "T" + i);
            t.start();
        }
    }

}