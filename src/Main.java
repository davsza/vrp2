import data.Data;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        Parser parser = new Parser();
        // converted if all files, else 'test'
        //parser.addPath("C:\\Users\\david\\PycharmProjects\\szakdoga\\converted");
        parser.addPath("C:\\Users\\david\\PycharmProjects\\szakdoga\\test");
        parser.setFolder();

        long startTime = System.currentTimeMillis();
        List<Data> dataList = parser.parse();
        long endTime = System.currentTimeMillis();
        System.out.println("Parsing took " + (endTime - startTime) + " milliseconds");

        Solver solver = new Solver(dataList, "C:\\Users\\david\\Documents\\Szakdoga\\results");
        //solver.initGreedy(dataList.get(0));
        startTime = System.currentTimeMillis();
        for(Data data : dataList) {
            solver.initGreedy(data);
            endTime = System.currentTimeMillis();
            System.out.println("Greedy took " + (endTime - startTime) + " milliseconds");
            solver.ALNS(data);
            endTime = System.currentTimeMillis();
            System.out.println("ALNS took " + (endTime - startTime) + " milliseconds");
        }
        endTime = System.currentTimeMillis();
        System.out.println("Parsing took " + (endTime - startTime) + " milliseconds");



        // System.out.println(dataList.get(0).getNodeOnIndex(16).getCy());
        // parser.printMatrices();F
    }
}