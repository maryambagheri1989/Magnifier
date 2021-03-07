package ptolemy.domains.atc.kernel;


public class info {

    public info(int trackId) {
        super();
        this.beenOcc = false;
        this.beenReq = false;
        this.trackId=trackId;
    }

    /**
     * If this track has been occupied. true if yes.
     */
    public boolean beenOcc;
    
    /**
     * If a moving object wanted to enter into this track; true if yes.
     */
    public boolean beenReq;
    
    /**
     * This actor which is a boundary actor depends to another actor with the information
     * such as beenOcc and beenReq 
     */
    public int trackId;
    
    
}
