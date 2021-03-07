package ptolemy.domains.atc.kernel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import ptolemy.actor.util.Time;
import ptolemy.data.IntToken;
import ptolemy.data.RecordToken;
import ptolemy.data.Token;
import ptolemy.domains.atc.lib.ObjectsList.MovingObjectCell;
import ptolemy.domains.de.kernel.DEEvent;
import ptolemy.domains.de.kernel.DEEventQueue;

public class Snapshot {

    public Snapshot(int parent, DEEventQueue eventQueue, String name, Time _modelTime, int _microstep, Map<Integer, String> underAnalysisMO) {
        super();
        this.parent=new ArrayList<Integer>();
        this.parent.add(parent);
        this.eventQueue=new ArrayList<>();
        if(eventQueue!=null) {
            Object[] objectArray=eventQueue.toArray();
            for(int i=0;i<objectArray.length;i++)
                this.eventQueue.add((DEEvent)objectArray[i]);
        }
//        this.eventQueue = eventQueue;
        this.name = name;
        this._modelTime = _modelTime;
//        removedEvents=new ArrayList<DEEvent>();
//        index=0;
        this.upToThisTaken=-1;
        this._microstep=_microstep;
        this.airportActors=new TreeMap<Integer, AirportFeilds>();
        this.destinationAirportActors=new TreeMap<Integer, DestinationAirportFields>();
        this.trackActors=new TreeMap<Integer, TrackFields>();
        this.inputTokens=new TreeMap<String, Map<Integer, Token>>();
        this.regionsUnderAnalysis=new TreeMap<>();
        this.isAircraftArrived=new ArrayList<>();
        this.underAnalysisMovingObjects=new HashMap<>();
        if(underAnalysisMO!=null)
            for(Entry<Integer, String> entry:underAnalysisMO.entrySet()) {
                this.underAnalysisMovingObjects.put(entry.getKey(), entry.getValue());
            }
        
    }
    
    public Snapshot(int parent, DEEventQueue eventQueue, String name, Time _modelTime, int _microstep, int upToThisTaken, Map<Integer, String> underAnalysisMO) {
        super();
        this.parent=new ArrayList<Integer>();
        this.parent.add(parent);
        this.eventQueue=new ArrayList<>();
        if(eventQueue!=null) {
            Object[] objectArray=eventQueue.toArray();
            for(int i=0;i<objectArray.length;i++)
                this.eventQueue.add((DEEvent)objectArray[i]);
        }
//        this.eventQueue = eventQueue;
        this.name = name;
        this._modelTime = _modelTime;
//        removedEvents=new ArrayList<DEEvent>();
//        index=0;
        this.upToThisTaken=upToThisTaken;
        this._microstep=_microstep;
        this.airportActors=new TreeMap<Integer, AirportFeilds>();
        this.destinationAirportActors=new TreeMap<Integer, DestinationAirportFields>();
        this.trackActors=new TreeMap<Integer, TrackFields>();
        this.inputTokens=new TreeMap<String, Map<Integer, Token>>();
        this.regionsUnderAnalysis=new TreeMap<>();
        this.isAircraftArrived=new ArrayList<>();
        
        this.underAnalysisMovingObjects=new HashMap<>();
        if(underAnalysisMO!=null)
            for(Entry<Integer, String> entry:underAnalysisMO.entrySet()) {
                this.underAnalysisMovingObjects.put(entry.getKey(),entry.getValue());
            }
            
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((_modelTime == null) ? 0 : _modelTime.hashCode());
        result = prime * result + ((airportActors == null) ? 0 : hashCodeOfAirportActors());
        result = prime * result + ((destinationAirportActors == null) ? 0 : hashCodeOfDesAirportActors());
        result = prime * result + ((eventQueue == null) ? 0 : hashCodeOfEventQueue());
        result = prime * result + ((trackActors == null) ? 0 : hashCodeOfTrackActors());
//        System.out.println(result);
        return result;
    }
    
    // Since Token.hashCode has not been defined and also the number of entries in the eventQueue and the
    // other fields are large, and also the key in the map may have some affect, we do not use feild.hashCode()
    public int hashCodeOfAirportActors() {
        int result=1;
        for(Entry<Integer, AirportFeilds> entry: airportActors.entrySet()) {
            if(entry.getValue()._inTransit!=null) {
                result+=((IntToken)((RecordToken)entry.getValue()._inTransit).get("aircraftId")).intValue();
            }
            if(entry.getValue()._transitExpires!=null)
                result+=entry.getValue()._transitExpires.hashCode();
        }
        return result; 
    }
    
    public int hashCodeOfDesAirportActors() {
        int result=1;
        for(Entry<Integer, DestinationAirportFields> entry: destinationAirportActors.entrySet()) {
            if(entry.getValue()._inTransit!=null) {
                result+=((IntToken)((RecordToken)entry.getValue()._inTransit).get("aircraftId")).intValue();
            }
            if(entry.getValue()._transitExpires!=null)
                result+=entry.getValue()._transitExpires.hashCode();
        }
        return result;
    }
    public int hashCodeOfEventQueue() {
        return eventQueue.size();
    }
    
    public int hashCodeOfTrackActors() {
        int result=1;
        for(Entry<Integer, TrackFields> entry: trackActors.entrySet()) {
            TrackFields temp=entry.getValue();
            result +=temp.OutRoute;
            result += (temp.called ? 1231 : 1237);
            result += (temp.genMode ? 1231 : 1237);
            result += ((temp.inTransit == null) ? 0 : ((IntToken)((RecordToken)temp.inTransit).get("aircraftId")).intValue());
//            result += ((temp.mObjectInHead == null) ? 0 : temp.mObjectInHead.hashCode());
            result += ((temp.transitExpires == null) ? 0 : temp.transitExpires.hashCode());
        }
        
        return result;
    }
    @Override
    public boolean equals(Object e) {
        if(e instanceof Snapshot)
        {
            if(!this._modelTime.equals(((Snapshot)e)._modelTime))
                return false;
            
            if(this.eventQueue.size()!=((Snapshot)e).eventQueue.size())
                return false;
            
            for(int i=0;i<this.eventQueue.size();i++)
            {
                if(!((Snapshot)e).eventQueue.contains(eventQueue.get(i)))
                    return false;
            }
            for(Entry<Integer, AirportFeilds> entry : this.airportActors.entrySet()){
                if(((Snapshot)e).airportActors.isEmpty())
                    return false;
                if(!((Snapshot)e).airportActors.containsValue(entry.getValue()))
                    return false;
            }   
            for(Entry<Integer, DestinationAirportFields> entry : this.destinationAirportActors.entrySet()){
                if(((Snapshot)e).destinationAirportActors.isEmpty())
                    return false;
                if(!((Snapshot)e).destinationAirportActors.containsValue(entry.getValue()))
                    return false;
            }
            
            for(Entry<Integer, TrackFields> entry : this.trackActors.entrySet()){
                if(((Snapshot)e).trackActors.isEmpty())
                    return false;
                if(!((Snapshot)e).trackActors.containsValue(entry.getValue()))
                    return false;
            }
            
        }
        return true;
    }
    
    
    
    public ArrayList<Integer> parent; // whose parents?
    
//    public DEEventQueue eventQueue; 
    public ArrayList<DEEvent> eventQueue;
    
    public int upToThisTaken;
    
    
    public String name;    // The state is resulted from choosing this event.
                           // We can recognize if the transition is timed or immediate. In timed, the name is null.
    public Time _modelTime;
    
//    public ArrayList<DEEvent> removedEvents; // Keeps events which are removed from the eventQueue in children. 
    
//    public int index; // Keeps the position in removedEvents from where the new events have been
    // added.
    
    public int _microstep;
    
    public Map<Integer,Boolean> regionsUnderAnalysis;
    
    // The key is "regionId,aircraftId". Only the entries who have 
    // true value are stored.
    public ArrayList<String> isAircraftArrived;
    
    public Map<Integer, AirportFeilds> airportActors;
    public Map<Integer, TrackFields> trackActors;
    public Map<String, Map<Integer, Token>> inputTokens;
    public Map<Integer, DestinationAirportFields> destinationAirportActors;
    
    // Store the list of moving objects which are under analysis
    public Map<Integer,String> underAnalysisMovingObjects;
    
    
    
    
}
