import data.Data;

import java.time.LocalTime;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        parseData();
    }

    public static void parseData() {


        for(int i = 7; i < 11; i++) {
            System.out.println("======================================================================");
            LocalTime start = LocalTime.now();
            System.out.println("Iteration " + i + " started at " + start.toString());
            Parser parser = new Parser();
            parser.addPath("C:\\Users\\david\\PycharmProjects\\szakdoga\\test");
            parser.setFolder();
            long startTime = System.nanoTime();
            List<Data> dataList = parser.parseKimInstances();
            Solver solver = new Solver(dataList);
            Logger logger;
            for (Data data : dataList) {
                System.out.println("---------------------------------------------------------------------------");
                System.out.println("Solving " + data.getInfo());
                logger = new Logger();
                logger.setPath("C:\\Users\\david\\Documents\\Szakdoga\\results\\final\\" + data.getInfo() + "_" + i +".txt");
                LocalTime greedyStart = LocalTime.now();
                System.out.println("Greedy started at " + greedyStart.toString());
                solver.initGreedy(data, logger);
                LocalTime greedyEnd = LocalTime.now();
                LocalTime ALNSStart = LocalTime.now();
                System.out.println("Greedy ended at " + greedyEnd + ", ALNS started at " + ALNSStart);
                solver.ALNS(data, logger);
                LocalTime ALNSEnd = LocalTime.now();
                System.out.println("ALNS ended at " + ALNSEnd);
                logger.writeFile();
            }
            long endTime = System.nanoTime();
            System.out.println("Solving iteration " + i + " took " + ((endTime - startTime) * 1e-9) + " seconds");
            LocalTime end = LocalTime.now();
            System.out.println("Iteration " + i + " ended at " + end.toString());
        }
    }

}