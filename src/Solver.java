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
        Node nextNode;
        Vehicle currentVehicle = data.getFleet().get(0);
        currentVehicle.switchNode(currentVehicle.getRoute().get(0), currentNode);

        while(data.unvisitedNode()){
            nextNode = data.findNextNode(currentVehicle, currentNode);
            if (!nextNode.getNullNode()) {
                float distance = data.getDistanceBetweenNode(currentNode, nextNode);
                currentVehicle.setCurrentTime(currentVehicle.getCurrentTime() + distance);
            }


        }

    }
}
