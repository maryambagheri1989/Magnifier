// Each ATC region
package ptolemy.domains.atc.kernel;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import ptolemy.data.ArrayToken;
import ptolemy.data.IntToken;
import ptolemy.domains.atc.lib.Track;
import ptolemy.kernel.util.IllegalActionException;

public class Region {

    
    public Region(int id) {
        super();
        underAnalysis=false;
        this.id=id;
        tracks=new TreeMap<>();
        travelingAicrafts= new TreeMap<Integer, String>();
        entryTracks=new TreeMap<>();
        departureTracks=new TreeMap<>();
        northNeighbors=new TreeMap<>();
        westNeighbors=new TreeMap<>();
        eastNeighbors=new TreeMap<>();
        southNeighbors=new TreeMap<>();
        aircraftHasArrived= new ArrayList();
        neighbors=new TreeMap();
        numInputfrom=new TreeMap<>();
//        arrivalTimeAtRegion=new TreeMap<>();
//        departureTimeFromRegion=new TreeMap<>();

    }
    public Region(int id, Map<Integer, Track> tracks, Map<Integer, String> travelingAicrafts,
            Map<Integer, Integer> entryTracks, Map<Integer, Integer> departureTracks, Region northRegion,
            Region westRegion, Region southRegion, Region eastRegion) {
        super();
        underAnalysis=false;
        this.id=id;
        this.tracks = tracks;
        this.travelingAicrafts = travelingAicrafts;
        this.entryTracks = entryTracks;
        this.departureTracks = departureTracks;
    }
    public  boolean underAnalysis;
    public int id;
    
    public Map<Integer, Track> tracks;
  //  public ArrayList<Track> tracks;
    // The schedule of the aircraft traveling through this region.
    // public Map<Integer,ArrayToken> travelingAicrafts;
    public Map<Integer,String> travelingAicrafts;
    
    // If the aircraft arrives at this region at its pre-determined time, we save
    // it. Only the aircraft which arrives at its time is stored.
    // otherwise, the aircraft has not arrived yet or has not arrived at its time.
    public ArrayList<Integer> aircraftHasArrived;
    
    // The aircraft reach to this region trough which track. If it does not have an entry track
    // it is not stored.
    public Map<Integer, Integer> entryTracks;
    // The aircraft arrives at which track after departing from this region
    public Map<Integer, Integer> departureTracks;
    // Tracks placed in the north of this region
    
//    public Map<Integer, Double> arrivalTimeAtRegion;
//    
//    public Map<Integer, Double> departureTimeFromRegion;
//    
    public Map<Integer, Track> northNeighbors;
    public Map<Integer, Track> westNeighbors;
    public Map<Integer, Track> eastNeighbors;
    public Map<Integer, Track> southNeighbors;
    
    public Map<Integer,Region> neighbors; //0:North, 1: East, 2: South, 3: West
    
    /**The key is the id of the region which sends inputs to "this" region, and the
     * value is the number of aircraft which are send through that region to the current region.*/
    public Map<Integer, Integer> numInputfrom;
    
    public Track _containsTrack(Map<Integer, Track> array, int prevTrack) {
        // TODO Auto-generated method stub
        if(array.containsKey(prevTrack))
            return array.get(prevTrack);
        return null;
    }
    
    
}
