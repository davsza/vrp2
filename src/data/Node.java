package data;


public class Node {

    private Integer id;
    private Float cx;
    private Float cy;
    private Float quantity;
    private Integer timeStart;
    private Integer timeEnd;
    private Float serviceTime;

    private Boolean visited;
    private  Boolean ghostNode;
    private Boolean depot;
    private Boolean dumpingSite;

    public Node() {
        this.visited = false;
        this.ghostNode = false;
        this.depot = false;
        this.dumpingSite = false;
    }

    public Boolean getDumpingSite() {
        return dumpingSite;
    }

    public void setDumpingSite(Boolean dumpingSite) {
        this.dumpingSite = dumpingSite;
    }

    public Boolean getDepot() {
        return depot;
    }

    public void setDepot(Boolean depot) {
        this.depot = depot;
    }


    public Boolean getGhostNode() {
        return ghostNode;
    }

    public void setGhostNode(Boolean ghostNode) {
        this.ghostNode = ghostNode;
    }

    public Boolean getVisited() {
        return visited;
    }

    public void setVisited(Boolean visited) {
        this.visited = visited;
    }

    public void setCx(Float cx) {
        this.cx = cx;
    }

    public void setCy(Float cy) {
        this.cy = cy;
    }

    public void setQuantity(Float quantity) {
        this.quantity = quantity;
    }

    public void setTimeStart(Integer timeStart) {
        this.timeStart = timeStart;
    }

    public void setTimeEnd(Integer timeEnd) {
        this.timeEnd = timeEnd;
    }

    public void setServiceTime(Float serviceTime) {
        this.serviceTime = serviceTime;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public Float getCx() {
        return cx;
    }

    public Float getCy() {
        return cy;
    }

    public Float getQuantity() {
        return quantity;
    }

    public Integer getTimeStart() {
        return timeStart;
    }

    public Integer getTimeEnd() {
        return timeEnd;
    }

    public Float getServiceTime() {
        return serviceTime;
    }
}
