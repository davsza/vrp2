import data.Data;
import data.Node;
import data.Vehicle;

import java.util.*;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        Parser parser = new Parser();
        kim(parser);
    }

    public static void kim(Parser parser) {
        parser.addPath("C:\\Users\\david\\PycharmProjects\\szakdoga\\test");
        parser.setFolder();

        long startTime = System.currentTimeMillis();
        List<Data> dataList = parser.parseKimInstances();
        long endTime = System.currentTimeMillis();
        System.out.println("Parsing took " + (endTime - startTime) + " milliseconds");

        Solver solver = new Solver(dataList, "C:\\Users\\david\\Documents\\Szakdoga\\results");
        //solver.initGreedy(dataList.get(0));
        startTime = System.currentTimeMillis();
        for(Data data : dataList) {
            System.out.println("-------------------");
            System.out.println("init " + data.getInfo());
            float greedy = solver.initGreedyKim(data);
            endTime = System.currentTimeMillis();

            List<Vehicle> vehicles = data.getFleet().stream().filter(vehicle -> vehicle.getRoute().size() > 0).collect(Collectors.toList());

            for(Vehicle vehicle : data.getFleet().stream().filter(vehicle -> vehicle.getRoute().size() > 0).collect(Collectors.toList())) {
                System.out.print("Vehicle " + vehicle.getId() + "'s route: ");
                for(Node node : vehicle.getRoute()) {
                    String str;
                    if(node.isDepot()) {
                        str = "DP0";
                    } else if (node.isDumpingSite()) {
                        str = "DS" + node.getId();
                    } else {
                        str = node.getId().toString();
                    }
                    System.out.print(str + " - ");
                }
                System.out.println();
            }

            System.out.println("Greedy took " + (endTime - startTime) + " milliseconds");

            startTime = System.currentTimeMillis();
            Data alns = solver.ALNSkim(data);
            endTime = System.currentTimeMillis();

            for(Vehicle vehicle : alns.getFleet().stream().filter(vehicle -> vehicle.getRoute().size() > 0).collect(Collectors.toList())) {
                System.out.print("Vehicle " + vehicle.getId() + "'s route: ");
                for(Node node : vehicle.getRoute()) {
                    String str;
                    if(node.isDepot()) {
                        str = "DP0";
                    } else if (node.isDumpingSite()) {
                        str = "DS" + node.getId();
                    } else {
                        str = node.getId().toString();
                    }
                    System.out.print(str + " - ");
                }
                System.out.println();
            }

            System.out.println("ALNS took " + (endTime - startTime) + " milliseconds");

        }
        endTime = System.currentTimeMillis();
    }

}