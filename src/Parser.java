import data.Data;
import data.FileSection;
import data.Node;
import data.Vehicle;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class Parser {

    String path; // Folder path
    List<Data> data;
    File folder;
    FileSection section;

    public Parser()
    {
        this.path = "";
        this.data = new ArrayList<>();
        this.section = null;
    }

    void addPath(String path)
    {
        this.path = path;
    }

    void setData(List<Data> data)
    {
        this.data = data;
    }

    void setFolder()
    {
        this.folder = new File(this.path);
    }

    String getPath()
    {
        return path;
    }

    List<Data> getData()
    {
        return data;
    }

    public File getFolder()
    {
        return folder;
    }

    public FileSection getSection() {
        return section;
    }

    public void setSection(FileSection section) {
        this.section = section;
    }

    public List<Data> parse()
    {
        for(File fileEntry : Objects.requireNonNull(folder.listFiles()))
        {
            System.out.println("Parsing " + fileEntry.getName());
            Data data = new Data();
            int size = 0;
            int rowCount = 0;
            Float[][] matrix = null;
            Set<Integer> dumpingSizes = Collections.EMPTY_SET;
            try
            {
                Scanner scanner = new Scanner(fileEntry);
                int idx = 0;
                while (scanner.hasNextLine())
                {
                    String line = scanner.nextLine();
                    if(idx == 0) {
                        String[] datasetAndName = line.split(":");
                        data.setDataset(datasetAndName[0].strip());
                        data.setInfo(datasetAndName[1].strip());
                        String dataSetSize = datasetAndName[1].strip().split("_")[1];
                        size = getDataSetSize(dataSetSize) + 1;
                        //System.out.println(size);
                        // setting dumping sites
                        // dumpingSizes = getDumpingSiteNodeIds(size, 3);
                        // dummy
                        dumpingSizes = new HashSet<>();
                        dumpingSizes.add(1);
                        //dumpingSizes.add(2);
                        //dumpingSizes.add(4);
                        //dumpingSizes.add(6);
                        //System.out.println(dumpingSizes);
                        matrix = new Float[size][size];
                        idx++; continue;
                    }
                    else if(line.contains("Nodes")) {
                        setSection(FileSection.NODES); continue;
                    }
                    else if(line.contains("Vehicles")) {
                        setSection(FileSection.VEHICLES); continue;
                    }
                    else if(line.contains("matrix")) {
                        setSection(FileSection.MATRIX); continue;
                    }

                    if (getSection().equals(FileSection.NODES))
                    {
                        String[] nodeAttributes = line.split(" ");
                        Node node = new Node();
                        node.setId(data.getNodeListSize());
                        node.setCx(Float.parseFloat(nodeAttributes[0]));
                        node.setCy(Float.parseFloat(nodeAttributes[1]));
                        node.setQuantity(Float.parseFloat(nodeAttributes[2]));
                        node.setTimeStart(Integer.parseInt(nodeAttributes[3]));
                        node.setTimeEnd(Integer.parseInt(nodeAttributes[4]));
                        node.setServiceTime(Float.parseFloat(nodeAttributes[5]));

                        // setting depot node
                        if(data.getNodeListSize() == 0) {
                            node.setDepot(true);
                        }

                        if(dumpingSizes.contains(data.getNodeListSize())) {
                            node.setDumpingSite(true);
                        }
                        data.addNode(node);
                    }
                    else if (getSection().equals(FileSection.VEHICLES))
                    {
                        String[] vehicleAttributes = line.split(" ");
                        Vehicle vehicle = new Vehicle();
                        vehicle.setType(Integer.parseInt(vehicleAttributes[0]));
                        vehicle.setDepartureNode(data.getNodeOnIndex(Integer.parseInt(vehicleAttributes[1])));
                        vehicle.setArrivalNode(data.getNodeOnIndex(Integer.parseInt(vehicleAttributes[2])));
                        vehicle.setMaximumCapacity((int)Float.parseFloat(vehicleAttributes[3]));
                        vehicle.setMaximumTravelTime(Float.parseFloat(vehicleAttributes[4]));
                        vehicle.setId(data.getFleet().size());
                        data.addVehicle(vehicle);
                    }
                    else if (getSection().equals(FileSection.MATRIX))
                    {
                        String[] matrixRow = line.split(" ");
                        for (int i = 0; i < matrixRow.length; i++)  matrix[rowCount][i] = Float.parseFloat(matrixRow[i]);
                        rowCount++;
                    }
                    idx++;
                }
                scanner.close();
            }
            catch (FileNotFoundException e)
            {
                throw new RuntimeException(e);
            }
            data.setMatrix(matrix);
            this.data.add(data);
        }
        return this.data;
    }

    private Set<Integer> getDumpingSiteNodeIds(int size, int quantity) {
        Set<Integer> dumpingSiteNodeIds = new HashSet<>();
        Random rnd = new Random();
        while (dumpingSiteNodeIds.size() < quantity) {
            // generate random numbers between 1 and size - 1 (because of index bound error)
            int id = rnd.nextInt(size - 2) + 2;
            dumpingSiteNodeIds.add(id);
        }
        return dumpingSiteNodeIds;
    }

    private int getDataSetSize(String dataSetSize) {
        if (dataSetSize.startsWith("0")) dataSetSize.substring(1);
        return Integer.parseInt(dataSetSize);
    }

    public void printMatrices() {
        for(Data data : this.data) {
            data.matrixPrint();
        }
    }
}
