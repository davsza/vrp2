import data.Data;
import data.Node;
import data.Vehicle;

import java.util.List;

public class Solver {

    private  List<Data> dataList;
    private   String logPath;

    public Solver(List<Data> dataList, String logPath) {
        this.dataList = dataList;
        this.logPath = logPath;
    }

    public void greedy(Data data) {

        // init vehicles
        for(Vehicle vehicle : data.getFleet()) vehicle.initVehicle(data.getNodeListSize());

        Node currentNode = data.getDepotNode();
        Node nextNode = null;
        Vehicle currentVehicle = data.getFleet().get(0);

        while(data.unvisitedNode()){
            nextNode = data.findNextNode(currentVehicle, currentNode);
        }

    }
}
