package data;

public class HeuristicWeights {

    private float randomRemove;
    private float worstRemove;
    private float relatedRemove;
    private float greedyInsert;
    private float regretInsert;
    private int currentRemove; // 1: randomRemove; 2: worstRemove; 3: relatedRemove
    private int currentInsert; // 1: greedyInsert; 2: regretInsert

    public HeuristicWeights() {
        this.randomRemove = 1;
        this.worstRemove = 1;
        this.relatedRemove = 1;
        this.greedyInsert = 1;
        this.regretInsert = 1;
        this.currentRemove = -1;
        this.currentInsert = -1;
    }

    public void reset() {
        setRandomRemove(0);
        setWorstRemove(0);
        setRelatedRemove(0);
        setGreedyInsert(0);
        setRegretInsert(0);
    }

    public int getCurrentRemove() {
        return currentRemove;
    }

    public void setCurrentRemove(int currentRemove) {
        this.currentRemove = currentRemove;
    }

    public int getCurrentInsert() {
        return currentInsert;
    }

    public void setCurrentInsert(int currentInsert) {
        this.currentInsert = currentInsert;
    }

    public float sumOfRepair() {
        return getGreedyInsert() + getRegretInsert();
    }

    public float sumOfDestroy() {
        return getRandomRemove() + getWorstRemove() + getRelatedRemove();
    }

    public float getRandomRemove() {
        return randomRemove;
    }

    public void setRandomRemove(float randomRemove) {
        this.randomRemove = randomRemove;
    }

    public float getWorstRemove() {
        return worstRemove;
    }

    public void setWorstRemove(float worstRemove) {
        this.worstRemove = worstRemove;
    }

    public float getRelatedRemove() {
        return relatedRemove;
    }

    public void setRelatedRemove(float relatedRemove) {
        this.relatedRemove = relatedRemove;
    }

    public float getGreedyInsert() {
        return greedyInsert;
    }

    public void setGreedyInsert(float greedyInsert) {
        this.greedyInsert = greedyInsert;
    }

    public float getRegretInsert() {
        return regretInsert;
    }

    public void setRegretInsert(float regretInsert) {
        this.regretInsert = regretInsert;
    }
}
