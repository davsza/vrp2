import data.Data;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        Parser parser = new Parser();
        parseData(parser);
    }

    public static void parseData(Parser parser) {
        parser.addPath("C:\\Users\\david\\PycharmProjects\\szakdoga\\test");
        parser.setFolder();

        long startTime = System.currentTimeMillis();
        List<Data> dataList = parser.parseKimInstances();
        long endTime = System.currentTimeMillis();
        System.out.println("Parsing took " + (endTime - startTime) + " milliseconds");

        Solver solver = new Solver(dataList);
        Logger logger;
        for(Data data : dataList) {
            logger = new Logger();
            logger.setPath("C:\\Users\\david\\Documents\\Szakdoga\\results\\" + data.getInfo() + ".txt");
            float greedy = solver.initGreedy(data, logger);

            Data alns = solver.ALNS(data, logger);

            logger.writeFile();

        }
    }

}