/***
 * This class is used in the reduced version of the multiple interactive 
 * coordinated actor models.
 */
package ptolemy.domains.atc.kernel;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import ptolemy.actor.util.Time;
import ptolemy.data.Token;
import ptolemy.domains.atc.lib.ChainClass;
import ptolemy.domains.de.kernel.DEEvent;
import ptolemy.domains.de.kernel.DEEventQueue;

public class GlobalSnapshot {
    
    public GlobalSnapshot(int currentCArea, int parent, Time _modelTime, int _microstep, DEEventQueue eventQueue, DEEventQueue parralelEvents) {
        super();
        this.upToThisTaken=-1;
        this.areas = new ArrayList<AreaSnapshot>();
        this.currentCArea=currentCArea;
        this.parent=new ArrayList<Integer>();
        this.parent.add(parent);
        this._modelTime=_modelTime;
        this._microstep=_microstep;
        this.eventQueue=new ArrayList<>();
        this.parallelEventQueue=new ArrayList<>();
        this.name="";
        index=0;
        
        if(eventQueue!=null) {
            Object[] objectArray=eventQueue.toArray();
            for(Object object: objectArray)
                this.eventQueue.add((DEEvent)object);
        }
        
        if(parralelEvents!=null) {
            Object[] objectArray=eventQueue.toArray();
            for(Object object: objectArray)
                this.parallelEventQueue.add((DEEvent)object);
        }
        
    }
    
    public GlobalSnapshot(int currentCArea, int parent, Time _modelTime, int _microstep, ArrayList<DEEvent> eventQueue, DEEventQueue parralelEvents) {
        super();
        this.upToThisTaken=-1;
        this.areas = new ArrayList<AreaSnapshot>();
        this.currentCArea=currentCArea;
        this.parent=new ArrayList<Integer>();
        this.parent.add(parent);
        this._modelTime=_modelTime;
        this._microstep=_microstep;
        this.eventQueue=new ArrayList<>();
        this.parallelEventQueue=new ArrayList<>();
        this.name="";
        index=0;
        
            for(Object object: eventQueue)
                this.eventQueue.add((DEEvent)object);

        
        if(parralelEvents!=null) {
            Object[] objectArray=parallelEventQueue.toArray();
            for(Object object: objectArray)
                this.parallelEventQueue.add((DEEvent)object);
        }
        
    }
    
    /**
     * This constructor is used when you create a new state and 
     * only copy the other areas except for the area under analysis.
     * @param areas
     * @param currentCArea
     * @param parent
     * @param _modelTime
     * @param _microstep
     */
    public GlobalSnapshot(ArrayList<AreaSnapshot> areas, int currentCArea, ArrayList<Integer> parent, Time _modelTime, int _microstep,ArrayList<DEEvent> eventQueue, ArrayList<DEEvent> parallelEvents) {
        super();
        
        // Since the other areas except for the currentCArea do not change,
        // we copy them
//        for(int i=0;i<areas.size();i++)
//            if(i!=currentCArea)
//                this.areas.add(areas.get(i));
//            else
//                this.areas.add(new AreaSnapshot(null, "", null, -1));
        this.currentCArea = currentCArea;
        this.upToThisTaken=-1;
        this.parent=new ArrayList<Integer>();
        for(int i=0;i<parent.size();i++)
            this.parent.add(parent.get(i));
        this._modelTime = _modelTime;
        this.inputTokens = new TreeMap<String, Map<Integer, Token>>();
        this._microstep = _microstep;
        this.eventQueue=new ArrayList<>();
        this.parallelEventQueue=new ArrayList<>();
        this.name="";
        index=0;
        
        this.areas = new ArrayList<AreaSnapshot>();
        
        for(int i=0;i<areas.size();i++) {
            this.areas.add(new AreaSnapshot(areas.get(i).eventQueue, areas.get(i).name, areas.get(i)._modelTime, areas.get(i)._microstep, areas.get(i).upToThisTaken,areas.get(i).airportActors, areas.get(i).destinationAirportActors, areas.get(i).trackActors, areas.get(i).inputTokens));
        }
        
        if(eventQueue!=null) {
//            Object[] objectArray=eventQueue.toArray();
            for(DEEvent object: eventQueue)
                this.eventQueue.add(object);
        }
        
        if(parallelEvents!=null) {
//            Object[] objectArray=eventQueue.toArray();
            for(DEEvent object: parallelEvents)
                this.parallelEventQueue.add(object);
        }
    }
    
    
    public GlobalSnapshot(ArrayList<AreaSnapshot> areas, int index, int upToThisTaken, String name,
            int currentCArea, ArrayList<Integer> parent, Time _modelTime, int _microstep,
            ArrayList<DEEvent> eventQueue, ArrayList<DEEvent> parallelEvents) {
        // TODO Auto-generated constructor stub
super();
        
        // Since the other areas except for the currentCArea do not change,
        // we copy them
//        for(int i=0;i<areas.size();i++)
//            if(i!=currentCArea)
//                this.areas.add(areas.get(i));
//            else
//                this.areas.add(new AreaSnapshot(null, "", null, -1));
        this.currentCArea = currentCArea;
        this.upToThisTaken=upToThisTaken;
        this.parent=new ArrayList<Integer>();
        for(int i=0;i<parent.size();i++)
            this.parent.add(parent.get(i));
        this._modelTime = _modelTime;
        this.inputTokens = new TreeMap<String, Map<Integer, Token>>();
        this._microstep = _microstep;
        this.eventQueue=new ArrayList<>();
        this.parallelEventQueue=new ArrayList<>();
        this.name=name;
        this.index=index;
        
        this.areas = new ArrayList<AreaSnapshot>();
        
        for(int i=0;i<areas.size();i++) {
            this.areas.add(new AreaSnapshot(areas.get(i).eventQueue, areas.get(i).name, areas.get(i)._modelTime, areas.get(i)._microstep, areas.get(i).upToThisTaken,areas.get(i).airportActors, areas.get(i).destinationAirportActors, areas.get(i).trackActors, areas.get(i).inputTokens));
        }
        
        if(eventQueue!=null) {
//            Object[] objectArray=eventQueue.toArray();
            for(DEEvent object: eventQueue)
                this.eventQueue.add(object);
        }
        
        if(parallelEvents!=null) {
//            Object[] objectArray=eventQueue.toArray();
            for(DEEvent object: parallelEvents)
                this.parallelEventQueue.add(object);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((_modelTime == null) ? 0 : _modelTime.hashCode());
        result = prime * result + ((areas == null) ? 0 : hashCodeOfAreas());
        result = prime * result + ((eventQueue == null) ? 0 : hashCodeOfEventQueue());
        result = prime * result + ((parallelEventQueue == null) ? 0 : hashCodeOfparallelEventQueue());
//        result = prime * result + currentCArea;
        return result;
    }
    



    private int hashCodeOfparallelEventQueue() {
        // TODO Auto-generated method stub
        return eventQueue.size();
    }


    public int hashCodeOfEventQueue() {
        return eventQueue.size();
    }
    
    private int hashCodeOfAreas() {
        // TODO Auto-generated method stub
        int result=1;
        for(int i=0;i<areas.size();i++)
            result+=areas.get(i).hashCode();
        return result;
    }

    
    @Override
    public boolean equals(Object e) {
        if(e instanceof GlobalSnapshot)
        {
            if(!this._modelTime.equals(((GlobalSnapshot)e)._modelTime))
                return false;
            
            if(this.areas.size()!=((GlobalSnapshot)e).areas.size())
                return false;
            
            for(int i=0;i<this.areas.size();i++)
            {
                if(!((GlobalSnapshot)e).areas.contains(areas.get(i)))
                    return false;
            }
            if(this.eventQueue.size()!=((GlobalSnapshot)e).eventQueue.size())
                return false;
            
            for(int i=0;i<this.eventQueue.size();i++)
            {
                if(!((GlobalSnapshot)e).eventQueue.contains(eventQueue.get(i)))
                    return false;
            }
            
            if(this.parallelEventQueue.size()!=((GlobalSnapshot)e).parallelEventQueue.size())
                return false;
            
            for(int i=0;i<this.parallelEventQueue.size();i++)
            {
                if(!((GlobalSnapshot)e).parallelEventQueue.contains(parallelEventQueue.get(i)))
                    return false;
            }
            
        }
        return true;
    }
    
    /**
     * Store the events of internalEventQueue into parallelEventQueue
     * @param _internalEventQueue
     */
    public void storeParallelEventQueue(DEEventQueue _internalEventQueue) {
        // TODO Auto-generated method stub
        for(Object e: _internalEventQueue.toArray())
            this.parallelEventQueue.add((DEEvent)e);
    }


    public int upToThisTaken;
    public ArrayList<DEEvent> eventQueue;
    public ArrayList<AreaSnapshot> areas; // A sub-state per each control area
    public int currentCArea; // the current control area which its state spaces is generated
    public ArrayList<Integer> parent; // whose parents?
    public Time _modelTime;
    public Map<String, Map<Integer, Token>> inputTokens; // to store the tokens of the channels
    // in the highest level of hierarchy.
    public int _microstep;
    
    /**
     * To store the events which should be taken in parallel.
     */
    public ArrayList<DEEvent> parallelEventQueue;
    public String name;
    
    /**
     * Since we are storing the parallelEvents of the regions in order of firing the regions,
     * we need to keep to which index of the parallelEventQueue we have saved the events of the previous region.
     */
    public int index;
    
    
    
    

}
