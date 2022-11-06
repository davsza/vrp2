package data;

public class Constants {

    private int PHI;
    private int CHI;
    private int PSI;
    private int OMEGA;
    private int P;
    private int P_WORST;
    private float W;
    private float C;
    private int SIGMA_1;
    private int SIGMA_2;
    private int SIGMA_3;
    private float R;
    private float ETA;
    private float ZETA;

    public Constants() {
        this.PHI = 9;
        this.CHI = 3;
        this.PSI = 2;
        this.OMEGA = 5;
        this.P = 6;
        this.P_WORST = 3;
        this.W = (float) 0.05;
        this.C = (float) 0.99975;
        this.SIGMA_1 = 33;
        this.SIGMA_2 = 9;
        this.SIGMA_3 = 13;
        this.R = (float) 0.1;
        this.ETA = (float) 0.025;
        this.ZETA = (float) 0.4;
    }

    public int getPHI() {
        return PHI;
    }

    public void setPHI(int PHI) {
        this.PHI = PHI;
    }

    public int getCHI() {
        return CHI;
    }

    public void setCHI(int CHI) {
        this.CHI = CHI;
    }

    public int getPSI() {
        return PSI;
    }

    public void setPSI(int PSI) {
        this.PSI = PSI;
    }

    public int getOMEGA() {
        return OMEGA;
    }

    public void setOMEGA(int OMEGA) {
        this.OMEGA = OMEGA;
    }

    public int getP() {
        return P;
    }

    public void setP(int p) {
        P = p;
    }

    public int getP_WORST() {
        return P_WORST;
    }

    public void setP_WORST(int p_WORST) {
        P_WORST = p_WORST;
    }

    public float getW() {
        return W;
    }

    public void setW(float w) {
        W = w;
    }

    public float getC() {
        return C;
    }

    public void setC(float c) {
        C = c;
    }

    public int getSIGMA_1() {
        return SIGMA_1;
    }

    public void setSIGMA_1(int SIGMA_1) {
        this.SIGMA_1 = SIGMA_1;
    }

    public int getSIGMA_2() {
        return SIGMA_2;
    }

    public void setSIGMA_2(int SIGMA_2) {
        this.SIGMA_2 = SIGMA_2;
    }

    public int getSIGMA_3() {
        return SIGMA_3;
    }

    public void setSIGMA_3(int SIGMA_3) {
        this.SIGMA_3 = SIGMA_3;
    }

    public float getR() {
        return R;
    }

    public void setR(float r) {
        R = r;
    }

    public float getETA() {
        return ETA;
    }

    public void setETA(float ETA) {
        this.ETA = ETA;
    }

    public float getZETA() {
        return ZETA;
    }

    public void setZETA(float ZETA) {
        this.ZETA = ZETA;
    }
}
