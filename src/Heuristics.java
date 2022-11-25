import data.*;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

public class Heuristics {

    private final Random random;
    private final Solver solver;
    private final Constants CONSTANTS;

    public Heuristics(Solver solver) {
        this.random = new Random();
        this.solver = solver;
        this.CONSTANTS = new Constants();
    }

    public void repairNodes(Data data, List<Node> nodesToSwap, HeuristicWeights heuristicWeights, Logger logger) {

        LocalTime startTime = LocalTime.now();
        long repairStart = System.nanoTime();
        logger.log("Repairing nodes started at: " + startTime);
        logger.log("Inserting " + nodesToSwap.size() + " nodes");

        float sumOf = heuristicWeights.sumOfRepair();
        float greedyWeight = heuristicWeights.getGreedyInsertWeight() / sumOf;
        float regret_2_Weight = heuristicWeights.getRegret_2_InsertWeight() / sumOf;
        float regret_3_Weight = heuristicWeights.getRegret_3_InsertWeight() / sumOf;
        float regret_K_Weight = heuristicWeights.getRegret_K_InsertWeight() / sumOf;
        double randomValue = random.nextDouble();
        if (randomValue < greedyWeight) {
            heuristicWeights.setCurrentInsert(1);
            logger.log("Insert method: greedyInsert");
            greedyInsert(data, nodesToSwap, logger);
        } else if (randomValue < greedyWeight + regret_2_Weight) {
            heuristicWeights.setCurrentInsert(2);
            logger.log("Insert method: regretInsert_2");
            regretInsert(data, nodesToSwap, 2, logger);
        } else if (randomValue < greedyWeight + regret_2_Weight + regret_3_Weight) {
            heuristicWeights.setCurrentInsert(3);
            logger.log("Insert method: regretInsert_3");
            regretInsert(data, nodesToSwap, 3, logger);
        } else if (randomValue < greedyWeight + regret_2_Weight + regret_3_Weight + regret_K_Weight) {
            heuristicWeights.setCurrentInsert(4);
            logger.log("Insert method: regretInsert_k");
            // TODO: stream
            int customerNodeCount = (int) data.getNodeList().stream().filter(node -> !node.isDepot() && !node.isDumpingSite()).count();
            regretInsert(data, nodesToSwap, customerNodeCount, logger);
        }

        LocalTime endTime = LocalTime.now();
        long repairEnd = System.nanoTime();
        logger.log("Repairing nodes ended at: " + endTime + ", took " + ((repairEnd - repairStart) * 1e-9) + " seconds");

    }

    private void regretInsert(Data data, List<Node> nodesToSwap, int p, Logger logger) {

        long startTime = System.nanoTime();
        logger.log("regretInsert_" + (p == 2 || p == 3 ? p : "k") + " started at: " + startTime);

        boolean valid;
        float initialValue = solver.getDataValue(data), bestDiff, currentValue, diff, distanceBetweenNodesToInsert, distanceBetweenNodesAfterInsert;
        int indexToInsert;
        long totalInsertValidityCheck = 0, startNano, endNano;
        List<NodeSwap> nodeSwapList = new ArrayList<>();
        Node nodeToInsert;
        NodeSwap currentNodeSwap;
        Vehicle penaltyVehicle = data.getPenaltyVehicle(), vehicleToInsertInto;

        for (Node nodesToInsert : nodesToSwap) {
            NodeSwap customerNodeSwap = new NodeSwap(nodesToInsert);
            boolean checkedEmptyVehicle = false;
            for (Vehicle vehicle : data.getFleet()) {
                bestDiff = Float.MAX_VALUE;

                if (vehicle.isPenaltyVehicle()) {
                    currentNodeSwap = new NodeSwap(nodesToInsert);
                    diff = 2 * data.getMaximumTravelDistance();
                    currentNodeSwap.setVehicle(vehicle);
                    currentNodeSwap.setIndex(vehicle.getRoute().size());
                    currentNodeSwap.setFoundVehicleForNodeToInsert(true);
                    currentNodeSwap.setNode(nodesToInsert);
                    currentNodeSwap.setValue(diff);
                    customerNodeSwap.getRegretNodeSwapList().add(currentNodeSwap);
                    continue;
                }

                if (checkedEmptyVehicle) continue;
                checkedEmptyVehicle = vehicle.getRoute().size() == 3;

                int vehicleRouteSize = vehicle.getRoute().size();
                currentNodeSwap = new NodeSwap(nodesToInsert);

                for (int i = 1; i < vehicleRouteSize - 1; i++) {

                    float previousNodeArrivalTime = vehicle.getArrivalTimes().get(i - 1);
                    Node previousNode = vehicle.getRoute().get(i - 1);
                    Node nextNode = vehicle.getRoute().get(i);
                    float serviceTimeAtPreviousNode = previousNode.getServiceTime();
                    float travelDistance = data.getDistanceBetweenNode(previousNode, nodesToInsert);
                    float arrivalTimeAtNode = previousNodeArrivalTime + serviceTimeAtPreviousNode + travelDistance;

                    if (arrivalTimeAtNode > nodesToInsert.getTimeEnd()) {
                        break;
                    }
                    if (nodesToInsert.getTimeStart() > nextNode.getTimeEnd()) {
                        continue;
                    }

                    distanceBetweenNodesToInsert = data.getDistanceBetweenNode(previousNode, nextNode);
                    vehicle.getRoute().add(i, nodesToInsert);
                    distanceBetweenNodesAfterInsert = data.getDistanceBetweenNode(previousNode, nodesToInsert) + data.getDistanceBetweenNode(nodesToInsert, nextNode);

                    startNano = System.nanoTime();
                    valid = solver.checkForValidity(data, vehicle);
                    endNano = System.nanoTime();
                    totalInsertValidityCheck += (endNano - startNano);

                    if (valid) {
                        currentValue = initialValue - distanceBetweenNodesToInsert + distanceBetweenNodesAfterInsert;
                        diff = currentValue - initialValue;
                        if (diff < bestDiff) {
                            bestDiff = diff;
                            currentNodeSwap.setVehicle(vehicle);
                            currentNodeSwap.setIndex(i);
                            currentNodeSwap.setFoundVehicleForNodeToInsert(true);
                            currentNodeSwap.setNode(nodesToInsert);
                            currentNodeSwap.setValue(diff);
                        }
                    }
                    vehicle.getRoute().remove(nodesToInsert);

                }
                if (currentNodeSwap.getVehicle() != null) {
                    customerNodeSwap.getRegretNodeSwapList().add(currentNodeSwap);
                }
            }
            customerNodeSwap.sortRegretList();
            nodeSwapList.add(customerNodeSwap);
        }

        while (nodesToSwap.size() > 0) {
            nodeSwapList.sort((o1, o2) -> (Float.compare(o1.getNumberOfFeasibleVehiclesToInsertInto(), o2.getNumberOfFeasibleVehiclesToInsertInto())));
            int leastFeasibleVehicleInsert = nodeSwapList.get(0).getNumberOfFeasibleVehiclesToInsertInto();

            NodeSwap bestNodeSwap = new NodeSwap();

            if (leastFeasibleVehicleInsert < p) {
                List<NodeSwap> feasibleNodeSwaps = nodeSwapList
                        .stream()
                        .filter(nodeSwap -> nodeSwap.getNumberOfFeasibleVehiclesToInsertInto() == leastFeasibleVehicleInsert)
                        .collect(Collectors.toList());
                float worst = 0;
                float bestDataValue = Float.MAX_VALUE;
                for (NodeSwap nodeSwap : feasibleNodeSwaps) {
                    float bestValue = nodeSwap.getRegretNodeSwapList().get(0).getValue();
                    diff = nodeSwap.getRegretSum(leastFeasibleVehicleInsert);
                    if (diff > worst) {
                        worst = diff;
                        bestNodeSwap = nodeSwap;
                        bestDataValue = bestValue;
                    } else if (diff == worst && nodeSwap.getRegretNodeSwapList().get(0).getValue() < bestDataValue) {
                        worst = diff;
                        bestNodeSwap = nodeSwap;
                        bestDataValue = bestValue;
                    }
                }
            } else {
                float worst = 0;
                float bestDataValue = Float.MAX_VALUE;
                for (NodeSwap nodeSwap : nodeSwapList) {
                    float bestValue = nodeSwap.getRegretNodeSwapList().get(0).getValue();
                    diff = nodeSwap.getRegretSum(p);
                    if (diff > worst) {
                        worst = diff;
                        bestNodeSwap = nodeSwap;
                        bestDataValue = bestValue;
                    } else if (diff == worst && nodeSwap.getRegretNodeSwapList().get(0).getValue() < bestDataValue) {
                        worst = diff;
                        bestNodeSwap = nodeSwap;
                        bestDataValue = bestValue;
                    }
                }
            }

            vehicleToInsertInto = bestNodeSwap.getRegretNodeSwapList().get(0).getVehicle();
            nodeToInsert = bestNodeSwap.getRegretNodeSwapList().get(0).getNode();
            indexToInsert = bestNodeSwap.getRegretNodeSwapList().get(0).getIndex();

            if (bestNodeSwap.getRegretNodeSwapList().get(0).isFoundVehicleForNodeToInsert()) {
                vehicleToInsertInto.getRoute().add(indexToInsert, nodeToInsert);
                solver.updateArrivalTimesForVehicle(vehicleToInsertInto, data);
            } else {
                penaltyVehicle.getRoute().add(indexToInsert, nodeToInsert);
            }
            nodesToSwap.remove(nodeToInsert);
            nodeSwapList.remove(bestNodeSwap);

            initialValue += bestNodeSwap.getRegretNodeSwapList().get(0).getValue();

            for (NodeSwap nodeSwap : nodeSwapList) {

                if(vehicleToInsertInto.isPenaltyVehicle()) {
                    break;
                }

                Vehicle finalVehicleToInsertInto = vehicleToInsertInto;
                List<NodeSwap> nodeSwapsWithSameVehicleList = nodeSwap.getRegretNodeSwapList()
                        .stream()
                        .filter(nodeSwap1 -> nodeSwap1.getVehicle().equals(finalVehicleToInsertInto))
                        .collect(Collectors.toList());

                if (nodeSwapsWithSameVehicleList.size() == 0) continue;

                NodeSwap selectedNodeSwap = nodeSwapsWithSameVehicleList.get(0);
                Node node = nodeSwap.getNode();

                boolean foundBetterValue = false;
                currentNodeSwap = null;

                if (selectedNodeSwap.getIndex() == indexToInsert) {
                    bestDiff = Float.MAX_VALUE;
                    selectedNodeSwap.setFoundVehicleForNodeToInsert(false);
                    for (int i = 1; i < vehicleToInsertInto.getRoute().size() - 1; i++) {

                        float previousNodeArrivalTime = vehicleToInsertInto.getArrivalTimes().get(i - 1);
                        Node previousNode = vehicleToInsertInto.getRoute().get(i - 1);
                        Node nextNode = vehicleToInsertInto.getRoute().get(i);
                        float serviceTimeAtPreviousNode = previousNode.getServiceTime();
                        float travelDistance = data.getDistanceBetweenNode(previousNode, node);
                        float arrivalTimeAtNode = previousNodeArrivalTime + serviceTimeAtPreviousNode + travelDistance;

                        if (arrivalTimeAtNode > node.getTimeEnd()) {
                            break;
                        }
                        if (node.getTimeStart() > nextNode.getTimeEnd()) {
                            continue;
                        }

                        distanceBetweenNodesToInsert = data.getDistanceBetweenNode(previousNode, nextNode);
                        vehicleToInsertInto.getRoute().add(i, node);
                        distanceBetweenNodesAfterInsert = data.getDistanceBetweenNode(previousNode, node) + data.getDistanceBetweenNode(node, nextNode);

                        startNano = System.nanoTime();
                        valid = solver.checkForValidity(data, vehicleToInsertInto);
                        endNano = System.nanoTime();
                        totalInsertValidityCheck += (endNano - startNano);

                        if (valid) {
                            currentValue = initialValue - distanceBetweenNodesToInsert + distanceBetweenNodesAfterInsert;
                            diff = currentValue - initialValue;
                            if (diff < bestDiff) {
                                bestDiff = diff;
                                selectedNodeSwap.setIndex(i);
                                selectedNodeSwap.setFoundVehicleForNodeToInsert(true);
                                selectedNodeSwap.setValue(diff);
                            }
                        }
                        vehicleToInsertInto.getRoute().remove(node);
                    }

                    if (!selectedNodeSwap.isFoundVehicleForNodeToInsert()) {
                        nodeSwap.getRegretNodeSwapList().remove(selectedNodeSwap);
                    }

                } else {
                    Node previousNode = vehicleToInsertInto.getRoute().get(indexToInsert - 1);
                    Node nextNode = vehicleToInsertInto.getRoute().get(indexToInsert + 1);

                    distanceBetweenNodesToInsert = data.getDistanceBetweenNode(previousNode, nodeToInsert);
                    vehicleToInsertInto.getRoute().add(indexToInsert, node);
                    distanceBetweenNodesAfterInsert = data.getDistanceBetweenNode(previousNode, node) + data.getDistanceBetweenNode(node, nodeToInsert);

                    startNano = System.nanoTime();
                    valid = solver.checkForValidity(data, vehicleToInsertInto);
                    endNano = System.nanoTime();
                    totalInsertValidityCheck += (endNano - startNano);

                    if (valid) {
                        currentValue = initialValue - distanceBetweenNodesToInsert + distanceBetweenNodesAfterInsert;
                        diff = currentValue - initialValue;
                        if (diff < selectedNodeSwap.getValue()) {
                            currentNodeSwap = new NodeSwap(node, vehicleToInsertInto, diff, indexToInsert, true);
                            foundBetterValue = true;
                        }
                    }

                    vehicleToInsertInto.getRoute().remove(indexToInsert);

                    // Insert after the inserted node
                    distanceBetweenNodesToInsert = data.getDistanceBetweenNode(nodeToInsert, nextNode);
                    vehicleToInsertInto.getRoute().add(indexToInsert + 1, node);
                    distanceBetweenNodesAfterInsert = data.getDistanceBetweenNode(nodeToInsert, node) + data.getDistanceBetweenNode(node, nextNode);

                    startNano = System.nanoTime();
                    valid = solver.checkForValidity(data, vehicleToInsertInto);
                    endNano = System.nanoTime();
                    totalInsertValidityCheck += (endNano - startNano);

                    if (valid) {
                        currentValue = initialValue - distanceBetweenNodesToInsert + distanceBetweenNodesAfterInsert;
                        diff = currentValue - initialValue;
                        if ((diff < selectedNodeSwap.getValue() && currentNodeSwap == null) || (currentNodeSwap != null && diff < currentNodeSwap.getValue())) {
                            currentNodeSwap = new NodeSwap(node, vehicleToInsertInto, diff, indexToInsert, true);
                            foundBetterValue = true;
                        }
                    }

                    vehicleToInsertInto.getRoute().remove(indexToInsert + 1);

                    if (foundBetterValue) {
                        selectedNodeSwap.setVehicle(currentNodeSwap.getVehicle());
                        selectedNodeSwap.setValue(currentNodeSwap.getValue());
                        selectedNodeSwap.setIndex(currentNodeSwap.getIndex());
                    } else if (selectedNodeSwap.getIndex() > indexToInsert) {
                        selectedNodeSwap.setIndex(selectedNodeSwap.getIndex() + 1);
                    }
                }
                nodeSwap.sortRegretList();
            }
        }
        long endTime = System.nanoTime();
        logger.log("regretInsert ended at: " + endTime + ", took " + ((endTime - startTime) * 1e-9) + " seconds");
        logger.log("Validating the data took " + (totalInsertValidityCheck * 1e-9) + " seconds");
    }

    private void greedyInsert(Data data, List<Node> nodesToSwap, Logger logger) {

        long startTime = System.nanoTime();
        logger.log("greedyInsert started at: " + startTime);

        boolean validSolution;
        float initialValue = solver.getDataValue(data), bestDiff, currentValue, diff, distanceBetweenNodesAfterInsert, distanceBetweenNodesToInsert;
        int indexToInsert;
        long totalInsertValidityCheck = 0, endNano, startNano;
        List<NodeSwap> nodeSwapList = new ArrayList<>();
        Node nodeToInsert;
        NodeSwap currentNodeSwap = null, bestNodeSwap;
        Vehicle penaltyVehicle = data.getPenaltyVehicle(), vehicleToInsertInto;

        for (Node nodesToInsert : nodesToSwap) {
            bestDiff = Float.MAX_VALUE;
            boolean checkedEmptyVehicle = false;
            for (Vehicle vehicle : data.getFleet()) {
                //TODO: penalty vehicle check
                if (vehicle.isPenaltyVehicle()) {
                    diff = 2 * data.getMaximumTravelDistance();
                    if (diff < bestDiff) {
                        bestDiff = diff;
                        currentNodeSwap = new NodeSwap(nodesToInsert, vehicle, diff, vehicle.getRoute().size(), true);
                    }
                    continue;
                }
                if (checkedEmptyVehicle) continue;
                checkedEmptyVehicle = vehicle.getRoute().size() == 3;
                for (int i = 1; i < vehicle.getRoute().size() - 1; i++) {

                    float previousNodeArrivalTime = vehicle.getArrivalTimes().get(i - 1);
                    Node previousNode = vehicle.getRoute().get(i - 1);
                    Node nextNode = vehicle.getRoute().get(i);
                    float serviceTimeAtPreviousNode = previousNode.getServiceTime();
                    float travelDistance = data.getDistanceBetweenNode(previousNode, nodesToInsert);
                    float arrivalTimeAtNode = previousNodeArrivalTime + serviceTimeAtPreviousNode + travelDistance;

                    if (arrivalTimeAtNode > nodesToInsert.getTimeEnd()) {
                        break;
                    }
                    if (nodesToInsert.getTimeStart() > nextNode.getTimeEnd()) {
                        continue;
                    }

                    distanceBetweenNodesToInsert = data.getDistanceBetweenNode(previousNode, nextNode);
                    vehicle.getRoute().add(i, nodesToInsert);
                    distanceBetweenNodesAfterInsert = data.getDistanceBetweenNode(previousNode, nodesToInsert) + data.getDistanceBetweenNode(nodesToInsert, nextNode);

                    startNano = System.nanoTime();
                    validSolution = solver.checkForValidity(data, vehicle);
                    endNano = System.nanoTime();
                    totalInsertValidityCheck += (endNano - startNano);

                    if (validSolution) {
                        currentValue = initialValue - distanceBetweenNodesToInsert + distanceBetweenNodesAfterInsert;
                        diff = currentValue - initialValue;
                        if (diff < bestDiff) {
                            bestDiff = diff;
                            currentNodeSwap = new NodeSwap(nodesToInsert, vehicle, diff, i, true);
                        }
                    }
                    vehicle.getRoute().remove(i);
                }
            }
            nodeSwapList.add(currentNodeSwap);
        }

        while (nodesToSwap.size() > 0) {
            nodeSwapList.sort((o1, o2) -> (Float.compare(o1.getValue(), o2.getValue())));

            bestNodeSwap = nodeSwapList.get(0);
            vehicleToInsertInto = bestNodeSwap.getVehicle();
            nodeToInsert = bestNodeSwap.getNode();
            indexToInsert = bestNodeSwap.getIndex();

            if (bestNodeSwap.isFoundVehicleForNodeToInsert()) {
                vehicleToInsertInto.getRoute().add(indexToInsert, nodeToInsert);
                solver.updateArrivalTimesForVehicle(vehicleToInsertInto, data);
            } else {
                penaltyVehicle.getRoute().add(indexToInsert, nodeToInsert);
            }
            nodesToSwap.remove(nodeToInsert);
            nodeSwapList.remove(0);

            initialValue += bestNodeSwap.getValue();

            for (NodeSwap nodeSwap : nodeSwapList) {
                if(vehicleToInsertInto.isPenaltyVehicle()) {
                    break;
                }
                boolean foundBetterValue = false;
                if (nodeSwap.getIndex() == indexToInsert && nodeSwap.getVehicle().equals(vehicleToInsertInto)) {

                    boolean checkedEmptyVehicle = false;
                    Node node = nodeSwap.getNode();
                    bestDiff = Float.MAX_VALUE;

                    for (Vehicle vehicle : data.getFleet()) {
                        //TODO: penalty vehicle check
                        if (vehicle.isPenaltyVehicle()) {
                            diff = 2 * data.getMaximumTravelDistance();
                            if (diff < bestDiff) {
                                bestDiff = diff;
                                currentNodeSwap = new NodeSwap(node, vehicle, diff, vehicle.getRoute().size() - 1, true);
                            }
                            continue;
                        }

                        if (checkedEmptyVehicle) continue;
                        checkedEmptyVehicle = vehicle.getRoute().size() == 3;

                        for (int i = 1; i < vehicle.getRoute().size() - 1; i++) {

                            float previousNodeArrivalTime = vehicle.getArrivalTimes().get(i - 1);
                            Node previousNode = vehicle.getRoute().get(i - 1);
                            Node nextNode = vehicle.getRoute().get(i);
                            float serviceTimeAtPreviousNode = previousNode.getServiceTime();
                            float travelDistance = data.getDistanceBetweenNode(previousNode, node);
                            float arrivalTimeAtNode = previousNodeArrivalTime + serviceTimeAtPreviousNode + travelDistance;

                            if (arrivalTimeAtNode > node.getTimeEnd()) {
                                break;
                            }
                            if (node.getTimeStart() > nextNode.getTimeEnd()) {
                                continue;
                            }

                            distanceBetweenNodesToInsert = data.getDistanceBetweenNode(previousNode, nextNode);
                            vehicle.getRoute().add(i, node);
                            distanceBetweenNodesAfterInsert = data.getDistanceBetweenNode(previousNode, node) + data.getDistanceBetweenNode(node, nextNode);

                            startNano = System.nanoTime();
                            validSolution = solver.checkForValidity(data, vehicle);
                            endNano = System.nanoTime();
                            totalInsertValidityCheck += (endNano - startNano);

                            if (validSolution) {
                                currentValue = initialValue - distanceBetweenNodesToInsert + distanceBetweenNodesAfterInsert;
                                diff = currentValue - initialValue;
                                if (diff < bestDiff) {
                                    bestDiff = diff;
                                    currentNodeSwap = new NodeSwap(node, vehicle, diff, i, true);
                                }
                            }
                            vehicle.getRoute().remove(i);
                        }
                    }
                    assert currentNodeSwap != null;
                    nodeSwap.setVehicle(currentNodeSwap.getVehicle());
                    nodeSwap.setValue(currentNodeSwap.getValue());
                    nodeSwap.setIndex(currentNodeSwap.getIndex());

                } else {
                    Node previousNode = vehicleToInsertInto.getRoute().get(indexToInsert - 1);
                    Node nextNode = vehicleToInsertInto.getRoute().get(indexToInsert + 1);
                    Node node = nodeSwap.getNode();

                    // Insert before the inserted node
                    distanceBetweenNodesToInsert = data.getDistanceBetweenNode(previousNode, nodeToInsert);
                    vehicleToInsertInto.getRoute().add(indexToInsert, node);
                    distanceBetweenNodesAfterInsert = data.getDistanceBetweenNode(previousNode, node) + data.getDistanceBetweenNode(node, nodeToInsert);

                    startNano = System.nanoTime();
                    validSolution = solver.checkForValidity(data, vehicleToInsertInto);
                    endNano = System.nanoTime();
                    totalInsertValidityCheck += (endNano - startNano);

                    if (validSolution) {
                        currentValue = initialValue - distanceBetweenNodesToInsert + distanceBetweenNodesAfterInsert;
                        diff = currentValue - initialValue;
                        if (diff < nodeSwap.getValue()) {
                            currentNodeSwap = new NodeSwap(node, vehicleToInsertInto, diff, indexToInsert, true);
                            foundBetterValue = true;
                        }
                    }

                    vehicleToInsertInto.getRoute().remove(indexToInsert);

                    // Insert after the inserted node
                    distanceBetweenNodesToInsert = data.getDistanceBetweenNode(nodeToInsert, nextNode);
                    vehicleToInsertInto.getRoute().add(indexToInsert + 1, node);
                    distanceBetweenNodesAfterInsert = data.getDistanceBetweenNode(nodeToInsert, node) + data.getDistanceBetweenNode(node, nextNode);

                    startNano = System.nanoTime();
                    validSolution = solver.checkForValidity(data, vehicleToInsertInto);
                    endNano = System.nanoTime();
                    totalInsertValidityCheck += (endNano - startNano);

                    if (validSolution) {
                        currentValue = initialValue - distanceBetweenNodesToInsert + distanceBetweenNodesAfterInsert;
                        diff = currentValue - initialValue;
                        if ((diff < nodeSwap.getValue() && currentNodeSwap == null) || (currentNodeSwap != null && diff < currentNodeSwap.getValue())) {
                            currentNodeSwap = new NodeSwap(node, vehicleToInsertInto, diff, indexToInsert, true);
                            foundBetterValue = true;
                        }
                    }

                    vehicleToInsertInto.getRoute().remove(indexToInsert + 1);

                    if (foundBetterValue) {
                        nodeSwap.setVehicle(currentNodeSwap.getVehicle());
                        nodeSwap.setValue(currentNodeSwap.getValue());
                        nodeSwap.setIndex(currentNodeSwap.getIndex());
                        currentNodeSwap = null;
                    } else if (nodeSwap.getVehicle().equals(vehicleToInsertInto) && nodeSwap.getIndex() > indexToInsert) {
                        nodeSwap.setIndex(nodeSwap.getIndex() + 1);
                    }
                }

            }
        }

        long endTime = System.nanoTime();
        logger.log("greedyInsert ended at: " + endTime + ", took " + ((endTime - startTime) * 1e-9) + " seconds");
        logger.log("Validating the data took " + (totalInsertValidityCheck * 1e-9) + " seconds");
    }

    public void destroyNodes(Data data, int p, List<Node> nodesToSwap, HeuristicWeights heuristicWeights, Logger logger) {

        LocalTime startTime = LocalTime.now();
        long destroyStart = System.nanoTime();
        logger.log("Destroying nodes started at: " + startTime);
        logger.log("Removing " + p + " nodes");

        float sumOf = heuristicWeights.sumOfDestroy();
        float worstWeight = heuristicWeights.getWorstRemovalWeight() / sumOf;
        float randomWeight = heuristicWeights.getRandomRemovalWeight() / sumOf;
        float relatedWeight = heuristicWeights.getRelatedRemovalWeight() / sumOf;
        float deleteWeight = heuristicWeights.getDeleteDisposalWeight() / sumOf;
        float swapWeight = heuristicWeights.getSwapDisposalWeight() / sumOf;
        float insertWeight = heuristicWeights.getInsertDisposalWeight() / sumOf;
        double randomValue = random.nextDouble();

        if (randomValue < worstWeight) {
            heuristicWeights.setCurrentRemove(1);
            logger.log("Destroy method: worstRemoval");
            worstRemove(data, p, nodesToSwap, CONSTANTS.getP_WORST(), logger);
        } else if (randomValue < worstWeight + randomWeight) {
            heuristicWeights.setCurrentRemove(2);
            logger.log("Destroy method: randomRemoval");
            randomRemove(data, p, nodesToSwap, logger);
        } else if (randomValue < worstWeight + randomWeight + relatedWeight) {
            heuristicWeights.setCurrentRemove(3);
            logger.log("Destroy method: relatedRemoval");
            relatedRemove(data, p, nodesToSwap, CONSTANTS.getPHI(), CONSTANTS.getCHI(), CONSTANTS.getPSI(), CONSTANTS.getP(), logger);
        } else if (randomValue < worstWeight + randomWeight + relatedWeight + deleteWeight) {
            heuristicWeights.setCurrentRemove(4);
            logger.log("Destroy method: deleteDisposal");
            deleteDisposal(data, nodesToSwap, logger);
        } else if (randomValue < worstWeight + randomWeight + relatedWeight + deleteWeight + swapWeight) {
            heuristicWeights.setCurrentRemove(5);
            logger.log("Destroy method: swapDisposal");
            swapDisposal(data, nodesToSwap, logger);
        } else if (randomValue < worstWeight + randomWeight + relatedWeight + deleteWeight + swapWeight + insertWeight) {
            heuristicWeights.setCurrentRemove(6);
            logger.log("Destroy method: insertDisposal");
            insertDisposal(data, nodesToSwap, logger);
        }

        LocalTime endTime = LocalTime.now();
        long destroyEnd = System.nanoTime();
        logger.log("Destroying nodes ended at: " + endTime + ", took " + ((destroyEnd - destroyStart) * 1e-9) + " seconds");

    }

    private void relatedRemove(Data data, int p, List<Node> nodesToSwap,
                               float phi, float chi, float psi, int P, Logger logger) {

        long startTime = System.nanoTime();
        logger.log("relatedRemoval started at: " + startTime);

        randomRemove(data, 1, nodesToSwap, logger);

        int randomIndex, indexToRemoveFrom;
        List<NodeSwap> nodeSwapList;
        Node nodeToRemove;
        NodeSwap bestNodeSwap, currentNodeSwap;
        Vehicle vehicleToRemoveFrom;

        while (nodesToSwap.size() < p) {
            nodeSwapList = new ArrayList<>();
            randomIndex = nodesToSwap.size() == 0 ? 0 : random.nextInt(nodesToSwap.size());
            Node nodeToCompare = nodesToSwap.get(randomIndex);
            data.calculateVisitingTime();
            // TODO: stream
            List<Vehicle> feasibleVehicles = data.getFleet().stream().filter(vehicle -> vehicle.getRoute().size() > 3).collect(Collectors.toList());
            for (Vehicle vehicle : feasibleVehicles) {
                List<Node> feasibleNodes = vehicle.getRoute().stream().filter(Node::customerNode).collect(Collectors.toList());
                for (Node node : feasibleNodes) {
                    currentNodeSwap = new NodeSwap();
                    float relatedness = phi * data.getDistanceBetweenNode(nodeToCompare, node)
                            + chi * Math.abs(nodeToCompare.getVisitedAt() - node.getVisitedAt())
                            + psi * Math.abs(nodeToCompare.getQuantity() - node.getQuantity());
                    currentNodeSwap.setNode(node);
                    currentNodeSwap.setValue(relatedness);
                    currentNodeSwap.setVehicle(vehicle);

                    nodeSwapList.add(currentNodeSwap);
                }
            }

            nodeSwapList.sort((o1, o2) -> (Float.compare(o1.getValue(), o2.getValue())));

            double y = random.nextDouble();
            int index = (int) (Math.pow(y, P) * nodeSwapList.size());

            bestNodeSwap = nodeSwapList.get(index);
            vehicleToRemoveFrom = bestNodeSwap.getVehicle();
            nodeToRemove = bestNodeSwap.getNode();
            indexToRemoveFrom = bestNodeSwap.getIndex();
            vehicleToRemoveFrom.getRoute().remove(indexToRemoveFrom);
            nodesToSwap.add(nodeToRemove);
        }

        long endTime = System.nanoTime();
        logger.log("relatedRemoval ended at: " + endTime + ", took " + ((endTime - startTime) * 1e-9) + " seconds");

    }

    private void randomRemove(Data data, int p, List<Node> nodesToSwap, Logger logger) {

        long startTime = System.nanoTime();
        logger.log("randomRemoval started at: " + startTime);

        boolean found;
        int numberOfFeasibleNodesToRemove, index;
        List<Node> feasibleNodesToRemove;

        while (nodesToSwap.size() < p) {
            // TODO: stream
            feasibleNodesToRemove = data.getNodeList().stream().filter(Node::customerNode).collect(Collectors.toList());
            numberOfFeasibleNodesToRemove = feasibleNodesToRemove.size();
            index = random.nextInt(numberOfFeasibleNodesToRemove);
            Node nodeToRemove = feasibleNodesToRemove.get(index);
            for (Vehicle vehicle : data.getFleet()) {
                found = false;
                for (int i = 0; i < vehicle.getRoute().size(); i++) {
                    Node node = vehicle.getRoute().get(i);
                    if ((int) node.getId() == nodeToRemove.getId()) {
                        nodesToSwap.add(node);
                        vehicle.getRoute().remove(node);
                        found = true;
                        break;
                    }
                }
                if (found) break;
            }
        }

        long endTime = System.nanoTime();
        logger.log("randomRemoval ended at: " + endTime + ", took " + ((endTime - startTime) * 1e-9) + " seconds");

    }

    private void worstRemove(Data data, int p, List<Node> nodesToSwap, int p_worst, Logger logger) {

        long startTime = System.nanoTime();
        logger.log("worstRemoval started at: " + startTime);

        float currentValue, initialValue = solver.getDataValue(data);
        int indexToRemoveFrom;
        List<NodeSwap> nodeSwapList = new ArrayList<>();
        Node nodeToRemove;
        NodeSwap bestNodeSwap, currentNodeSwap;
        Vehicle vehicleToRemoveFrom;

        for (Vehicle vehicle : data.getFleet()) {
            if (vehicle.getRoute().size() < 4 || vehicle.isPenaltyVehicle()) {
                continue;
            }
            for (int i = 0; i < vehicle.getRoute().size(); i++) {
                Node node = vehicle.getRoute().get(i);
                currentNodeSwap = new NodeSwap();
                if (!node.isDepot() && !node.isDumpingSite()) {
                    if(vehicle.isPenaltyVehicle()) {
                        currentValue = initialValue - 2 * data.getMaximumTravelDistance();
                        currentNodeSwap.setNode(node);
                        currentNodeSwap.setValue(currentValue);
                        currentNodeSwap.setIndex(i);
                        currentNodeSwap.setVehicle(vehicle);
                        nodeSwapList.add(currentNodeSwap);
                    } else {
                        Node previousNode = vehicle.getRoute().get(i - 1);
                        Node nextNode = vehicle.getRoute().get(i + 1);
                        float distanceBetweenNodesBeforeRemoval = data.getDistanceBetweenNode(previousNode, node) + data.getDistanceBetweenNode(node, nextNode);
                        vehicle.getRoute().remove(node);
                        float distanceBetweenNodesAfterRemoval = data.getDistanceBetweenNode(previousNode, nextNode);
                        currentValue = initialValue - distanceBetweenNodesBeforeRemoval + distanceBetweenNodesAfterRemoval;
                        currentNodeSwap.setNode(node);
                        currentNodeSwap.setValue(currentValue);
                        currentNodeSwap.setIndex(i);
                        currentNodeSwap.setVehicle(vehicle);
                        nodeSwapList.add(currentNodeSwap);
                        vehicle.getRoute().add(i, node);
                    }
                }
            }
        }

        while (nodesToSwap.size() < p) {
            nodeSwapList.sort((o1, o2) -> (Float.compare(o2.getValue(), o1.getValue())));

            double y = random.nextDouble();
            int index = (int) (Math.pow(y, p_worst) * nodeSwapList.size());

            bestNodeSwap = nodeSwapList.get(index);
            vehicleToRemoveFrom = bestNodeSwap.getVehicle();
            nodeToRemove = bestNodeSwap.getNode();
            indexToRemoveFrom = bestNodeSwap.getIndex();
            vehicleToRemoveFrom.getRoute().remove(indexToRemoveFrom);
            nodeSwapList.remove(bestNodeSwap);
            nodesToSwap.add(nodeToRemove);

            // Removal before the removed node
            Node previousNode = vehicleToRemoveFrom.getRoute().get(indexToRemoveFrom - 1);
            if (!previousNode.isDepot() && !previousNode.isDumpingSite()) {
                float distanceBeforeRemoval_ = data.getDistanceBetweenNode(previousNode, nodeToRemove);
                float distanceAfterRemoval_ = data.getDistanceBetweenNode(previousNode, vehicleToRemoveFrom.getRoute().get(indexToRemoveFrom));
                currentValue = initialValue - distanceBeforeRemoval_ + distanceAfterRemoval_;
                currentNodeSwap = nodeSwapList.stream().filter(nodeSwap -> nodeSwap.getNode().getId() == (int) previousNode.getId()).findFirst().get();
                if (currentValue > currentNodeSwap.getValue()) {
                    currentNodeSwap.setValue(currentValue);
                    currentNodeSwap.setVehicle(vehicleToRemoveFrom);
                    currentNodeSwap.setIndex(indexToRemoveFrom - 1);
                }

            }

            for (NodeSwap nodeSwap : nodeSwapList) {
                if (nodeSwap.getVehicle().equals(vehicleToRemoveFrom) && nodeSwap.getIndex() > indexToRemoveFrom) {
                    nodeSwap.setIndex(nodeSwap.getIndex() - 1);
                }
            }

            // Removal after the removed node
            Node nextNode = vehicleToRemoveFrom.getRoute().get(indexToRemoveFrom);
            if (!nextNode.isDepot() && !nextNode.isDumpingSite()) {
                float distanceBeforeRemoval_ = data.getDistanceBetweenNode(nodeToRemove, nextNode);
                float distanceAfterRemoval_ = data.getDistanceBetweenNode(vehicleToRemoveFrom.getRoute().get(indexToRemoveFrom - 1), nextNode);
                currentValue = initialValue - distanceBeforeRemoval_ + distanceAfterRemoval_;

                currentNodeSwap = nodeSwapList.stream().filter(nodeSwap -> nodeSwap.getNode().getId() == (int) nextNode.getId()).findFirst().get();
                if (currentValue > currentNodeSwap.getValue()) {
                    currentNodeSwap.setValue(currentValue);
                    currentNodeSwap.setVehicle(vehicleToRemoveFrom);
                    currentNodeSwap.setIndex(indexToRemoveFrom);
                }
            }
        }

        long endTime = System.nanoTime();
        logger.log("worstRemoval ended at: " + endTime + ", took " + ((endTime - startTime) * 1e-9) + " seconds");

    }

    private void deleteDisposal(Data data, List<Node> nodesToSwap, Logger logger) {

        long startTime = System.nanoTime();
        logger.log("deleteDisposal started at: " + startTime);

        List<NodeSwap> nodeSwapList = new ArrayList<>();
        // TODO: stream
        List<Vehicle> feasibleVehicles = data.getFleet().stream().filter(vehicle -> vehicle.getRoute().stream().filter(Node::isDumpingSite).count() > 1).collect(Collectors.toList());

        if (feasibleVehicles.size() == 0)  return;

        for (Vehicle vehicle : feasibleVehicles) {
            for (int i = 0; i < vehicle.getRoute().size(); i++) {
                Node node = vehicle.getRoute().get(i);
                if (node.isDumpingSite()) {
                    NodeSwap nodeSwap = new NodeSwap(node, vehicle, 0, i, false);
                    nodeSwapList.add(nodeSwap);
                }
            }
        }

        int randomIndex = random.nextInt(nodeSwapList.size());
        NodeSwap nodeSwap = nodeSwapList.get(randomIndex);
        Vehicle vehicle = nodeSwap.getVehicle();
        Node dumpingSite = nodeSwap.getNode();
        int dumpingSiteIndex = nodeSwap.getIndex();

        // TODO: for loop
        nodeSwapList = nodeSwapList.stream().filter(nodeSwap1 -> nodeSwap1.getVehicle() == vehicle).collect(Collectors.toList());

        if (vehicle.getRoute().get(dumpingSiteIndex + 1).isDepot()) {
            Node currentNode = dumpingSite;
            vehicle.getRoute().remove(currentNode);
            currentNode = vehicle.getRoute().get(dumpingSiteIndex - 1);
            while (!currentNode.isDumpingSite()) {
                nodesToSwap.add(currentNode);
                vehicle.getRoute().remove(currentNode);
                dumpingSiteIndex--;
                currentNode = vehicle.getRoute().get(dumpingSiteIndex - 1);
            }
        } else {
            int maximumCapacity = vehicle.getMaximumCapacity();
            int startingIndex = nodeSwapList.indexOf(nodeSwap) == 0 ? 1 : nodeSwapList.get(nodeSwapList.indexOf(nodeSwap) - 1).getIndex();
            float overallQuantity = 0;
            for (int i = startingIndex; i < nodeSwapList.get(nodeSwapList.indexOf(nodeSwap) + 1).getIndex(); i++) {
                Node node = vehicle.getRoute().get(i);
                if (!node.isDumpingSite()) {
                    overallQuantity += node.getQuantity();
                }
            }
            vehicle.getRoute().remove(dumpingSite);
            int numberOfNodesRemoved = 0;
            while (overallQuantity > maximumCapacity) {
                Node currentNode = vehicle.getRoute().get(dumpingSiteIndex - 1);
                overallQuantity -= currentNode.getQuantity();
                nodesToSwap.add(currentNode);
                vehicle.getRoute().remove(currentNode);
                numberOfNodesRemoved++;
                if (numberOfNodesRemoved % 2 == 0) {
                    dumpingSiteIndex--;
                }
            }
        }

        long endTime = System.nanoTime();
        logger.log("worstRemoval ended at: " + endTime + ", took " + ((endTime - startTime) * 1e-9) + " seconds");

    }

    private void swapDisposal(Data data, List<Node> nodesToSwap, Logger logger) {

        long startTime = System.nanoTime();
        logger.log("swapDisposal started at: " + startTime);

        List<NodeSwap> nodeSwapList = new ArrayList<>();
        // TODO: stream
        // TODO: not completely sure why don't I swap with only 1 disposal, but so be it
        int numberOfDisposalSites = (int) data.getNodeList().stream().filter(Node::isDumpingSite).count();
        if (numberOfDisposalSites == 1) {
            return;
        }
        // TODO: stream
        List<Vehicle> feasibleVehicles = data.getFleet().stream().filter(vehicle -> vehicle.getRoute().size() > 3).collect(Collectors.toList());
        for (Vehicle vehicle : feasibleVehicles) {
            for (int i = 0; i < vehicle.getRoute().size(); i++) {
                Node node = vehicle.getRoute().get(i);
                if (node.isDumpingSite()) {
                    NodeSwap nodeSwap = new NodeSwap(node, vehicle, 0, i, false);
                    nodeSwapList.add(nodeSwap);
                }
            }
        }

        int randomIndex = random.nextInt(nodeSwapList.size());
        NodeSwap nodeSwap = nodeSwapList.get(randomIndex);
        Vehicle vehicle = nodeSwap.getVehicle();
        Node dumpingSite = nodeSwap.getNode();
        int dumpingSiteIndex = nodeSwap.getIndex();

        // TODO: stream
        List<Node> disposalSitesToSwapWith = data.getNodeList().stream().filter(disposalSite -> disposalSite.isDumpingSite() && (int)disposalSite.getId() != dumpingSite.getId()).collect(Collectors.toList());
        randomIndex = random.nextInt(disposalSitesToSwapWith.size());
        Node disposalSiteToSwapWith = disposalSitesToSwapWith.get(randomIndex);

        vehicle.getRoute().set(dumpingSiteIndex, disposalSiteToSwapWith);
        float disposalTimeEnd = disposalSiteToSwapWith.getTimeEnd();
        float arrivalTimeAtPreviousNode, arrivalTimeAtNextNode, travelDistance, serviceTime, arrivalTimeAtDisposalSite;
        Node previousNode, nextNode;

        while (true) {
            previousNode = vehicle.getRoute().get(dumpingSiteIndex - 1);
            arrivalTimeAtPreviousNode = vehicle.getArrivalTimes().get(dumpingSiteIndex - 1);
            serviceTime = previousNode.getServiceTime();
            travelDistance = data.getDistanceBetweenNode(previousNode, disposalSiteToSwapWith);
            arrivalTimeAtDisposalSite = arrivalTimeAtPreviousNode + serviceTime + travelDistance;
            if (arrivalTimeAtDisposalSite <= disposalTimeEnd) {
                vehicle.getArrivalTimes().set(dumpingSiteIndex, Math.max(arrivalTimeAtDisposalSite, disposalSiteToSwapWith.getTimeStart()));
                solver.updateArrivalTimes(data);
                break;
            }
            nodesToSwap.add(previousNode);
            vehicle.getArrivalTimes().remove(dumpingSiteIndex - 1);
            vehicle.getRoute().remove(previousNode);
            dumpingSiteIndex--;
        }
        for (int i = dumpingSiteIndex + 1; i < vehicle.getRoute().size() - 1; i++) {
            nextNode = vehicle.getRoute().get(i);
            serviceTime = disposalSiteToSwapWith.getServiceTime();
            travelDistance = data.getDistanceBetweenNode(disposalSiteToSwapWith, nextNode);
            arrivalTimeAtNextNode = arrivalTimeAtDisposalSite + serviceTime + travelDistance;
            if (arrivalTimeAtNextNode <= nextNode.getTimeEnd()) {
                vehicle.getArrivalTimes().set(i, Math.max(arrivalTimeAtNextNode, nextNode.getTimeStart()));
                solver.updateArrivalTimes(data);
                continue;
            }
            nodesToSwap.add(nextNode);
            vehicle.getArrivalTimes().remove(i);
            vehicle.getRoute().remove(nextNode);
            i--;
        }

        long endTime = System.nanoTime();
        logger.log("swapDisposal ended at: " + endTime + ", took " + ((endTime - startTime) * 1e-9) + " seconds");

    }

    private void insertDisposal(Data data, List<Node> nodesToSwap, Logger logger) {

        long startTime = System.nanoTime();
        logger.log("insertDisposal started at: " + startTime);

        // TODO: stream
        List<Vehicle> feasibleVehicles = data.getFleet().stream().filter(vehicle -> vehicle.getRoute().size() > 3).collect(Collectors.toList());
        int randomIndex = random.nextInt(feasibleVehicles.size());
        Vehicle vehicleToInsertInto = feasibleVehicles.get(randomIndex);

        // TODO: stream
        List<Node> disposalSites = vehicleToInsertInto.getRoute().stream().filter(Node::isDumpingSite).collect(Collectors.toList());
        Node dumpingSiteToInsertAfter = disposalSites.get(disposalSites.size() - 1);

        // TODO: stream
        List<Node> disposalSitesToSwapWith = data.getNodeList().stream().filter(disposalSite -> disposalSite.isDumpingSite() && (int)disposalSite.getId() != dumpingSiteToInsertAfter.getId()).collect(Collectors.toList());
        randomIndex = random.nextInt(disposalSitesToSwapWith.size());
        Node disposalSiteToInsert = disposalSitesToSwapWith.get(randomIndex);

        int index = vehicleToInsertInto.getRoute().size();
        vehicleToInsertInto.getRoute().add(vehicleToInsertInto.getRoute().size() - 1, disposalSiteToInsert);

        Node currentNode;
        vehicleToInsertInto.getArrivalTimes().add(index, (float) 0);
        float arriveTimeAtPreviousNode, serviceTimeAtPreviousNode, travelDistance;
        while (true) {
            currentNode = vehicleToInsertInto.getRoute().get(index - 1);
            arriveTimeAtPreviousNode = vehicleToInsertInto.getArrivalTimes().get(index - 1);
            serviceTimeAtPreviousNode = currentNode.getServiceTime();
            travelDistance = data.getDistanceBetweenNode(currentNode, disposalSiteToInsert);
            if (arriveTimeAtPreviousNode + serviceTimeAtPreviousNode + travelDistance <= disposalSiteToInsert.getTimeEnd()) {
                vehicleToInsertInto.getArrivalTimes().set(index, arriveTimeAtPreviousNode + serviceTimeAtPreviousNode + travelDistance);
                break;
            }
            vehicleToInsertInto.getArrivalTimes().remove(index - 1);
            nodesToSwap.add(currentNode);
            vehicleToInsertInto.getRoute().remove(currentNode);
            index--;
        }

        long endTime = System.nanoTime();
        logger.log("insertDisposal ended at: " + endTime + ", took " + ((endTime - startTime) * 1e-9) + " seconds");

    }

}
