package ptolemy.domains.atc.kernel;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import ptolemy.actor.util.Time;
import ptolemy.data.IntToken;
import ptolemy.data.RecordToken;
import ptolemy.data.Token;
import ptolemy.domains.atc.lib.Airport;
import ptolemy.domains.atc.lib.DestinationAirport;
import ptolemy.domains.atc.lib.NMTrack;
import ptolemy.domains.de.kernel.DEEvent;
import ptolemy.domains.de.kernel.DEEventQueue;

public class AreaSnapshot {
    
    public AreaSnapshot(DEEventQueue eventQueue, String name, Time _modelTime, int _microstep) {
        super();
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
        this.eventsForPreboundaries=new ArrayList<>();
    }
    
    public AreaSnapshot(DEEventQueue eventQueue, String name, Time _modelTime, int _microstep, int upToThisTaken) {
        super();
       
        this.eventQueue=new ArrayList<>();
        if(eventQueue!=null) {
            Object[] objectArray=eventQueue.toArray();
            for(int i=0;i<objectArray.length;i++)
                this.eventQueue.add((DEEvent)objectArray[i]);
        }
        this.name = name;
        this._modelTime = _modelTime;

        this.upToThisTaken=upToThisTaken;
        this._microstep=_microstep;
        this.airportActors=new TreeMap<Integer, AirportFeilds>();
        this.destinationAirportActors=new TreeMap<Integer, DestinationAirportFields>();
        this.trackActors=new TreeMap<Integer, TrackFields>();
        this.inputTokens=new TreeMap<String, Map<Integer, Token>>();
        this.eventsForPreboundaries=new ArrayList<>();
            
    }
    
    public AreaSnapshot(ArrayList<DEEvent> eventQueue, String name, Time _modelTime, int _microstep, int upToThisTaken, Map<Integer, AirportFeilds> airportActors,
            Map<Integer, DestinationAirportFields> destinationAirportActors, Map<Integer, TrackFields> trackActors,Map<String, Map<Integer, Token>> inputTokens) {
        super();
       
        this.eventQueue=new ArrayList<>();
        if(eventQueue!=null) {
            Object[] objectArray=eventQueue.toArray();
            for(int i=0;i<objectArray.length;i++)
                this.eventQueue.add((DEEvent)objectArray[i]);
        }
        this.name = name;
        this._modelTime = _modelTime;

        this.upToThisTaken=upToThisTaken;
        this._microstep=_microstep;
        this.airportActors=new TreeMap<Integer, AirportFeilds>();
        this.destinationAirportActors=new TreeMap<Integer, DestinationAirportFields>();
        this.trackActors=new TreeMap<Integer, TrackFields>();
        this.inputTokens=new TreeMap<String, Map<Integer, Token>>();
        this.eventsForPreboundaries=new ArrayList<>();
        
        for(Entry<Integer,AirportFeilds> entry:airportActors.entrySet()) {
            this.airportActors.put(entry.getKey(), new AirportFeilds(
                    entry.getValue()._airplanes,entry.getValue()._inTransit, entry.getValue()._transitExpires));
        }
        
        for(Entry<Integer,DestinationAirportFields> entry:destinationAirportActors.entrySet()) {
            this.destinationAirportActors.put(entry.getKey(), 
                    new DestinationAirportFields(entry.getValue()._inTransit, entry.getValue()._transitExpires, entry.getValue()._called));
        }
        
        for(Entry<Integer,TrackFields> entry:trackActors.entrySet()) {
            this.trackActors.put(entry.getKey(), 
                    new TrackFields(
                            entry.getValue().called, entry.getValue().inTransit, entry.getValue().OutRoute, entry.getValue().transitExpires));
        }
        
        for(Entry<String, Map<Integer,Token>> entry: inputTokens.entrySet()) {
            Map<Integer,Token> temp=new TreeMap<>();
            for(Entry<Integer,Token> entry2:entry.getValue().entrySet()) {
                temp.put(entry2.getKey(), entry2.getValue());
            }
            this.inputTokens.put(entry.getKey(), temp);
        }
        
    }
    
    public AreaSnapshot() {
        // TODO Auto-generated constructor stub
        this.upToThisTaken=-1;
        this.eventQueue=new ArrayList<>();
        this.airportActors=new TreeMap<Integer, AirportFeilds>();
        this.destinationAirportActors=new TreeMap<Integer, DestinationAirportFields>();
        this.trackActors=new TreeMap<Integer, TrackFields>();
        this.inputTokens=new TreeMap<String, Map<Integer, Token>>();
        this.eventsForPreboundaries=new ArrayList<>();
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
        if(e instanceof AreaSnapshot)
        {
            if(this._modelTime!=null && ((AreaSnapshot)e)._modelTime!=null)
                if(!this._modelTime.equals(((AreaSnapshot)e)._modelTime))
                    return false;
            
            if(this.eventQueue.size()!=((AreaSnapshot)e).eventQueue.size())
                return false;
            
            for(int i=0;i<this.eventQueue.size();i++)
            {
                if(!((AreaSnapshot)e).eventQueue.contains(eventQueue.get(i)))
                    return false;
            }
            for(Entry<Integer, AirportFeilds> entry : this.airportActors.entrySet()){
                if(((AreaSnapshot)e).airportActors.isEmpty())
                    return false;
                if(!((AreaSnapshot)e).airportActors.containsValue(entry.getValue()))
                    return false;
            }   
            for(Entry<Integer, DestinationAirportFields> entry : this.destinationAirportActors.entrySet()){
                if(((AreaSnapshot)e).destinationAirportActors.isEmpty())
                    return false;
                if(!((AreaSnapshot)e).destinationAirportActors.containsValue(entry.getValue()))
                    return false;
            }
            
            for(Entry<Integer, TrackFields> entry : this.trackActors.entrySet()){
                if(((AreaSnapshot)e).trackActors.isEmpty())
                    return false;
                if(!((AreaSnapshot)e).trackActors.containsValue(entry.getValue()))
                    return false;
            }
            
        }
        return true;
    }
    
    public int _microstep;
    public ArrayList<DEEvent> eventQueue;
    
    public int upToThisTaken;
    public String name;    // The state is resulted from choosing this event.
    // We can recognize if the transition is timed or immediate. In timed, the name is null.
    public Time _modelTime;
    public Map<Integer, AirportFeilds> airportActors;
    public Map<Integer, TrackFields> trackActors;
    public Map<String, Map<Integer, Token>> inputTokens;
    public Map<Integer, DestinationAirportFields> destinationAirportActors;
    
    //When we execute the sequential part of an area, some internal boundary actors send messages to boundary actors,
    //which need to be stored.
    public ArrayList<DEEvent> eventsForPreboundaries;

}
