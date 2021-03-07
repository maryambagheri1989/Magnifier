package ptolemy.domains.atc.kernel.policy1;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.NoRouteToHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import de.cau.cs.kieler.klay.layered.p4nodes.bk.NeighborhoodInformation;
import ptolemy.actor.Actor;
import ptolemy.actor.CompositeActor;
import ptolemy.actor.Director;
import ptolemy.actor.FiringEvent;
import ptolemy.actor.IOPort;
import ptolemy.actor.Manager;
import ptolemy.actor.Receiver;
import ptolemy.actor.SuperdenseTimeDirector;
import ptolemy.actor.util.Time;
import ptolemy.data.ArrayToken;
import ptolemy.data.BooleanToken;
import ptolemy.data.DoubleToken;
import ptolemy.data.IntToken;
import ptolemy.data.RecordToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.domains.atc.kernel.ATCReceiver;
import ptolemy.domains.atc.kernel.AirportFeilds;
import ptolemy.domains.atc.kernel.DestinationAirportFields;
import ptolemy.domains.atc.kernel.Region;
import ptolemy.domains.atc.kernel.Snapshot;
import ptolemy.domains.atc.kernel.TrackFields;
import ptolemy.domains.atc.lib.Airport;
import ptolemy.domains.atc.lib.DestinationAirport;
import ptolemy.domains.atc.lib.ObjectsList.MovingObjectCell;
import ptolemy.domains.atc.lib.TimedStatesList;
import ptolemy.domains.atc.lib.Track;
import ptolemy.domains.de.kernel.DEEvent;
import ptolemy.domains.sdf.kernel.SDFDirector;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Nameable;
import ptolemy.kernel.util.NamedObj;

public class MagnifierDirector extends ATCDirector{

    public MagnifierDirector(CompositeEntity container, String name)
            throws IllegalActionException, NameDuplicationException {
        super(container, name);
        // TODO Auto-generated constructor stub
        numberOfRegions = new Parameter(this, "numberOfRegions");
        numberOfRegions.setExpression("1");
        numberOfRegions.setTypeEquals(BaseType.INT);
        
        networkDimension = new Parameter(this, "networkDimension");
        networkDimension.setExpression("3");
        networkDimension.setTypeEquals(BaseType.INT);
        
        dimentionOfRegion = new Parameter(this, "dimentionOfRegion");
        dimentionOfRegion.setExpression("3");
        dimentionOfRegion.setTypeEquals(BaseType.INT);
        
        indexOfFile = new Parameter(this, "indexOfFile");
        indexOfFile.setExpression("0");
        indexOfFile.setTypeEquals(BaseType.INT);
        

        
        
        
    }
    
    public Parameter numberOfRegions;
    public Parameter networkDimension;
    public Parameter dimentionOfRegion;
    public Parameter indexOfFile;
//    public Parameter iteration;
    
    @Override
    public void initialize() throws IllegalActionException {
        _stateSpace=new ArrayList<Snapshot>();
        _stopAnalysis=false;
        _informationLoaded=false;
        _timeReachableStates=new TimedStatesList(null, null);
        _timeReachableStatesMap=new HashMap<>();
        arrivalTimeToNet=new HashMap<>();
        _currentTimedState=null;
        startTime=0;
        _currentStateIndex=-1;
        deadlockDetected=false;
        timeOver=false;
        _inputGenerator=new TreeMap();
        underAnalysisMO=new TreeMap<>();
        regions=new ArrayList<Region>();
        numOfRegions=((IntToken)numberOfRegions.getToken()).intValue();
        dimension=((IntToken)networkDimension.getToken()).intValue();
        regionDimension=((IntToken)dimentionOfRegion.getToken()).intValue();
        for(int i=1;i<=numOfRegions;i++)
        {
            regions.add(new Region(i));
        }
        super.initialize();
        
    }
    
    /**
     * The magnifier starts when a storm occurs in a region.
     */
    @Override
    public void handleTrackAttributeChanged(NamedObj track)
            throws IllegalActionException {
        int id = ((IntToken) ((Track)track).trackId.getToken()).intValue();
        if (_stormyTracks.size() != 0) {
            if (_stormyTracks.containsKey(id)) {
                _stormyTracks.put(id, ((Track)track).stormy.getToken());
            } 
//                else {
//                throw new IllegalActionException(
//                        "The entry for this track has not been set in stormyTrack array ");
//            }
        }
        if(((Track)track).stormy.getToken().equals(new BooleanToken(true)) && regions!=null)
        startMagnifying(((IntToken) ((Track)track).regionId).intValue());
    }
    
    /**
     * This function, besides initializing a track, as this track and its corresponding region
     * to the neighbors of the other regions if this track is a borderline track.
     */
    @Override
    public void handleInitializedTrack(NamedObj track) throws IllegalActionException {
        int region=((IntToken) ((Track)track).regionId).intValue();
        int id=((IntToken) ((Track)track).trackId.getToken()).intValue();
        regions.get(region-1).tracks.put(id, ((Track)track));
        ArrayToken borders=(ArrayToken) ((Track)track).border.getToken();
        if(borders!=null)
        for(int i=0;i<borders.length();i++) {
            String temp=((StringToken) borders.getElement(i)).stringValue();
            if(temp!="")
            {
//                
                int regionId=Integer.valueOf(temp.substring(1));
                switch(temp.substring(0,1)) {
                case "N":
                {
                    regions.get(regionId-1).northNeighbors.put(id,((Track)track));
                    regions.get(regionId-1).neighbors.put(0, regions.get(region-1));
                    break;
                }
                case "W":
                {
                    regions.get(regionId-1).westNeighbors.put(id,((Track)track));
                    regions.get(regionId-1).neighbors.put(3, regions.get(region-1));
                    break;
                }
                case "S":
                {
                    regions.get(regionId-1).southNeighbors.put(id,((Track)track));
                    regions.get(regionId-1).neighbors.put(2, regions.get(region-1));
                    break;
                }
                case "E":
                {
                    regions.get(regionId-1).eastNeighbors.put(id,((Track)track));
                    regions.get(regionId-1).neighbors.put(1, regions.get(region-1));
                }
                }
            }
        }
        super.handleInitializedTrack(((Track)track));
    }
    
    @Override
    public void wrapup() throws IllegalActionException {
        
        int iteration=0;
        String outputFileStates="";
        String outputFileTime="";
        
        Director executiveDirector = ((CompositeActor) getContainer())
                .getExecutiveDirector();
        if(executiveDirector instanceof SDFDirector) {
            iteration=((SDFDirector)executiveDirector)._iterationCount;
            outputFileStates="outputS"+iterationCount(iteration+1)+".txt";
            outputFileTime="outputT"+iterationCount(iteration+1)+".txt";
        }
        else if(((IntToken)indexOfFile.getToken()).intValue()==0) {
            outputFileStates="outputS.txt";
            outputFileTime="outputT.txt";
        }
        else {
            outputFileStates="outputS"+((IntToken)indexOfFile.getToken()).intValue()+".txt";
            outputFileTime="outputT"+((IntToken)indexOfFile.getToken()).intValue()+".txt";
        }
        

        super.wrapup();
        _disabledActors = null;
        _informationLoaded=false;
        _stopAnalysis=false;
        synchronized (_eventQueueLock) {
            _eventQueue.clear();
        }
        _noMoreActorsToFire = false;
        _microstep = 0;
        try {

              File file=new File(outputFileStates);
              File fileTime=new File(outputFileTime);
            
//            FileWriter writer = new FileWriter(file, false);
            FileWriter writer = new FileWriter(file, true);
            PrintWriter _writer =new PrintWriter(writer);
            
            FileWriter writerTime = new FileWriter(fileTime, true);
            PrintWriter _writerTime =new PrintWriter(writerTime);

            if(deadlockDetected) {
                _writerTime.print("deadlock"+"\n");
                _writer.print("deadlock"+"\n");
            }
            else if(timeOver)
            {
                _writerTime.print("timeOver"+"\n");
                _writer.print("timeOver"+"\n");
            }
            else {
                if(startTime==0) // Magnifier is not called at all
                    _writerTime.print(0+"\n");
                else
                    _writerTime.print((System.currentTimeMillis()-startTime)+"\n");
                _writer.print(_stateSpace.size()+"\n");
            }
            _writerTime.close();
            

            _writer.close();
            deadlockDetected=false;
            timeOver=false;
            _stateSpace.clear();
            _timeReachableStates.clear();
            _timeReachableStatesMap.clear();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    
    private String _makeString(int i,Snapshot snapshot) {
        // TODO Auto-generated method stub
        String k=String.valueOf(i);
        k+="; Parent: "+snapshot.parent+"; ActionName: "+snapshot.name+"; ModelTime: "+snapshot._modelTime+
                "; microstep: "+snapshot._microstep+"; eventQueue: \n";
        Object[] eventArray=snapshot.eventQueue.toArray();
        int j=-1;
        for(Object object: eventArray){
            j++;
            k+="Actor: "+((DEEvent)object).actor().getFullName();
            if(snapshot.trackActors.containsKey(j)){
                if(snapshot.trackActors.get(j).inTransit!=null){
                    k+="; inTransit: "+((RecordToken)snapshot.trackActors.get(j).inTransit).get("aircraftId")
                            +"; transitExpire: "+snapshot.trackActors.get(j).transitExpires+"\n";
                }
                else{
                    k+="; inTransit: "+"null \n";
                }
            }
            if(snapshot.airportActors.containsKey(j)){
                if(snapshot.airportActors.get(j)._inTransit!=null)
                    k+="; inTransit: "+((RecordToken)snapshot.airportActors.get(j)._inTransit).get("aircraftId");
                else
                    k+="; inTransit: null";
                k+="; transitExpire: "+snapshot.airportActors.get(j)._transitExpires;      
                for(int l=0;l<snapshot.airportActors.get(j)._airplanes.size();l++)
                    k+="; A"+snapshot.airportActors.get(j)._airplanes.get(l).get("aircraftId");
                k+="\n";
            }
            if(snapshot.destinationAirportActors.containsKey(j)){
                if(snapshot.destinationAirportActors.get(j)._inTransit!=null)
                    k+="; inTransit: "+((RecordToken)snapshot.destinationAirportActors.get(j)._inTransit).get("aircraftId");
                else
                    k+="; inTransit: null";
                k+="; transitExpire: "+snapshot.destinationAirportActors.get(j)._transitExpires;      
                k+="\n";
            }
                    
        }
        return k;
    }
    
    @Override
    public boolean postfire() throws IllegalActionException {
        
        //************Added by Maryam :3600000
        if(startTime>0 && System.currentTimeMillis()-startTime>=3600000) {
            timeOver=true;
               return false;
        }
        if(_stopAnalysis==true)
            return false;
//        _timeReachableStates.remove();
        try {
            _timeReachableStates.take();
            _timeReachableStatesMap.remove(_currentTimedState);
           
    
            if(!_timeReachableStates.isEmpty()){
                _noMoreActorsToFire=false;
//              _currentStateIndex=_timeReachableStates.peek();
                _currentStateIndex=_timeReachableStates.get().stateIndex;
                Snapshot temp =_stateSpace.get(_currentStateIndex);
                _currentTimedState=temp;
                

                _eventQueue.clear();
                Object[] eventArray=temp.eventQueue.toArray();
                for(Object object: eventArray){
                    _eventQueue.put((DEEvent)object);
                }
                _fillModel(temp);
            }
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        //************
        
        boolean result = super.postfire();

        // If any output ports still have tokens to transfer,
        // request a refiring at the current time.
        CompositeActor container = (CompositeActor) getContainer();
        Iterator<IOPort> outports = container.outputPortList().iterator();
        boolean moreOutputsToTransfer = false;
        while (outports.hasNext() && !moreOutputsToTransfer) {
            IOPort outport = outports.next();
            for (int i = 0; i < outport.getWidthInside(); i++) {
                if (outport.hasNewTokenInside(i)) {
                    moreOutputsToTransfer = true;
                    break;
                }
            }
        }

        // Reset the microstep to zero if the next event is
        // in the future.
        synchronized (_eventQueueLock) {
            if (!_eventQueue.isEmpty() && !moreOutputsToTransfer) {
                DEEvent next = _eventQueue.get();
                if (next.timeStamp().compareTo(getModelTime()) > 0) {
                    _microstep = 0;
                }
            }
            boolean stop = ((BooleanToken) stopWhenQueueIsEmpty.getToken())
                    .booleanValue();

            // Request refiring and/or stop the model.
            // There are two conditions to stop the model.
            // 1. There are no more actors to be fired (i.e. event queue is
            // empty), and either of the following conditions is satisfied:
            //     a. the stopWhenQueueIsEmpty parameter is set to true.
            //     b. the current model time equals the model stop time.
            // 2. The event queue is not empty, but the current time exceeds
            // the stop time.
            if (moreOutputsToTransfer) {
                fireContainerAt(getModelTime());
            } else if (_noMoreActorsToFire && (stop
                    || getModelTime().compareTo(getModelStopTime()) == 0)) {
                if (_debugging) {
                    _debug("No more actors to fire and time to stop.");
                }
                _exceedStopTime = true;
                result = false;
            } else if (_exceedStopTime) {
                // If the current time is bigger than the stop time,
                // stop the model execution.
                result = false;
            } else if (isEmbedded() && !_eventQueue.isEmpty()) {
                // If the event queue is not empty and the container is an
                // embedded model, ask the upper level director in the
                // hierarchy to refire the container at the timestamp of the
                // first event of the local event queue.
                // This design allows the upper level director (actually all
                // levels in hierarchy) to keep a relatively short event queue.
                _requestFiring();
            }
        }
        if (isEmbedded()) {
            // Indicate that fireAt() requests should be passed up the
            // hierarchy if they are made before the next iteration.
            _delegateFireAt = true;
        }
        // NOTE: The following commented block enforces that no events with
        // different tags can exist in the same receiver.
        // This is a quite different semantics from the previous designs,
        // and its effects are still under investigation and debate.
        //        // Clear all of the contained actor's input ports.
        //        for (Iterator actors = ((CompositeActor)getContainer())
        //                .entityList(Actor.class).iterator();
        //                actors.hasNext();) {
        //            Entity actor = (Entity)actors.next();
        //            Iterator ports = actor.portList().iterator();
        //            while (ports.hasNext()) {
        //                IOPort port = (IOPort)ports.next();
        //                if (port.isInput()) {
        //                    // Clear all receivers.
        //                    Receiver[][] receivers = port.getReceivers();
        //                    if (receivers == null) {
        //                        throw new InternalErrorException(this, null,
        //                                "port.getReceivers() returned null! "
        //                                + "This should never happen. "
        //                                + "port was '" + port + "'");
        //                    }
        //                    for (int i = 0; i < receivers.length; i++) {
        //                        Receiver[] receivers2 = receivers[i];
        //                        for (int j = 0; j < receivers2.length; j++) {
        //                            receivers2[j].clear();
        //                        }
        //                    }
        //                }
        //            }
        //        }
        return result;
    }

    
    @Override
    protected Actor _getNextActorToFire() throws IllegalActionException {
        if (_eventQueue == null) {
            throw new IllegalActionException(
                    "Fire method called before the preinitialize method.");
        }
        //**********Added by Maryam
        boolean timeIncreased=false;
      //**********
        
        Actor actorToFire = null;
        DEEvent lastFoundEvent = null;
        DEEvent nextEvent = null;

        // Keep taking events out until there are no more events that have the
        // same tag and go to the same destination actor, or until the queue is
        // empty, or until a stop is requested.
        // LOOPLABEL::GetNextEvent
        while (!_stopRequested) {
            // Get the next event from the event queue.
            if (_stopWhenQueueIsEmpty) {
                if (_eventQueue.isEmpty()) {
                    // If the event queue is empty,
                    // jump out of the loop: LOOPLABEL::GetNextEvent
                    break;
                }
            }

            if (isEmbedded()) {
                // If the director is not at the top level.
                if (_eventQueue.isEmpty()) {
                    // This could happen if the container simply fires
                    // this composite at times it chooses. Most directors
                    // do this (SDF, SR, Continuous, etc.). It can also
                    // happen if an input is provided to a parameter port
                    // and the container is DE.
                    // In all these cases, no actors inside need to be
                    // fired.
                    break;
                }
                // For an embedded DE director, the following code prevents
                // the director from reacting to future events with bigger
                // time values in their tags.
                // For a top-level DE director, there is no such constraint
                // because the top-level director is responsible to advance
                // simulation by increasing the model tag.
                nextEvent = _eventQueue.get();

                // An embedded director should process events
                // that only happen at the current tag.
                // If the event is in the past, that is an error,
                // because the event should have been consumed in prefire().
                if (nextEvent.timeStamp().compareTo(getModelTime()) < 0) {
                    // missed an event
                    throw new IllegalActionException(
                            "Fire: Missed an event: the next event tag "
                                    + nextEvent.timeStamp() + " :: "
                                    + nextEvent.microstep()
                                    + " is earlier than the current model tag "
                                    + getModelTime() + " :: " + _microstep
                                    + " !");
                }

                // If the event is in the future time, it is ignored
                // and will be processed later. There is some complexity
                // here for backward compatibility with directors that do
                // not support superdense time. If the enclosing director
                // does not support superdense time, then we ignore the
                // microstep. Otherwise, we require the microstep of
                // the event to match the microstep that was set in
                // prefire(), which matches the microstep of the enclosing
                // director.
                boolean microstepMatches = true;
                Nameable container = getContainer();
                if (container instanceof CompositeActor) {
                    Director executiveDirector = ((CompositeActor) container)
                            .getExecutiveDirector();
                    // Some composites, such as RunCompositeActor want to be treated
                    // as if they are at the top level even though they have an executive
                    // director, so be sure to check _isTopLevel().
                    if (executiveDirector instanceof SuperdenseTimeDirector
                            && !_isTopLevel()) {
                        // If the next event microstep in the past (which it should
                        // not be normally), then we will consider it to match.
                        microstepMatches = nextEvent.microstep() <= _microstep;
                    }
                }

                int comparison = nextEvent.timeStamp()
                        .compareTo(getModelTime());
                //****************Changed by Maryam
               // if (comparison > 0 || comparison == 0 && !microstepMatches) { ///????????????????
                if (comparison > 0) {
                    // reset the next event
                    nextEvent = null;

                    // jump out of the loop: LOOPLABEL::GetNextEvent
                    break;
                }
            } else { // if (!topLevel)
                // If the director is at the top level
                // If the event queue is empty, normally
                // a blocking read is performed on the queue.
                // However, there are two conditions that the blocking
                // read is not performed, which are checked below.
                if (_eventQueue.isEmpty()) {
                    // The two conditions are:
                    // 1. An actor to be fired has been found; or
                    // 2. There are no more events in the event queue,
                    // and the current time is equal to the stop time.
                    if (actorToFire != null
                            || getModelTime().equals(getModelStopTime())) {
                        // jump out of the loop: LOOPLABEL::GetNextEvent
                        break;
                    }
                }

                // Otherwise, if the event queue is empty,
                // a blocking read is performed on the queue.
                // stopFire() needs to also cause this to fall out!
                synchronized (_eventQueueLock) {
                    while (_eventQueue.isEmpty() && !_stopRequested
                            && !_stopFireRequested) {
                        if (_debugging) {
                            _debug("Queue is empty. Waiting for input events.");
                        }

                        try {
                            // NOTE: Release the read access held
                            // by this thread to prevent deadlocks.
                            // NOTE: If a ChangeRequest has been requested,
                            // then _eventQueue.notifyAll() is called
                            // and stopFire() is called, so we will stop
                            // waiting for events. However,
                            // CompositeActor used to call stopFire() before
                            // queuing the change request, which created the risk
                            // that the below wait() would be terminated by
                            // a notifyAll() on _eventQueue with _stopFireRequested
                            // having been set, but before the change request has
                            // actually been filed.  See CompositeActor.requestChange().
                            // Does this matter? It means that on the next invocation
                            // of the fire() method, we could resume waiting on an empty queue
                            // without having filed the change request. That filing will
                            // no longer succeed in interrupting this wait, since
                            // stopFire() has already been called. Only on the next
                            // instance of change request would the first change
                            // request get a chance to execute.
                            workspace().wait(_eventQueueLock);
                        } catch (InterruptedException e) {
                            // If the wait is interrupted,
                            // then stop waiting.
                            break;
                        }
                    } // Close the blocking read while loop

                    // To reach this point, either the event queue is not empty,
                    // or _stopRequested or _stopFireRequested is true, or an interrupted exception
                    // happened.
                    if (_eventQueue.isEmpty()) {
                        // Stop is requested or this method is interrupted.
                        // This can occur, for example, if a change has been requested.
                        // jump out of the loop: LOOPLABEL::GetNextEvent
                        return null;
                    }
                    // At least one event is found in the event queue.
                    nextEvent = _eventQueue.get();
                } // Close synchronized block
            }

            // This is the end of the different behaviors of embedded and
            // top-level directors on getting the next event.
            // When this point is reached, the nextEvent can not be null.
            // In the rest of this method, this is not checked any more.

            // If the actorToFire is null, find the destination actor associated
            // with the event just found. Store this event as lastFoundEvent and
            // go back to continue the GetNextEvent loop.
            // Otherwise, check whether the event just found goes to the
            // same actor to be fired. If so, dequeue that event and continue
            // the GetNextEvent loop. Otherwise, jump out of the GetNextEvent
            // loop.
            // TESTIT
            if (actorToFire == null) {
                // If the actorToFire is not set yet,
                // find the actor associated with the event just found,
                // and update the current tag with the event tag.
                Time currentTime;
                int depth = 0;
                try {
                    synchronized (_eventQueueLock) {
                        lastFoundEvent = _eventQueue.get();
                        currentTime = _consultTimeRegulators(
                                lastFoundEvent.timeStamp());

                        // NOTE: Synchronize to real time here for backward compatibility,
                        // but the preferred way to do this is now to use a
                        // {@link SynchronizeToRealTime} attribute, which implements the
                        //  {@link TimeRegulator} interface.
                        if (_synchronizeToRealTime) {
                            // If synchronized to the real time.
                            Manager manager = ((CompositeActor) getContainer())
                                    .getManager();
                            while (!_stopRequested && !_stopFireRequested) {
                                lastFoundEvent = _eventQueue.get();
                                currentTime = lastFoundEvent.timeStamp();

                                if (currentTime
                                        .compareTo(getModelStopTime()) > 0) {
                                    // Next event is past the stop time of the model.
                                    // Do not stall.
                                    break;
                                }

                                long elapsedTime = elapsedTimeSinceStart();

                                // NOTE: We assume that the elapsed time can be
                                // safely cast to a double.  This means that
                                // the DE domain has an upper limit on running
                                // time of Double.MAX_VALUE milliseconds.
                                double elapsedTimeInSeconds = elapsedTime
                                        / 1000.0;
                                ptolemy.actor.util.Time elapsed = new ptolemy.actor.util.Time(
                                        this, elapsedTimeInSeconds);
                                if (currentTime.compareTo(elapsed) <= 0) {
                                    // Enough real time has passed already. Do not stall.
                                    break;
                                }

                                // NOTE: We used to do the following, but it had a limitation.
                                // In particular, if any user code also calculated the elapsed
                                // time and then constructed a Time object to post an event
                                // on the event queue, there was no assurance that the quantization
                                // would be the same, and hence it was possible for that event
                                // to be in the past when posted, even if done in the same thread.
                                // To ensure that the comparison of current time against model time
                                // always yields the same result, we have to do the comparison using
                                // the Time class, which is what the event queue does.
                                /*
                                if (currentTime.getDoubleValue() <= elapsedTimeInSeconds) {
                                    break;
                                }*/

                                long timeToWait = (long) (currentTime
                                        .subtract(elapsed).getDoubleValue()
                                        * 1000.0);

                                if (timeToWait > 0) {
                                    if (_debugging) {
                                        _debug("Waiting for real time to pass: "
                                                + timeToWait);
                                    }

                                    try {
                                        // NOTE: The built-in Java wait() method
                                        // does not release the
                                        // locks on the workspace, which would block
                                        // UI interactions and may cause deadlocks.
                                        // SOLUTION: explicitly release read permissions.
                                        depth = _workspace
                                                .releaseReadPermission();
                                        // Allow change requests to execute immediately while we are waiting.
                                        // This will have the side effect of executing any pending change requests.
                                        setDeferringChangeRequests(false);
                                        // Tell the manager what thread is waiting.
                                        manager.setWaitingThread(
                                                Thread.currentThread());
                                        _eventQueueLock.wait(timeToWait);
                                    } catch (InterruptedException ex) {
                                        // Ignore and circulate around the loop.
                                        // The interrupt could be due to a change request,
                                        // which we will want to process.
                                        // This used to do the following with flawed reasoning:
                                        /*
                                        throw new IllegalActionException(
                                            this,
                                            ex,
                                            "Thread interrupted when waiting for"
                                                    + " real time to match model time.");
                                        */
                                        // The reasoning was:
                                        // Continue executing?
                                        // No, because this could be a problem if any
                                        // actor assumes that model time always exceeds
                                        // real time when synchronizeToRealTime is set.
                                        //
                                        // But this is flawed because we are in a while loop
                                        // that will check again for matching to real time.
                                        // EAL 10/27/15.
                                    } finally {
                                        setDeferringChangeRequests(true);
                                        manager.setWaitingThread(null);
                                    }
                                }
                            } // while
                              // If stopFire() has been called, then the wait for real
                              // time above was interrupted by a change request. Hence,
                              // real time will not have reached the time of the first
                              // event in the event queue. If we allow this method to
                              // proceed, it will set model time to that event time,
                              // which is in the future. This violates the principle
                              // of synchronize to real time.  Hence, we must return
                              // without processing the event or incrementing time.

                            // NOTE: CompositeActor used to call stopFire() before
                            // queuing the change request, which created the risk
                            // that the above wait() would be terminated by
                            // a notifyAll() on _eventQueue with _stopFireRequested
                            // having been set, but before the change request has
                            // actually been filed.  See CompositeActor.requestChange().
                            // Does this matter? It means that on the next invocation
                            // of the fire() method, we could resume processing the
                            // same event, waiting for real time to elapse, without
                            // having filed the change request. That filing will
                            // no longer succeed in interrupting this wait, since
                            // stopFire() has already been called. Alternatively,
                            // before we get to the wait for real time in the next
                            // firing, the change request could complete and be
                            // executed.
                            if (_stopRequested || _stopFireRequested) {
                                return null;
                            }
                        } // if (_synchronizeToRealTime)
                    } // sync
                } finally {
                    if (depth > 0) {
                        _workspace.reacquireReadPermission(depth);
                    }
                }

                // Consume the earliest event from the queue. The event must be
                // obtained here, since a new event could have been enqueued
                // into the queue while the queue was waiting. Note however
                // that this would usually be an error. Any other thread that
                // posts events in the event queue should do so in a change request,
                // which will not be executed during the above wait.
                // Nonetheless, we are conservative here, and take the earliest
                // event in the event queue.
                synchronized (_eventQueueLock) {
                    lastFoundEvent = _eventQueue.take();
                    currentTime = lastFoundEvent.timeStamp();
                    actorToFire = lastFoundEvent.actor();

                    // NOTE: The _enqueueEvent method discards the events
                    // for disabled actors.
                    if (_disabledActors != null
                            && _disabledActors.contains(actorToFire)) {
                        // This actor has requested not to be fired again.
                        if (_debugging) {
                            _debug("Skipping disabled actor: ",
                                    ((Nameable) actorToFire).getFullName());
                        }

                        actorToFire = null;

                        // start a new iteration of the loop:
                        // LOOPLABEL::GetNextEvent
                        continue;
                    }

                    // Advance the current time to the event time.
                    // NOTE: This is the only place that the model time changes.
                    
                    //*************Added by Maryam
                    if(currentTime.compareTo(getModelTime())>0)
                    {
                        // We have a timed transition
                        timeIncreased=true;
                        _stateSpace.get(_nextStateIndex).upToThisTaken++;
                    }
                    else
                    {
                        _stateSpace.get(_currentStateIndex).upToThisTaken++;
                    }
                    //*************
                    
                    setModelTime(currentTime);

                    // Advance the current microstep to the event microstep.
                    _microstep = lastFoundEvent.microstep();
                    if (_debugging) {
                        _debug("Current time is: (" + currentTime + ", "
                                + _microstep + ")");
                    }
                    // Exceeding stop time means the current time is strictly
                    // bigger than the model stop time.
                    if (currentTime.compareTo(getModelStopTime()) > 0) {
                        if (_debugging) {
                            _debug("Current time has passed the stop time.");
                        }

                        _exceedStopTime = true;
                        return null;
                    }
                }
            } else { // i.e., actorToFire != null
                // In a previous iteration of this while loop,
                // we have already found an event and the actor to react to it.
                // Check whether the newly found event has the same tag
                // and destination actor. If so, they are
                // handled at the same time. For example, a pure
                // event and a trigger event that go to the same actor.
                if (nextEvent.hasTheSameTagAs(lastFoundEvent)
                        && nextEvent.actor() == actorToFire) {
                    // Consume the event from the queue and discard it.
                    // In theory, there should be no event with the same depth
                    // as well as tag because
                    // the DEEvent class equals() method returns true in this
                    // case, and the CalendarQueue class does not enqueue an
                    // event that is equal to one already on the queue.
                    // Note that the Repeat actor, for one, produces a sequence
                    // of outputs, each of which will have the same microstep.
                    // These reduce to a single event in the event queue.
                    // The DEReceiver in the downstream port, however,
                    // contains multiple tokens. When the one event on
                    // event queue is encountered, then the actor will
                    // be repeatedly fired until it has no more input tokens.
                    // However, there could be events with the same tag
                    // and different depths, e.g. a trigger event and a pure
                    // event going to the same actor.
                    synchronized (_eventQueueLock) {
                        DEEvent temp=_eventQueue.take();
                      //*************Added by Maryam
                        // Although this would not happen for us, because in our model
                        // every track actor is fired through only one event
                        if(timeIncreased==true)
                        {
                            // We have a timed transition
                            _stateSpace.get(_nextStateIndex).upToThisTaken++;
                        }
                        else
                        {
                            _stateSpace.get(_currentStateIndex).upToThisTaken++;
                        }
                        //*************
                    }
                } else {
                    // Next event has a future tag or a different destination.
                    break;
                }
            }
            if (actorToFire != null && _aspectsPresent) {
                if (_actorsFinished.contains(actorToFire)) {
                    _actorsFinished.remove(actorToFire);
                } else if (!_schedule((NamedObj) actorToFire, getModelTime())) {
                    _nextScheduleTime.get(_aspectForActor.get(actorToFire))
                            .add(getModelTime());
                    if (_actorsInExecution == null) {
                        _actorsInExecution = new HashMap();
                    }
                    List<DEEvent> events = _actorsInExecution.get(actorToFire);
                    if (events == null) {
                        events = new ArrayList<DEEvent>();
                    }
                    events.add(lastFoundEvent);
                    _actorsInExecution.put(actorToFire, events);
                    actorToFire = null;
                }
            }
        } // close the loop: LOOPLABEL::GetNextEvent

        // Note that the actor to be fired can be null.
        return actorToFire;
    }
    
    @Override
    protected boolean _checkForNextEvent() throws IllegalActionException {
        // The following code enforces that a firing of a
        // DE director only handles events with the same tag.
        // If the earliest event in the event queue is in the future,
        // this code terminates the current iteration.
        // This code is applied on both embedded and top-level directors.
        synchronized (_eventQueueLock) {
            if (!_eventQueue.isEmpty()) {
                DEEvent next = _eventQueue.get();

                if (next.timeStamp().compareTo(getModelTime()) > 0) {
                    // If the next event is in the future time,
                    // jump out of the big while loop in fire() and
                    // proceed to postfire().
                    return false;
                }  //*************This else if is removed by Maryam, because we do not need to compare microstep
                /*
                else if (next.microstep() > _microstep) {
                    // If the next event has a bigger microstep,
                    // jump out of the big while loop in fire() and
                    // proceed to postfire().
                    return false;
                } */
                //********** The else if below is changed by Maryam
                //else if (next.timeStamp().compareTo(getModelTime()) < 0
                     //  || next.microstep() < _microstep) {
                 else if (next.timeStamp().compareTo(getModelTime()) < 0) {
                    throw new IllegalActionException(
                            "The tag of the next event (" + next.timeStamp()
                                    + "." + next.microstep()
                                    + ") can not be less than"
                                    + " the current tag (" + getModelTime()
                                    + "." + _microstep + ") !");
                } else {
                    // The next event has the same tag as the current tag,
                    // indicating that at least one actor is going to be
                    // fired at the current iteration.
                    // Continue the current iteration.
                }
            } //************** Added by Maryam
            else{
                return false;
            }
            //**************
        }
        return true;
    }
    
    
    protected boolean _checkForNextEventInParent() throws IllegalActionException {
        // The following code enforces that a firing of a
        // DE director only handles events with the same tag.
        // If the earliest event in the event queue is in the future,
        // this code terminates the current iteration.
        // This code is applied on both embedded and top-level directors.
//        Object[] objectArray=_stateSpace.get(_currentStateIndex).eventQueue.toArray();
        if(_stateSpace.get(_currentStateIndex).eventQueue.size()!=0 && 
                _stateSpace.get(_currentStateIndex).upToThisTaken+1 < _stateSpace.get(_currentStateIndex).eventQueue.size()) {
            DEEvent next=_stateSpace.get(_currentStateIndex).eventQueue.get(_stateSpace.get(_currentStateIndex).upToThisTaken+1);
            if (next.timeStamp().compareTo(getModelTime()) > 0) {
                // If the next event is in the future time,
                // jump out of the big while loop in fire() and
                // proceed to postfire().
                return false;
            }  
            else if (next.timeStamp().compareTo(getModelTime()) < 0) {
                throw new IllegalActionException(
                        "The tag of the next event (" + next.timeStamp()
                                + "." + next.microstep()
                                + ") can not be less than"
                                + " the current tag (" + getModelTime()
                                + "." + _microstep + ") !");
            } else {
                // The next event has the same tag as the current tag,
                // indicating that at least one actor is going to be
                // fired at the current iteration.
                // Continue the current iteration.
            }
        }
        else {
            return false;
        }
        return true;
    }
    
    
    int roundCounter = 0;
    @Override
    public void fire() throws IllegalActionException {
        if (_debugging) {
            _debug("========= " + this.getName() + " director fires at "
                    + getModelTime() + "  with microstep as " + _microstep);
        }
        roundCounter++;
    
//        System.out.println("--------------------------" + roundCounter + "--------------------------");
        
        //**********Added by Maryam
        ArrayList<Integer[]> forkNodes=new ArrayList<Integer[]>(); 
        tempStates=new HashMap<>();
        //******************
        // NOTE: This fire method does not call super.fire()
        // because this method is very different from that of the super class.
        // A BIG while loop that handles all events with the same tag.
        int counter = 0;
        while (true) {  
            int result = _fire();
            
            //************Added by Maryam
             if((_informationLoaded && isAnalysisEnough() && underAnalysisMO.isEmpty()) || deadlockDetected==true) {
                 _stopAnalysis=true;
                 return;
             }
            //****************************
            
            assert result <= 1 && result >= -1;
            if (result == 1) {
                continue;
            } else if (result == -1) {
                _noActorToFire();
                return;
            } // else if 0, keep executing

            // after actor firing, the subclass may wish to perform some book keeping
            // procedures. However in this class the following method does nothing.
            _actorFired();
            //************ Changed by Maryam
            if ( recentlyTimeStVisited!=-1 || !_checkForNextEvent()) {
                //************ Added by Maryam
                boolean flag=false;
                int parentIndex=-1;
                if(recentlyTimeStVisited==-1) {
                    // If this variable is not -1, then we are visiting a duplicative state
                    // in tempState and have to remove it from _stateSpace... Otherwise the state
                    // is a time state

                        int x=-1;
                 
                        if(_timeReachableStatesMap.containsKey(_stateSpace.get(_currentStateIndex))) {
                            x=_timeReachableStatesMap.get(_stateSpace.get(_currentStateIndex)); //returns index in stateSpace array
                        }

                        if(x!=-1) { // timeReachableStates contains temp2
                            parentIndex=_stateSpace.get(_currentStateIndex).parent.get(0);
                            _cleanModel(_stateSpace.get(_currentStateIndex).eventQueue);
                            _stateSpace.remove(_currentStateIndex);
                           _stateSpace.get(x).parent.add(parentIndex);
                        }
                        else {
//                            _timeReachableStates.add(_currentStateIndex);
                            _timeReachableStates.add(_currentStateIndex, _stateSpace.get(_currentStateIndex)._modelTime);
                            _timeReachableStatesMap.put(_stateSpace.get(_currentStateIndex), _currentStateIndex);
                        }

                }
                
                do{
                   
                    if(parentIndex==-1) {
                        _cleanModel(_stateSpace.get(_currentStateIndex).eventQueue);
                        _currentStateIndex=_stateSpace.get(_currentStateIndex).parent.get(0);
                    }
                    else {
                        _currentStateIndex=parentIndex;
                        parentIndex=-1;
                    }
                    if(!forkNodes.isEmpty() && _currentStateIndex<forkNodes.get(forkNodes.size()-1)[0])
                    {
                        // We have passed the node in forkCounter and reached its parent
                        if(recentlyTimeStVisited!=-1) {
                            forkNodes.get(forkNodes.size()-1)[1]--;
                            int t=recentlyTimeStVisited;
                            while(forkNodes.get(forkNodes.size()-1)[0]!=t)
                            {
                                int parent=_stateSpace.get(t).parent.get(0);
                                _stateSpace.remove(t);
                                t=parent;
                                
                            }
                            recentlyTimeStVisited=-1;
                        }
                        if(forkNodes.get(forkNodes.size()-1)[1]==0)
                        {
                            //No child remains for this node and this node has to be removed.
                            recentlyTimeStVisited=forkNodes.get(forkNodes.size()-1)[0];
                            forkNodes.remove(forkNodes.size()-1);
//                            forkCounter--;
                        }
                    }
                    if(_stateSpace.get(_currentStateIndex).upToThisTaken==-1)
                    {
                        flag=true;
//                        tempTimeReachableStates.clear();
                        forkNodes.clear();
                        tempStates.clear();
                        break;
                    }

                } while(!_checkForNextEventInParent());
                 if(flag){
                    break;
                }
                 
                if(!forkNodes.isEmpty() && forkNodes.get(forkNodes.size()-1)[0]==_currentStateIndex) //We've already added this fork  node
                {
                    if(recentlyTimeStVisited==-1){
                        Integer [] a={forkNodes.get(forkNodes.size()-1)[0],forkNodes.get(forkNodes.size()-1)[1]+1};
                        forkNodes.remove(forkNodes.size()-1);
                        forkNodes.add(a);
                    }
                    else{
                        int t=recentlyTimeStVisited;
                        while(_currentStateIndex!=t)
                        {
                            int parent=_stateSpace.get(t).parent.get(0);
                            _stateSpace.remove(t);
                            t=parent;
                            
                        }
                        recentlyTimeStVisited=-1;
                    }
                    
                    
                }
                else{
                    if(recentlyTimeStVisited==-1){
                        Integer [] a={_currentStateIndex,2};
                        forkNodes.add(a);
                    }
                    else{
                        Integer [] a={_currentStateIndex,1};
                        forkNodes.add(a);
                        int t=recentlyTimeStVisited;
                        while(_currentStateIndex!=t)
                        {
                            int parent=_stateSpace.get(t).parent.get(0);
                            _stateSpace.remove(t);
                            t=parent;
                            
                        }
                        recentlyTimeStVisited=-1;
                        
                    }
                }
                //Fill _eventQueue
                _eventQueue.clear();
                for(int i=_stateSpace.get(_currentStateIndex).upToThisTaken+1;i<_stateSpace.get(_currentStateIndex).eventQueue.size();i++)
                    _eventQueue.put(_stateSpace.get(_currentStateIndex).eventQueue.get(i));
                // Fill MODEL
                _fillModel(_stateSpace.get(_currentStateIndex));
                    
                //*************
                
             //************Removed by Maryam   
              //  break;
              //*************
            } // else keep executing in the current iteration
            counter ++;

        } // Close the BIG while loop.
//        System.out.println("Total:" + counter);
        // Since we are now actually stopping the firing, we can set this false.
        _stopFireRequested = false;

        if (_debugging) {
            _debug("DE director fired!");
        }
    }
    
    private boolean isAnalysisEnough() {
        // TODO Auto-generated method stub
        // If the regions which are not under analysis and have at least one under
        // analysis region that sends them inputs, receive their aircraft at the predetermined times

        boolean flag=false;
        for(int i=0;i<regions.size();i++)
        {
            if(regions.get(i).underAnalysis==false) {
             // The unaffected regions receive how many inputs from those of their neighbors which are under analysis
                Map<Integer, Integer> nieghbors=hasUnderAnalysisNeighbor(regions.get(i));
                int count=0;
                for(Entry<Integer, Integer> entry: nieghbors.entrySet())
                    count+=entry.getValue();
                if(count==0) { // this region does not have affected neighbors
                    // or does not receive input from an affected neighbor
                    continue;
                }
                if(regions.get(i).aircraftHasArrived.size()!=count)
                    return false;
                flag=true;// We have found at least one unaffected region who receives
                // all of its inputs at the pre-specified times
            }
        }
        if(flag==true)
            return true;
        else
            return false;
    }

    private Map<Integer, Integer> hasUnderAnalysisNeighbor(Region region) {
        // TODO Auto-generated method stub
        Map<Integer, Integer> count=new TreeMap<>();
        // if this region has north neighborhood
        for(Entry<Integer, Integer> entry: region.numInputfrom.entrySet()) {
            if(region.neighbors.containsKey(0) && region.neighbors.get(0).id==entry.getKey()) // North neighbor
                if(region.neighbors.get(0).underAnalysis)
                    count.put(entry.getKey(), entry.getValue());
            if(region.neighbors.containsKey(1) && region.neighbors.get(1).id==entry.getKey()) // East neighbor
                if(region.neighbors.get(1).underAnalysis)
                    count.put(entry.getKey(), entry.getValue());
            if(region.neighbors.containsKey(2) && region.neighbors.get(2).id==entry.getKey()) // South neighbor
                if(region.neighbors.get(2).underAnalysis)
                    count.put(entry.getKey(), entry.getValue());
            if(region.neighbors.containsKey(3) && region.neighbors.get(3).id==entry.getKey()) // West neighbor
                if(region.neighbors.get(3).underAnalysis)
                    count.put(entry.getKey(), entry.getValue());
        }
        return count;
    }



    private void _cleanModel(ArrayList<DEEvent> eventQueue) throws IllegalActionException {
        // TODO Auto-generated method stub
        for(DEEvent object: eventQueue){
            NamedObj actor=(NamedObj) object.actor();
            
            IOPort port= object.ioPort();
            if(port!=null){
                Receiver[][] receiver=port.getReceivers();
                for(int i=0;i<receiver.length;i++){
                    for(int j=0;j<receiver[i].length;j++)
                        receiver[i][j].clear();
                } 
            }
            if(actor instanceof Track){
                ((Track)actor).cleanTrack();
                _inTransit.put(((IntToken)(((Track)actor).trackId.getToken())).intValue(), false);
            }
            if(actor instanceof Airport){
                ((Airport)actor)._airplanes.clear();
                ((Airport)actor)._inTransit=null;
            }
            if(actor instanceof DestinationAirport){
                ((DestinationAirport)actor).cleanDestinationAirport();
            }
        }
        underAnalysisMO.clear();
    }

    private void _fillModel(Snapshot state) throws IllegalActionException {
        // Don't change _eventQueue, only fill actors and ports, and modelTime
      
        for(Entry<Integer, Boolean> entry:state.regionsUnderAnalysis.entrySet())
        {
            regions.get(entry.getKey()-1).underAnalysis=entry.getValue();
            regions.get(entry.getKey()-1).aircraftHasArrived.clear();
        }
        
        for(int j=0;j<state.isAircraftArrived.size();j++) {
            String[] temp=state.isAircraftArrived.get(j).split(",");
            regions.get(Integer.valueOf(temp[0])-1).aircraftHasArrived.add(Integer.valueOf(temp[1]));
        }
        
        setModelTime(state._modelTime);
        _microstep=state._microstep;
        
        underAnalysisMO=new TreeMap<>();
        for(Entry<Integer,String> entry:state.underAnalysisMovingObjects.entrySet())
            underAnalysisMO.put(entry.getKey(), entry.getValue());
        
//        Object[] eventArray=state.eventQueue.toArray();
        int i=0;
        for(DEEvent object: state.eventQueue){
            NamedObj actor=(NamedObj)(object.actor());
            if(actor instanceof Track){
                _fillTrack(actor,state.trackActors.get(i));
            }
            if(actor instanceof Airport){
                _fillAirport(actor, state.airportActors.get(i));
            }
            if(actor instanceof DestinationAirport) {
                _fillDestinationAirport(actor, state.destinationAirportActors.get(i));
            }
            
            IOPort port=object.ioPort();
            if(port!=null){
                String name=object.actor().getFullName()+port.getFullName();
                _fillInputPort(object,state.inputTokens.get(name));
            }  
            i++;
        }
    }
    
    private void _fillTrack(NamedObj actor, TrackFields trackFields) throws IllegalActionException {
        // TODO Auto-generated method stub
        ((Track) actor).generator=trackFields.genMode;
        ((Track) actor)._called=trackFields.called;
        ((Track) actor)._inTransit=trackFields.inTransit;
        ((Track) actor)._OutRoute=trackFields.OutRoute;
        ((Track) actor)._transitExpires=trackFields.transitExpires;
        
        if(trackFields.genMode==true)
            if(trackFields.movingObjectsList.isEmpty())
                throw new IllegalActionException("Actor is in generating mode but is list is empay");
            else 
            {
                MovingObjectCell temp=trackFields.movingObjectsList.head;
                while(temp!=null) {
                    ((Track) actor).movingObjectsList.add(temp.movingObject, temp.timeOfDeparting, temp.departureDirection);
                    temp=temp.next;
                }
            }
        
        if(trackFields.inTransit!=null )
            _inTransit.put(((IntToken)((Track) actor).trackId.getToken()).intValue(), true);
        else{
            _inTransit.put(((IntToken)((Track) actor).trackId.getToken()).intValue(), false);
        }
    }

    @Override
    protected int _fire() throws IllegalActionException {
        // Find the next actor to be fired.
        //********************Added by Maryam

       
        if(_currentStateIndex==-1)
        {
            // Create initial state
            _currentStateIndex=0;
            _stateSpace.add(new Snapshot(-1, getEventQueue(), null, getModelTime(),_microstep,underAnalysisMO));
            _storeState(_currentStateIndex);
//            _timeReachableStates.add(_currentStateIndex);
            _timeReachableStates.add(_currentStateIndex, getModelTime());
            _currentTimedState= _stateSpace.get(_currentStateIndex);
            _timeReachableStatesMap.put(_currentTimedState, _currentStateIndex);
          
        }
        // If we reach this point and _eventQueue is not null but the eventQueue in currentStateIndex
        // is null, means that the _eventQueue has changed in between and we have to update the eventQueue of currentStateIndex,
        // we also need to storeState(_currentStateIndex)
        if(_stateSpace.get(_currentStateIndex).eventQueue.isEmpty() && !_eventQueue.isEmpty())
        {
            Object[] objectArray=getEventQueue().toArray();
            for(Object object: objectArray)
                _stateSpace.get(_currentStateIndex).eventQueue.add((DEEvent) object);
//                _stateSpace.get(_currentStateIndex).eventQueue=getEventQueue();
                _storeState(_currentStateIndex);
        }
        
        // If we have a time transition, this state will be the target state, otherwise,
        // this will be the state which is obtained by firing an actor.
        // The information of this state will be updated. 
        _stateSpace.add(new Snapshot(_currentStateIndex,null,null,getModelTime(),_microstep,null));
        _nextStateIndex=_stateSpace.size()-1; // For time _nextStateIndex for immediate, current
        //***********************
        
        Actor actorToFire = _getNextActorToFire();
        
        //********************Added by Maryam
        if(actorToFire==null){
//            System.out.println(_makeString(_currentStateIndex, _stateSpace.get(_currentStateIndex)));
            _stateSpace.remove(_nextStateIndex);
        }
        //***********************
        
        // Check whether the actor to be fired is null.
        // -- If the actor to be fired is null,
        // There are two conditions that the actor to be fired
        // can be null.
        if (actorToFire == null) {
            if (_isTopLevel()) {
                // Case 1:
                // If this director is an executive director at
                // the top level, a null actor means that there are
                // no events in the event queue.
                if (_debugging) {
                    _debug("No more events in the event queue.");
                }

                // Setting the following variable to true makes the
                // postfire method return false.
                // Do not do this if _stopFireRequested is true,
                // since there may in fact be actors to fire, but
                // their firing has been deferred.
                if (!_stopFireRequested) {
                    _noMoreActorsToFire = true;
                }
            } else {
                // Case 2:
                // If this director belongs to an opaque composite model,
                // which is not at the top level, the director may be
                // invoked by an update of an external parameter port.
                // Therefore, no actors contained by the composite model
                // need to be fired.
                // NOTE: There may still be events in the event queue
                // of this director that are scheduled for future firings.
                if (_debugging) {
                    _debug("No actor requests to be fired "
                            + "at the current tag.");
                }
            }
            // Nothing more needs to be done in the current iteration.
            // Simply return.
            // Since we are now actually stopping the firing, we can set this false.
            _stopFireRequested = false;
            return -1;
        }

        // NOTE: Here we used to check to see whether
        // the actor to be fired is the container of this director,
        // and if so, return to give the outside domain a chance to react
        // to that event. This strategy assumed that the
        // topological sort would always assign the composite actor the
        // lowest priority, which would guarantee that all the inside actors
        // have fired (reacted to their triggers) before the composite
        // actor is what is returned. However, the priority no longer
        // seems to always be lower. A better strategy is to continue
        // firing until we have exhausted all events with the current
        // tag and microstep.
        if (actorToFire == getContainer()) {
            /* What we used to do (before 5/17/09):
            // Since we are now actually stopping the firing, we can set this false.
            _stopFireRequested = false;
            return;
             */
            return 1;
        }

        if (_debugging) {
            _debug("****** Actor to fire: " + actorToFire.getFullName());
        }
        
        //********************Added by Maryam
        if(_stateSpace.get(_currentStateIndex)._modelTime.compareTo(getModelTime())<0){
            // Model time has been increased, and we have a timed transition.
            _stateSpace.get(_nextStateIndex)._modelTime=getModelTime();
            _stateSpace.get(_nextStateIndex)._microstep=_microstep;
            _copyState(_nextStateIndex,_stateSpace.get(_currentStateIndex));
            _currentStateIndex=_nextStateIndex;
            _stateSpace.add(new Snapshot(_currentStateIndex,null,null,getModelTime(),_microstep,null));
            _nextStateIndex=_stateSpace.size()-1;
        }
        
        _stateSpace.get(_nextStateIndex).name=actorToFire.getFullName();
        _stateSpace.get(_nextStateIndex)._microstep=_microstep;

        
        // Keep firing the actor to be fired until there are no more input
        // tokens available in any of its input ports with the same tag, or its prefire()
        // method returns false.
        boolean refire;

        do {
            refire = false;

            // NOTE: There are enough tests here against the
            // _debugging variable that it makes sense to split
            // into two duplicate versions.
            if (_debugging) {
                // Debugging. Report everything.
                // If the actor to be fired is not contained by the container,
                // it may just be deleted. Put this actor to the
                // list of disabled actors.
                if (!((CompositeEntity) getContainer())
                        .deepContains((NamedObj) actorToFire)) {
                    _debug("Actor no longer under the control of this director. Disabling actor.");
                    _disableActor(actorToFire);
                    break;
                }

                _debug(new FiringEvent(this, actorToFire,
                        FiringEvent.BEFORE_PREFIRE));

                if (!actorToFire.prefire()) {
                    _debug("*** Prefire returned false.");
                    break;
                }

                _debug(new FiringEvent(this, actorToFire,
                        FiringEvent.AFTER_PREFIRE));

                _debug(new FiringEvent(this, actorToFire,
                        FiringEvent.BEFORE_FIRE));

                actorToFire.fire();
                _debug(new FiringEvent(this, actorToFire,
                        FiringEvent.AFTER_FIRE));

                _debug(new FiringEvent(this, actorToFire,
                        FiringEvent.BEFORE_POSTFIRE));

                if (!actorToFire.postfire()) {
                    _debug("*** Postfire returned false:",
                            ((Nameable) actorToFire).getName());

                    // This actor requests not to be fired again.
                    _disableActor(actorToFire);
                    break;
                }

                _debug(new FiringEvent(this, actorToFire,
                        FiringEvent.AFTER_POSTFIRE));
            } else {
                // No debugging.
                // If the actor to be fired is not contained by the container,
                // it may just be deleted. Put this actor to the
                // list of disabled actors.
                if (!((CompositeEntity) getContainer())
                        .deepContains((NamedObj) actorToFire)) {
                    _disableActor(actorToFire);
                    break;
                }
                if (!actorToFire.prefire()) {
                    break;
                }

                actorToFire.fire();
                // NOTE: It is the fact that we postfire actors now that makes
                // this director not comply with the actor abstract semantics.
                // However, it's quite a redesign to make it comply, and the
                // semantics would not be backward compatible. It really needs
                // to be a new director to comply.
                if (!actorToFire.postfire()) {
                    // This actor requests not to be fired again.
                    _disableActor(actorToFire);
                    break;
                }

            }

            // Check all the input ports of the actor to see whether there
            // are more input tokens to be processed.
            // FIXME: This particular situation can only occur if either the
            // actor failed to consume a token, or multiple
            // events with the same destination were queued with the same tag.
            // In theory, both are errors. One possible fix for the latter
            // case would be to requeue the token with a larger microstep.
            // A possible fix for the former (if we can detect it) would
            // be to throw an exception. This would be far better than
            // going into an infinite loop.
            Iterator<?> inputPorts = actorToFire.inputPortList().iterator();

            while (inputPorts.hasNext() && !refire) {
                IOPort port = (IOPort) inputPorts.next();

                // iterate all the channels of the current input port.
                for (int i = 0; i < port.getWidth(); i++) {
                    if (port.hasNewToken(i)) {
                        if (_debugging) {
                            _debug("Port named " + port.getName()
                                    + " still has input on channel " + i
                                    + ". Refire the actor.");
                        }
                        // refire only if can be scheduled.
                        if (!_aspectsPresent || _schedule(
                                (NamedObj) actorToFire, getModelTime())) {
                            refire = true;

                            // Found a channel that has input data,
                            // jump out of the for loop.
                            break;
                        } else if (_aspectsPresent) {
                            if (_actorsInExecution == null) {
                                _actorsInExecution = new HashMap();
                            }
                            List<DEEvent> events = _actorsInExecution
                                    .get(actorToFire);
                            if (events == null) {
                                events = new ArrayList<DEEvent>();
                            }

                            events.add(new DEEvent(port, getModelTime(), 1,
                                    _getDepthOfActor(actorToFire)));
                            _actorsInExecution.put(actorToFire, events);
                        }
                    }
                }
            }
        } while (refire); // close the do {...} while () loop
        // NOTE: On the above, it would be nice to be able to
        // check _stopFireRequested, but this doesn't actually work.
        // In particular, firing an actor may trigger a call to stopFire(),
        // for example if the actor makes a change request, as for example
        // an FSM actor will do.  This will prevent subsequent firings,
        // incorrectly.
        
        //***************Added by Maryam
     

        int upToThis=_stateSpace.get(_currentStateIndex).upToThisTaken;
        
        for(int i=0;i<upToThis;i++)
            _eventQueue.put(_stateSpace.get(_currentStateIndex).eventQueue.get(i));
        
        Object[] objectArray=_eventQueue.toArray();
        for(Object object: objectArray)
            _stateSpace.get(_nextStateIndex).eventQueue.add((DEEvent) object);
        
        _storeState(_nextStateIndex);
        _currentStateIndex=_nextStateIndex;
        
        // Check whether the _currentStateIndex exists in tempStates

        check();
        
        if(tempStates.containsKey(_stateSpace.get(_currentStateIndex)))
            recentlyTimeStVisited=_currentStateIndex;
        else
            tempStates.put(_stateSpace.get(_currentStateIndex), _currentStateIndex);
        //***********************
        return 0;
    }
    
    
    private void check() throws IllegalActionException {
        // TODO Auto-generated method stub
        if(_stateSpace.get(_currentStateIndex).underAnalysisMovingObjects.containsKey(335)) {
            if(Integer.valueOf(_stateSpace.get(_currentStateIndex).underAnalysisMovingObjects.get(335).split(",")[0])==48)
            for(DEEvent o: _stateSpace.get(_currentStateIndex).eventQueue) {
                if(o.actor() instanceof Track) {
                    int trackId=((IntToken)(((Track)o.actor()).trackId.getToken())).intValue();
                    if(trackId==48) {
                      return;
                    }
                    }
                }

        }
        
    }

    private void _copyState(int _nextStateIndex, Snapshot snapshot) {
        // TODO Auto-generated method stub
        _stateSpace.get(_nextStateIndex).eventQueue=snapshot.eventQueue;
        _stateSpace.get(_nextStateIndex).airportActors=snapshot.airportActors;
        _stateSpace.get(_nextStateIndex).destinationAirportActors=snapshot.destinationAirportActors;
        _stateSpace.get(_nextStateIndex).inputTokens=snapshot.inputTokens;
        _stateSpace.get(_nextStateIndex).trackActors=snapshot.trackActors;
        _stateSpace.get(_nextStateIndex).regionsUnderAnalysis=snapshot.regionsUnderAnalysis;
        _stateSpace.get(_nextStateIndex).isAircraftArrived=snapshot.isAircraftArrived;
        _stateSpace.get(_nextStateIndex).underAnalysisMovingObjects=snapshot.underAnalysisMovingObjects;
    }

    private void _storeState(int _currentStateIndex) throws IllegalActionException {
        // TODO Auto-generated method stub
        
        int eventCounter=-1;
        for(DEEvent object: _stateSpace.get(_currentStateIndex).eventQueue){
            eventCounter++;
            NamedObj actor=(NamedObj) object.actor();
            //Store tokens on the ports
            IOPort port=object.ioPort();
            if(port!=null){
                Receiver[][] receiver=port.getReceivers();
                Map<Integer, Token> temp=new TreeMap<Integer, Token>();
                //For each channel, check existence of the token in it. 
                for(int i=0;i<port.getWidth();i++){
                    if(port.hasNewToken(i)){
                        Token token=((ATCReceiver)receiver[i][0]).getToken();
                        temp.put(i,token);
                    }
                }
                // For one event, we check all channels of the port.
                    _stateSpace.get(_currentStateIndex).inputTokens.put(actor.getFullName()+port.getFullName(), temp);
            }
            // End of storing tokens on the ports
            
            if(actor instanceof Airport){
                _stateSpace.get(_currentStateIndex).airportActors.put(eventCounter,new AirportFeilds(
                        ((Airport)actor)._airplanes,((Airport)actor)._inTransit, ((Airport)actor)._transitExpires));
            }
            else if(actor instanceof DestinationAirport) {
                _stateSpace.get(_currentStateIndex).destinationAirportActors.put(eventCounter, new DestinationAirportFields(((DestinationAirport)actor)._inTransit, ((DestinationAirport)actor)._transitExpires, ((DestinationAirport)actor)._called));
            }
            else if(actor instanceof  Track ){
                _stateSpace.get(_currentStateIndex).trackActors.put(eventCounter, new TrackFields(
                        ((Track)actor)._called, ((Track)actor)._inTransit, ((Track)actor)._OutRoute, ((Track)actor)._transitExpires,((Track)actor).generator, ((Track)actor).movingObjectsList));
            }
        }
     // Store status of regions
        for(int i=0;i<regions.size();i++) {
            _stateSpace.get(_currentStateIndex).regionsUnderAnalysis.put(i+1, regions.get(i).underAnalysis);
            for(int j=0;j<regions.get(i).aircraftHasArrived.size();j++)
                _stateSpace.get(_currentStateIndex).isAircraftArrived.add(i+1+","+regions.get(i).aircraftHasArrived.get(j));
        }
        
     //Store underAnalysisMovingObjects
      for(Entry<Integer, String> entry: underAnalysisMO.entrySet()) {
          _stateSpace.get(_currentStateIndex).underAnalysisMovingObjects.put(entry.getKey(),entry.getValue());
      }
        
    }

    /**
     * The route of each moving object in each region is stored as the route of the aircraft
     * in the region to the end. Please attend that the id of the regions are removed from a route. 
     * This function also stores the track from which the aircraft enters into each region, and
     * also the track to which the aircraft arrives when it departs from this region. This also stores
     * some information about how many aircraft entor into a region from a given region.
     * @throws IOException
     * @throws IllegalActionException
     */
    
    // Assume that a route is {1,(2,1.5),(3,2.5);2,(4,3.5),(5,4.5)}. 
    // In this route, 1 and 2 show the region's id, and each tuple shows a track and arrival time 
    // at the track. 
    private void _readFromFile() throws IOException, IllegalActionException {
        
        int iteration=0;
        String fileName="";
        Director executiveDirector = ((CompositeActor) getContainer())
                .getExecutiveDirector();
        if(executiveDirector instanceof SDFDirector) {
            iteration=((SDFDirector)executiveDirector)._iterationCount;
            fileName="input"+iterationCount(iteration+1)+".txt";
        }else if(((IntToken)indexOfFile.getToken()).intValue()==0) {
            fileName="input.txt";
        }
        else {
            fileName="input"+((IntToken)indexOfFile.getToken()).intValue()+".txt"; 
        }
        
        BufferedReader bufferedReader =  new BufferedReader(new

                FileReader(fileName));
        String line="";
        int counter=0;
        while((line=bufferedReader.readLine())!=null) {
            counter++;
            int prevRegion=-1;
            String [] parts=line.split(";"); // each region is separated with ;
            for(int i=0;i<parts.length;i++) {
                
                if(i==0) {
                    double time=Double.valueOf(parts[i].split(",")[1].split("/")[1].substring(0,
                            parts[i].split(",")[1].split("/")[1].length()-1));
                    arrivalTimeToNet.put(counter, time);
                }
                int entranceTrack;
                int exitTrack;
                if(i==0) 
                    entranceTrack=-1;
                else
                {
                    String[] pre=parts[i-1].split(",");
                    String x=pre[pre.length-1];
                    x=x.substring(1, x.length()-1);// for (1,2.5), this gives 1,2.5
                    entranceTrack=Integer.valueOf(x.split("/")[0]);
                }
                
                if(i==parts.length-1) {
                    exitTrack=-1;
                }
                else
                {
                    String[] pre=parts[i+1].split(",");
                    String x=pre[1];
                    x=x.substring(1, x.length()-1);// for (1/2.5), this gives 1/2.5
                    exitTrack=Integer.valueOf(x.split("/")[0]);
                }
                String [] tracks=parts[i].split(","); // tracks in a route are separated with ,
                int regionId=0;
             
                if(tracks[0].length()!=1 && tracks[0].substring(0,1).equals("{"))
                    regionId=Integer.valueOf(tracks[0].substring(1));
                else
                    regionId=Integer.valueOf(tracks[0].substring(0));
                // To store the route from the start of this region
                String travelingMap="";
                for(int j=i;j<parts.length;j++) {
                    String temp="";
                    if(j==parts.length-1)
                        temp=parts[j].substring(parts[j].split(",")[0].length()+1,parts[j].length()-1);
                    else
                        temp=parts[j].substring(parts[j].split(",")[0].length()+1);
                    travelingMap+=temp;
                    if(j!=parts.length-1)
                        travelingMap+=";";
                }
                
                regions.get(regionId-1).travelingAicrafts.put(counter, travelingMap);
//                regions.get(regionId-1).aircraftHasArrived.put(counter, false);
                if(exitTrack!=-1)
                    regions.get(regionId-1).departureTracks.put(counter, exitTrack);
                if(entranceTrack!=-1)
                    regions.get(regionId-1).entryTracks.put(counter, entranceTrack);
                if(prevRegion!=-1 && !regions.get(regionId-1).numInputfrom.containsKey(prevRegion))
                    regions.get(regionId-1).numInputfrom.put(prevRegion,1);
                else if(regions.get(regionId-1).numInputfrom.containsKey(prevRegion))
                    regions.get(regionId-1).numInputfrom.put(prevRegion,
                            regions.get(regionId-1).numInputfrom.get(prevRegion)+1);
                prevRegion=regionId;
            }
        }
    }
    
    /**
     * If the track parameter is not null, find a new route for its aircraft and fill
     * its information with the new aircraft.
     * @param id, region id
     * @param time
     * @param dir: The direction of propagation.The dir for the backward propagation is true and for the 
     * forward propagation is false. Also, to load information of a region, when the model is started, the dir is false.
     * @throws IllegalActionException
     * @throws NumberFormatException
     * @throws IllegalAccessException
     */
    @SuppressWarnings({ "unused", "deprecation" })
    private void _loadInformationOfRegion(int id, double time, Track affectedTrack,boolean dir) throws IllegalActionException, NumberFormatException, IllegalAccessException {
        //****** You use traveling map for each region, check what happens when an aircraft
        // travel from a region to another. Also check, when the region under analysis is set
       
        int desRegionId=id-1; // For backward propagation, this is the region of the rejected moving object
        // For forward propagation, this is destination of the rejected moving object.
        int sourceRegionId=-1;
        regions.get(desRegionId).underAnalysis=true;
        int travelingAirInAffTrack=-1;
        if(affectedTrack!=null && dir==false) { // For forward, affectedTrack is the track whose message is rejected.
            travelingAirInAffTrack=-1;
            sourceRegionId=((IntToken)affectedTrack.regionId).intValue();
        }
        if(affectedTrack!=null && dir==true) {
            // For backward, affectedTrack is the track whose message is rejected
            travelingAirInAffTrack=((IntToken)((RecordToken)affectedTrack._inTransit).get("aircraftId")).intValue();
            sourceRegionId=id; // for backward, source and destination are the same.
        }
        
        for(Entry<Integer, Track> entry: regions.get(desRegionId).tracks.entrySet()) {
            if(travelingAirInAffTrack==-1 || ((IntToken)entry.getValue().trackId.getToken()).intValue()!= ((IntToken)affectedTrack.trackId.getToken()).intValue()) {             
                entry.getValue().cleanTrack();
            }
        }
        
        for(Entry<Integer, String> entry:regions.get(desRegionId).travelingAicrafts.entrySet())
        {
   
           
            int aircraftId=entry.getKey();
            if(aircraftId==151)
                System.out.println("joy");
            
            if(aircraftId==travelingAirInAffTrack)
                continue;
            
            // It is possible that a moving object is rerouted and arrives at a region from another region,
            // so the initial information shows that this moving object will arrive from a region which is not 
            // under analysis, while the moving object will arrive from an under analysis region.
            // Another situation is when the moving object is in region 1, and it has to pass from region 2, and
            // then region 3, but the change has already been propagated into region 3. So we should not fill the 
            // the region 3 for this moving object, because it will arrive later. 
            // I think this is correct with this assumption that there is not unaffected region between 1 an 3???. 
//            if(underAnalysisMO.containsKey(entry.getKey()))
//                continue;
            
            if(checkUnderAnalysisMo(entry,time)) {
                continue;
            }
            
            
            boolean ifArrived=regions.get(desRegionId).aircraftHasArrived.contains(aircraftId);
            int trackId=-1;
            Track track=null;
            if(regions.get(desRegionId).entryTracks.containsKey(aircraftId)) {
                trackId=regions.get(desRegionId).entryTracks.get(aircraftId); //entryTrackId
                track=getTrackInRegion(trackId,regions.get(desRegionId)); // Find the entry track in neighbors of desRegionId
            }
            // If the aircraft comes from an under analysis region and it has not 
            // arrived at its previous time yet (has not marked yet), then it will arrive later
            // and you do not need to fill the desRegionId for this.
            if(track!=null && sourceRegionId!=-1 && !ifArrived && regions.get(((IntToken)track.regionId).intValue()-1).underAnalysis==true)
                continue;
            // else, either aircraft has arrived at its time or comes from an unaffected area

           String[] travelingMap=entry.getValue().split(";")[0].split(",");
           double arrivalTimeOfAircraft=Double.valueOf(travelingMap[0].substring(1, travelingMap[0].length()-1).split("/")[1]);
           
           
           if(ifArrived==true && time==arrivalTimeOfAircraft) {
               //If aircraft has arrived at its time and its time is equal to the change
               // propagation time, then you have to fire its departure track, the track
               // who will send this aircraft to the destination region (that is track variable)
               int departureTrackId=Integer.valueOf(travelingMap[0].substring(1, travelingMap[0].length()-1).split("/")[0]);
               Track departureTrack=regions.get(desRegionId)._containsTrack(regions.get(desRegionId).tracks, departureTrackId);
               
               _callSendForTrack(track,entry.getKey(),entry.getValue(),departureTrackId,time-arrivalTimeToNet.get(aircraftId));
               departureTrack._called=true;
//               if(aircraftId==700)
//                   System.out.println("Here");
               String value=String.valueOf(departureTrackId)+","+time;
               underAnalysisMO.put(aircraftId, value);
               continue;
               
           }
           
           // When we reach this point, the aircraft either has arrived at its time but  before change propagation, or
           // it arrives from an unaffected area. 
            int prevTrack=-1;
            for(int i=0;i<travelingMap.length;i++) {
                String temp=travelingMap[i];
                temp=temp.substring(1, temp.length()-1);
                String[] temp2=temp.split("/");
                if(time<=Double.valueOf(temp2[1]) && prevTrack==-1 && track!=null) { 
                  // This only happens when aircraft comes from an unaffected area.
                  //This aircraft will arrive later, so fill the actor to generate input for
                  // this region.
                    String value=String.valueOf(((IntToken)track._id).intValue())+","+temp2[1];

                    underAnalysisMO.put(aircraftId, value);
                    Map<String, Token> aircraft = new TreeMap<String, Token>();
                    aircraft.put("aircraftId", new IntToken(aircraftId));
                    aircraft.put("aircraftSpeed", new IntToken(200));
                    aircraft.put("flightMap", new StringToken(entry.getValue()));
                    aircraft.put("priorTrack", new IntToken(trackId));
                    
                    double fuel=Double.valueOf(temp2[1])-arrivalTimeToNet.get(aircraftId);
                    
                    if(dimension==15)
                        aircraft.put("fuel", new DoubleToken(325-fuel));
                    else if (dimension==12)
                        aircraft.put("fuel", new DoubleToken(244-fuel));
                    else if(dimension==9)
                        aircraft.put("fuel", new DoubleToken(200-fuel));
                    else
                        aircraft.put("fuel", new DoubleToken(500-fuel));
                    
                    
                    // Whether the track belongs to the north neighbors
                    if(((BooleanToken)((ArrayToken)track.border.getToken()).contains(new StringToken("N"+id))).booleanValue())
                    {
                        track.generator=true;
                        track.putMovingObject(new RecordToken(aircraft),"South",Double.valueOf(temp2[1]));
                        _inputGenerator.put(((IntToken)track.trackId.getToken()).intValue(), track);
                    }
                    else if(((BooleanToken)((ArrayToken)track.border.getToken()).contains(new StringToken("W"+id))).booleanValue())
                    {
                        track.generator=true;
                        track.putMovingObject(new RecordToken(aircraft),"East",Double.valueOf(temp2[1]));
                        _inputGenerator.put(((IntToken)track.trackId.getToken()).intValue(), track);
                    }
                    else if(((BooleanToken)((ArrayToken)track.border.getToken()).contains(new StringToken("E"+id))).booleanValue())
                    {
                        track.generator=true;
                        track.putMovingObject(new RecordToken(aircraft),"West",Double.valueOf(temp2[1]));
                        _inputGenerator.put(((IntToken)track.trackId.getToken()).intValue(), track);
                    }
                    else if(((BooleanToken)((ArrayToken)track.border.getToken()).contains(new StringToken("S"+id))).booleanValue())
                    {
                        track.generator=true;
                        track.putMovingObject(new RecordToken(aircraft),"North",Double.valueOf(temp2[1]));
                        _inputGenerator.put(((IntToken)track.trackId.getToken()).intValue(), track);
                    }
                    break;
                }
                else if(time<=Double.valueOf(temp2[1]) && prevTrack==-1 && track==null)
                {
                    //Fill source airport 
                    track=regions.get(desRegionId)._containsTrack(regions.get(desRegionId).tracks,Integer.valueOf(temp2[0]));
                    System.out.println(entry.getKey()+entry.getValue());
                    Airport airport=_airportsId.get(((IntToken)track.connectedSourceA.getToken()).intValue());
                    Map<String, Token> aircraft = new TreeMap<String, Token>();
                    aircraft.put("aircraftId", new IntToken(aircraftId));
                    aircraft.put("aircraftSpeed", new IntToken(200));
                    aircraft.put("flightMap", new StringToken(entry.getValue()));
                    aircraft.put("priorTrack", new IntToken(trackId));
                    if(dimension==15)
                        aircraft.put("fuel", new DoubleToken(325));
                    else if (dimension==12)
                        aircraft.put("fuel", new DoubleToken(244));
                    else if(dimension==9)
                        aircraft.put("fuel", new DoubleToken(200));
                    else
                        aircraft.put("fuel", new DoubleToken(500));
                    _LoadAirport(airport,Double.valueOf(temp2[1]),new RecordToken(aircraft));

                    String value=String.valueOf(((IntToken)airport.airportId.getToken()).intValue())+","+temp2[1];
                    underAnalysisMO.put(aircraftId, value);
                    break;
                }
                else if(time<=Double.valueOf(temp2[1])) {
                    // This can happen for both cases
                    
                    if(ifArrived && time==Double.valueOf(temp2[1])) {
                        
                        if((track=regions.get(desRegionId)._containsTrack(regions.get(desRegionId).tracks,prevTrack))!=null) {
                            fireAt(track,Double.valueOf(temp2[1]));
                            int count=0;
                            for(int j=0;j<i-1;j++) {
                                count+=travelingMap[j].length();
                                count+=1;
                            }
                            
                            double fuel=Double.valueOf(temp2[1])-arrivalTimeToNet.get(aircraftId);
//                            if(aircraftId==700)
//                                System.out.println("Here");
                            String value=String.valueOf(((IntToken)track._id).intValue())+","+temp2[1];
                            underAnalysisMO.put(aircraftId, value);
                            _loadTrack(aircraftId,track,Double.valueOf(temp2[1]),entry.getValue().substring(count),fuel);
                        }
                        else 
                            throw new IllegalActionException("The actor "+Integer.valueOf(temp2[0])+"does not belong to the region "+desRegionId);
                        

                        
                        break;
                    }
                    if((track=regions.get(desRegionId)._containsTrack(regions.get(desRegionId).tracks,prevTrack))!=null) {
                        fireAt(track,Double.valueOf(temp2[1]));
                        int count=0;
                        for(int j=0;j<i-1;j++) {
                            count+=travelingMap[j].length();
                            count+=1;
                        }
                        
                        double fuel=Double.valueOf(temp2[1])-arrivalTimeToNet.get(aircraftId);

                        String value=String.valueOf(((IntToken)track._id).intValue())+","+temp2[1];
                        underAnalysisMO.put(aircraftId, value);
                        _loadTrack(aircraftId,track,Double.valueOf(temp2[1]),entry.getValue().substring(count),fuel);
                    }
                    else 
                        throw new IllegalActionException("The actor "+Integer.valueOf(temp2[0])+"does not belong to the region "+desRegionId);
                    break;
                }
                else if(time>Double.valueOf(temp2[1]) && i==(travelingMap.length-1) && time<=Double.valueOf(temp2[1])+1.0 && entry.getValue().split(";").length!=1){

                    if((track=regions.get(desRegionId)._containsTrack(regions.get(desRegionId).tracks,Integer.valueOf(temp2[0])))!=null) {
                       //We reach this point if the moving object is not under analysis and ( next region is under analysis, or the moving
                       // object has not arrived at this region or its time less than the departure time, or the
                        // information has not been loaded.)
                        // if the moving object has been sent successfully, it is under analysis.
                        
                        // If information has not been loaded, we are filling the region affected by storm
                        // Otherwise, we are filling the region affected by the change propagation. 
                        // This case shows the change is propagated backward. In this situation
                        // if mObjectListHead is empty or it has a different value from entry.getKey, we have sent the moving object at this time, so you don't
                        // need to load information of this track. Otherwise, you need to load. 
//                        MovingObjectCell objectCell=null;
//                        if(!track.movingObjectsList.isEmpty())
//                            objectCell=track.movingObjectsList.get();
//                        if(_informationLoaded==true && (track.mObjectListHead==null || ((IntToken)(((RecordToken)track.mObjectListHead.movingObject).get("aircraftId"))).intValue()!=aircraftId))
//                            break;
//                        else {
                            // If information hasn't been loaded, or mObjectListhead is not null and is equal to the current aircraft
                            // or for forward change propagation, when the aircraft has not left the track yet
                            fireAt(track,Double.valueOf(temp2[1])+1.0);
                            int count=0;
                            for(int j=0;j<i;j++) {
                                count+=travelingMap[j].length();
                                count+=1;
                            }

                            double x=Double.valueOf(temp2[1])+1.0;
                            String value=String.valueOf(((IntToken)track._id).intValue())+","+x;
                            underAnalysisMO.put(aircraftId, value);
                            double fuel=Double.valueOf(temp2[1])+1.0-arrivalTimeToNet.get(aircraftId);
                            _loadTrack(aircraftId, track, Double.valueOf(temp2[1])+1.0, entry.getValue().substring(count),fuel);
                        
//                        }
                    }
                }
                else if(time>Double.valueOf(temp2[1]) && i==(travelingMap.length-1) && time<=Double.valueOf(temp2[1])+1.0 && entry.getValue().split(";").length==1){
                    // This is destination airport
                    if(ifArrived && time==Double.valueOf(temp2[1])+1.0) {
                        break;
                    }
                    int count=0;
                    for(int j=0;j<i;j++) {
                        count+=travelingMap[j].length();
                        count+=1;
                    }    
                    
                    double fuel=Double.valueOf(temp2[1])+1.0-arrivalTimeToNet.get(aircraftId);
                    _loadDestinationAirport(aircraftId, Integer.valueOf(temp2[0]), Double.valueOf(temp2[1])+1.0, entry.getValue().substring(count),fuel);
//                    if(aircraftId==700)
//                        System.out.println("Here");
                    double x=Double.valueOf(temp2[1])+1.0;
                    String value=temp2[0]+","+(x);
                    underAnalysisMO.put(aircraftId, value);
                }
                else if(time>Double.valueOf(temp2[1]) && i==(travelingMap.length-1) && time>Double.valueOf(temp2[1])+1.0 && entry.getValue().split(";").length!=1){
                    // It means that the aircraft has arrived at its predetermined time at its next region
                    // or it has departs from the current region at is departure time.
                    int departureTrackId=regions.get(desRegionId).departureTracks.get(entry.getKey());
                    int nextRegionId=findRegionofTrack(departureTrackId); // This only works for n*n networks.
                    setInRegionAirArrived(entry.getKey(),nextRegionId);
                    break;
                }
                else if(time>Double.valueOf(temp2[1])){
                    // This can happen for both cases
                    prevTrack=Integer.valueOf(temp2[0]);
                }
                
            }
        }
        
        if(travelingAirInAffTrack!=-1 && dir==true) { // for backward change propagation.
            
            ArrayList<Integer> transits=findAndCheckNeigh(affectedTrack,time);
            
            if(transits==null) {
                fireAt(affectedTrack, affectedTrack._transitExpires);
            }
            else {
                for(int i=0;i<transits.size();i++)
                    if(transits.get(i)!=-1)
                    _inTransit.put(transits.get(i), true);
              //Route of the aircraft in the region affected by the change propagation for input change propagation.
              String temp=((StringToken)((RecordToken)affectedTrack._inTransit).get("flightMap")).toString();
              // Find a new route from prior track to the last track in the next region
              
              Map<String, Token> routeInRegionId=rerouteUnacceptedAircraft(affectedTrack._inTransit,sourceRegionId);
              // When you change the route of aircraft in the region affected by the change, load track with this new information
      
              Map<String, Token> newAircraft = new TreeMap<String, Token>();
              Token plane=affectedTrack._inTransit;
              newAircraft.put("aircraftId",((RecordToken)plane).get("aircraftId"));
              newAircraft.put("aircraftSpeed",((RecordToken)plane).get("aircraftSpeed"));
              newAircraft.put("flightMap", routeInRegionId.get("flightMap"));
              newAircraft.put("priorTrack",((RecordToken) plane).get("priorTrack"));
              newAircraft.put("fuel",((RecordToken)plane).get("fuel"));
              affectedTrack._inTransit=createNewMObject(new RecordToken(newAircraft), ((DoubleToken)routeInRegionId.get("delay")).doubleValue());
              affectedTrack._called=true;
              affectedTrack.generator=false;
              
              if(((IntToken)routeInRegionId.get("route")).intValue()!=-1) // It finds a new route
                  affectedTrack._OutRoute=((IntToken)routeInRegionId.get("route")).intValue();
              
              // if route==-1, it could not find a new route and should flight based on its previous route
              affectedTrack._transitExpires=affectedTrack._transitExpires.add(((DoubleToken)routeInRegionId.get("delay")).doubleValue());

              // And fireAT the director to fire the track at the new time
                  fireAt(affectedTrack, affectedTrack._transitExpires);
                  setInTransitStatusOfTrack(affectedTrack.trackId.getToken(), true);
//              }
              // If the change is propagated forward, the aircraft in affected track is rerouted itself.
              
              // The route of the aircraft in destination region might be changed.
              // For backward change propagation, when the route is changed, the route in
              // the destination region is also set (the destination is already under analysis).
              
              // For forward change propagation, by rejecting the aircraft, it is rerouted
              // and its new route contains route in destination region.
              for(int i=0;i<transits.size();i++)
                  if(transits.get(i)!=-1)
                  _inTransit.put(transits.get(i), false);
                
            }
            
//            
            
        }
        
        for(Entry<Integer,Track> entry: _inputGenerator.entrySet()) {
            entry.getValue().startGeneratingInput();
        }
        _inputGenerator.clear();
    }

    /**
     * return null if the aircraft cannot be rerouted
     * 
     * @param affectedTrack
     * @return 
     * @throws IllegalActionException 
     * @throws NumberFormatException 
     */
    private ArrayList<Integer> findAndCheckNeigh(Track affectedTrack, double time) throws NumberFormatException, IllegalActionException {
        // TODO Auto-generated method stub
        
        //if flag is false, return null
        //if flag is true and transit array is null, return transit with -1 entry
        //otherwise return transit.
        boolean flag=true; // This is true if the aircraft can be rerouted
        int regionId=((IntToken)affectedTrack.regionId).intValue()-1;
        ArrayToken neighbors= (ArrayToken)affectedTrack.neighbors;
        int[] neighborsId=new int[3];
        int[] neighborsRegions=new int[3];
        
        for (int i = 0; i < neighbors.length(); i++) {
            int id=((IntToken)neighbors.getElement(i)).intValue();
            int regionOfNeigh=-1;
            if(id!=-1 && !_airportsId.containsKey(id))
                regionOfNeigh=findRegionofTrack(id)-1;
            neighborsId[i]=id;
            neighborsRegions[i]=regionOfNeigh;
        }
        
        ArrayList<Integer> transit=new ArrayList<>();
        for( Entry<Integer,String> entry: regions.get(regionId).travelingAicrafts.entrySet()) {
            String flightMap=entry.getValue().split(";")[0];
            
            for (int i = 0; i < 3; i++) {
                int id=neighborsId[i];
                int regionOfNeigh=neighborsRegions[i];
                
                // if this a boundary actor in the same region as the affectedTrack

                if(regionOfNeigh!=-1 && regions.get(regionOfNeigh).tracks.get(id).border.getToken()!=null && regionOfNeigh==regionId) {
                    //if this neighbor has a token to send it out at the given time
                    String check="("+id+"/"+(time-1)+")";
                    if(flightMap.contains(check)) {
                        flag=false;
                        break;
                    }
                    // if this neighbor receive a token at the same time
                    check="("+id+"/"+time+")";
                    if(flightMap.contains(check)) {
                        flag=false;
                        break;
                    }
                    // if it does not have token
                    check="("+id+"/";
                    if(!flightMap.contains(check)) {
                        flag=true;
                    }
                    
                    //if it has token but send it later
                    else if(flightMap.contains(check)) {
                        //set intransit                        
                        double arrivalTime=Double.valueOf(flightMap.substring(flightMap.indexOf(check)+check.length()).split("\\)")[0]);
                        if(arrivalTime<time && time<arrivalTime+1) {
                            transit.add(id);
                            flag=true;
                        }
                    }
                }
                
                
            }
            if(flag==false)
                break; 
        }
        
        
        if(flag==true) // the aircraft can be rerouted here
        {
            if(transit.isEmpty()) {
                transit.add(-1);
                return transit;
            }
            else
                return transit;
        }
        else 
            return null;
        
    }

    private boolean checkUnderAnalysisMo(Entry<Integer, String> entry,double time) throws IllegalActionException, IllegalAccessException {
        // TODO Auto-generated method stub
        
        int aircraftId=entry.getKey();
//        if(aircraftId==335)
//            System.out.println("joy");
        String flightMap=entry.getValue();
        String firstEntry=flightMap.replace(";", ",").split(",")[0].split("/")[1];
        double arrivalTime=Double.valueOf(firstEntry.substring(0, firstEntry.length()-1));
        
        String lastEntry=flightMap.split(";")[0].split(",")[flightMap.split(";")[0].split(",").length-1].split("/")[1];
        double departureTime=Double.valueOf(lastEntry.substring(0, lastEntry.length()-1))+1;
        //if the moving object is under analysis
        if(underAnalysisMO.containsKey(aircraftId)) {
            int trackId=Integer.valueOf(underAnalysisMO.get(aircraftId).split(",")[0]);
            //System.out.println(underAnalysisMO.get(aircraftId).split(",")[1]+" "+_currentStateIndex+" "+aircraftId);
            double timeOfAdding=Double.valueOf(underAnalysisMO.get(aircraftId).split(",")[1]);
            
            // time of entering to the analysis is equal to the change propagation time
            if(time==timeOfAdding) {
                //for less than, it may arrive later
                //for equal part, it will be sent.
                if(time<=arrivalTime)
                    return true;
                //has been sent
                if(time>=departureTime)
                    return true;
            }
            
            if(timeOfAdding<time)
                return true;
            if(timeOfAdding>time && arrivalTime<=timeOfAdding && departureTime<timeOfAdding
                    && time<=departureTime) {
                //find the track and remove this aircraft from it.
                //remove aircraft from under analysis, and return false
                if(!_airportsId.containsKey(trackId)) {
                    int regionOfTrack=findRegionofTrack(trackId)-1;
                    Track track=null;
                    if((track=regions.get(regionOfTrack).tracks.get(trackId))!=null) {
                        if(((ArrayToken) ((Track)track).border.getToken())!=null)
                        {
                            
                            //track is a boundary actor
                            if(track.generator==true)
                                track.removeFromList(aircraftId);
                            else
                                throw new IllegalActionException("track is not generator "+_currentStateIndex+" "+aircraftId);
                            removeFromUnderAnalysis(aircraftId);
                            return false;
                        }
                    }
                    else throw new IllegalActionException("track is empty");
                }
                
            }
            return true;
        }
        
        return false;
    }

    @SuppressWarnings("deprecation")
    private void _loadDestinationAirport(int aircraftId, Integer airportId, double time, String flightMap,double fuel) {
        // TODO Auto-generated method stub
        
        try {
        _desAirports.get(airportId)._called=true;
        _desAirports.get(airportId)._transitExpires=new Time(this,time);

        Map<String, Token> aircraft = new TreeMap<String, Token>();
        aircraft.put("aircraftId", new IntToken(aircraftId));
        aircraft.put("aircraftSpeed", new IntToken(200));
        aircraft.put("flightMap", new StringToken(flightMap));
        aircraft.put("priorTrack", new IntToken(0));
        if(dimension==15)
            aircraft.put("fuel", new DoubleToken(325-fuel));
        else if (dimension==12)
            aircraft.put("fuel", new DoubleToken(244-fuel));
        else if(dimension==9)
            aircraft.put("fuel", new DoubleToken(200-fuel));
        else
            aircraft.put("fuel", new DoubleToken(500-fuel));
        _desAirports.get(airportId)._inTransit=new RecordToken(aircraft);
        fireAt(_desAirports.get(airportId), time);
        } catch (IllegalActionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }

    private void _LoadAirport(Airport airport, Double time, RecordToken aircraft) {
        // TODO Auto-generated method stub
        Map<String, Token> airplane = new TreeMap<String, Token>();
        airplane.put("aircraftId", ((RecordToken)aircraft).get("aircraftId"));
        airplane.put("aircraftSpeed", ((RecordToken)aircraft).get("aircraftSpeed"));
        airplane.put("flightMap", ((RecordToken)aircraft).get("flightMap"));
        airplane.put("priorTrack",((RecordToken)aircraft).get("priorTrack"));
        airplane.put("fuel", ((RecordToken)aircraft).get("fuel"));
        airplane.put("departureTime", new DoubleToken(time));
        airport.putMovingObject(airplane);
        
    }

    /**
     * This function finds the given trackId in its neighbors and returns it track.
     * @param trackId
     * @param region
     * @return
     */
    private Track getTrackInRegion(int trackId, Region region) {
        // TODO Auto-generated method stub
        Track track;
        if((track=region._containsTrack(region.northNeighbors,trackId))!=null) {
            return track;
        }
        else if((track=region._containsTrack(region.eastNeighbors,trackId))!=null) {
            return track;
        }
        else if((track=region._containsTrack(region.southNeighbors,trackId))!=null) {
            return track;
        }
        else if((track=region._containsTrack(region.westNeighbors,trackId))!=null) {
            return track;
        }
            
        return null;
    }

    /**
     * It create an inTransit for the track and calls its send function to send the inTransit to track
     * with the id departureTrackId. This send is accepted.
     * @param track
     * @param aircraftId
     * @param aircraftMap
     * @param departureTrackId
     */
    private void _callSendForTrack(Track track, Integer aircraftId, String aircraftMap, int departureTrackId, double fuel) {
        // TODO Auto-generated method stub
        
        try {
            ArrayToken neighborsOfTrack = _neighbors.get(((IntToken)track.trackId.getToken()).intValue());
            
            Map<String, Token> aircraft = new TreeMap<String, Token>();
            aircraft.put("aircraftId", new IntToken(aircraftId));
            aircraft.put("aircraftSpeed", new IntToken(200));
            aircraft.put("flightMap", new StringToken(aircraftMap));
            aircraft.put("priorTrack", new IntToken(((IntToken)track.trackId.getToken()).intValue()));
            if(dimension==15)
                aircraft.put("fuel", new DoubleToken(325-fuel));
            else if (dimension==12)
                aircraft.put("fuel", new DoubleToken(244-fuel));
            else if(dimension==9)
                aircraft.put("fuel", new DoubleToken(200-fuel));
            else
                aircraft.put("fuel", new DoubleToken(500-fuel));
            Token inTransit;
            
                inTransit = new RecordToken(aircraft);
    
            if(((IntToken)neighborsOfTrack.getElement(0)).intValue()==departureTrackId) {
                track.northOutput.send(0, inTransit);
                return;
            }
            if(((IntToken)neighborsOfTrack.getElement(1)).intValue()==departureTrackId) {
                track.eastOutput.send(0, inTransit);
                return;
            }
            if(((IntToken)neighborsOfTrack.getElement(2)).intValue()==departureTrackId) {
                track.southOutput.send(0, inTransit);
                return;
            }
        } catch (IllegalActionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /**
     * Load information of a track. The aircraft with the given traveling map is routed, and 
     * the a new route (which is the result of routing function) is set as the route of the moving object
     * in the track.
     * @param aircraftId
     * @param track
     * @param time
     * @param travelingMap
     * @throws IllegalActionException
     */
    private void _loadTrack(int aircraftId, Track track, Double time,String travelingMap, double fuel) throws IllegalActionException {
        // TODO Auto-generated method stub
        
        track._called=true;
        track.generator=false;
        
        Map<String, Token> aircraft = new TreeMap<String, Token>();
        aircraft.put("aircraftId", new IntToken(aircraftId));
        aircraft.put("aircraftSpeed", new IntToken(200));
        aircraft.put("flightMap", new StringToken(travelingMap));
        aircraft.put("priorTrack", new IntToken(0));
        if(dimension==15)
            aircraft.put("fuel", new DoubleToken(325-fuel));
        else if (dimension==12)
            aircraft.put("fuel", new DoubleToken(244-fuel));
        else if(dimension==9)
            aircraft.put("fuel", new DoubleToken(200-fuel));
        else
            aircraft.put("fuel", new DoubleToken(500-fuel));
        
        RecordToken aircraftWithInformation=routing(new RecordToken(aircraft), ((IntToken)track.trackId.getToken()));
        track._OutRoute=((IntToken) aircraftWithInformation.get("route")).intValue();
        
        Map<String, Token> newAircraft = new TreeMap<String, Token>();
        newAircraft.put("aircraftId",
                aircraftWithInformation.get("aircraftId"));
        newAircraft.put("aircraftSpeed",
                aircraftWithInformation.get("aircraftSpeed"));
        newAircraft.put("flightMap",
                aircraftWithInformation.get("flightMap"));
        newAircraft.put("priorTrack",
                aircraftWithInformation.get("priorTrack"));

        newAircraft.put("fuel", aircraftWithInformation.get("fuel"));
        track._inTransit= new RecordToken(newAircraft);
        track._transitExpires=new Time(this,time);
        track.movingObjectsList.clear();
        setInTransitStatusOfTrack(track.trackId.getToken(),true);
    }

    private ArrayList<Snapshot> _stateSpace;
 //   private Queue<Integer> _timeReachableStates;
    private TimedStatesList _timeReachableStates;
    private HashMap<Snapshot, Integer> _timeReachableStatesMap;
    private Snapshot _currentTimedState;
    private int _currentStateIndex;
    private int _nextStateIndex;
    private boolean _stopAnalysis;
    
    int numOfRegions;
    /**
     * regions. This variable contains tracks of each region.
     */
    private ArrayList<Region> regions;
    
    // true if the information has been loaded.
    private boolean _informationLoaded;
    
    // Store the tracks which should generate inputs for the regions.
    private Map<Integer,Track> _inputGenerator;
    
    private int recentlyTimeStVisited=-1;
    
    // To store states generated between two timed transitions
    private HashMap<Snapshot, Integer> tempStates;
    
    private long startTime; 
    
    /**The dimension of the network which is n.For ins, the dimention of a 15*15 network is 15. */
    private int dimension;
    
    /**The dimension of a region: number of the regions are (n/regionDimension)*(n/regionDimension). */
    private int regionDimension;
    
    /**When a moving object is involved in analysis, we add its id to this array.
     * The key is id of the moving object and the value is "trackId+time" that shows the moving object 
     * at which time and which track is getting under analysis */
    private Map<Integer, String> underAnalysisMO;
    
    /**
     * True if a deadlock (livelock) happens in the model. The deadlock happens when at least a moving object runs out of fuel.  
     */
    private boolean deadlockDetected;
    
    /**
     * If the execution time goes more than one houre. 
     */
    private boolean timeOver;
    
    /**
     * Arrival time to the traffic network for each aircraft
     */
    private Map<Integer, Double> arrivalTimeToNet;

    /**
     * When the track as a generator is not able to generate a message
     * at a pre-specified time for the region.
     * @param track
     * @throws IllegalActionException 
     * @throws IllegalAccessException 
     * @throws NumberFormatException 
     */
    public void handleChangPropagationBack(Track currentTrack) throws IllegalActionException, NumberFormatException, IllegalAccessException {
        // TODO Auto-generated method stub
       
        int regionId=((IntToken) currentTrack.regionId).intValue();
        
//        if(regionId==4)
//            System.out.println(_currentStateIndex);
        
        Track track;
        // Remove events related to this region from eventQueue, The aircraft are not under analysis anymore,
        // because we will fill their related track in load information and they will be set under analysis
        Object[] objectArray=_eventQueue.toArray();
        for(Object o: objectArray) {
            if(((DEEvent)o).actor() instanceof Track) {
                int trackId=((IntToken)(((Track)((DEEvent)o).actor()).trackId.getToken())).intValue();
                if((track=regions.get(regionId-1)._containsTrack(regions.get(regionId-1).tracks,trackId))!=null) {
                    if(track._inTransit!=null) {
                        int aircraftId=((IntToken)((RecordToken)(track._inTransit)).get("aircraftId")).intValue();
                        int aircraftInAff=((IntToken)((RecordToken)currentTrack._inTransit).get("aircraftId")).intValue();
                        if(aircraftId!=aircraftInAff)
                            underAnalysisMO.remove(aircraftId);
                    }
//                    else
//                        throw new IllegalActionException("No moving object in this track");
                    _eventQueue.remove((DEEvent)o);
                }
            }
        }
        // Active the region
        _loadInformationOfRegion(regionId, getModelTime().getDoubleValue(),currentTrack,true);
    }

    /**
     * When the track does not send out its aircraft at the pre-specified time
     * @param track
     * @param destinationRegionId 
     * @throws IllegalAccessException 
     * @throws IllegalActionException 
     * @throws NumberFormatException 
     */
    public void handleChangPropagationForward(Track track, int destinationRegionId) throws NumberFormatException, IllegalActionException, IllegalAccessException {
        // TODO Auto-generated method stub
        
        if(regions.get(destinationRegionId-1).underAnalysis==true)
            return;
        else {
         // Remove events related to this region from eventQueue
            Object[] objectArray=_eventQueue.toArray();
            for(Object o: objectArray) {
                if(((DEEvent)o).actor() instanceof Track) {
                    int trackId=((IntToken)(((Track)((DEEvent)o).actor()).trackId.getToken())).intValue();
                    Track track2;
                    if((track2=regions.get(destinationRegionId-1)._containsTrack(regions.get(destinationRegionId-1).tracks,trackId))!=null) {
                        if(track2._inTransit!=null) {
                            int aircraftId=((IntToken)((RecordToken)(track2._inTransit)).get("aircraftId")).intValue();
                            underAnalysisMO.remove(aircraftId);
                        }
//                        else
//                            throw new IllegalActionException("No moving object in this track");
                        _eventQueue.remove((DEEvent)o);
                    }
                }
            }
        }
        // Active the region
        _loadInformationOfRegion(destinationRegionId, getModelTime().getDoubleValue(), track,false);
    }

    /**
     * Get departure time of the given moving object from the given region
     * @param planeId
     * @param regionId
     * @return
     */
    public double getDepartureTime(int planeId, int regionId) {
        // TODO Auto-generated method stub
        if(!regions.get(regionId-1).travelingAicrafts.containsKey(planeId))
            return -1;
        String route=regions.get(regionId-1).travelingAicrafts.get(planeId);
        String[] parts=route.split(";");
        String[] parts0=parts[0].split(",");
        String schedule=parts0[parts0.length-1];
        schedule=schedule.substring(1, schedule.length()-1);
        return Double.valueOf(schedule.split("/")[1])+1;
    }
    
    public int getDepartureTrack(int planeId, int regionId) {
        // TODO Auto-generated method stub
        if(regions.get(regionId-1).departureTracks.containsKey(planeId))
            return regions.get(regionId-1).departureTracks.get(planeId);
        else
            return -1;
    }

    public boolean getRegionStatus(int destinationRegionId) {
        // TODO Auto-generated method stub
        int regionId=destinationRegionId-1;
        return regions.get(regionId).underAnalysis;
    }
    
    /** Reroute an aircraft.
     *  @param aircraft The aircraft
     *  @return A Map of rerouted aircraft.
     *  @exception IllegalActionException If thrown while getting the flightMap or setting parameters.
     */
    public Map<String, Token> rerouteUnacceptedAircraft(Token aircraft,int regionId)
            throws IllegalActionException {
        
        RecordToken airplane = (RecordToken) aircraft;
        String flightMap = ((StringToken) airplane.get("flightMap")).stringValue();
//        System.out.println("airplane: "+airplane.get("aircraftId")+" route: "+flightMap);
//        if(flightMap.equals("(17/83.55),(8/84.55),(9/85.55),(99/86.55)") && ((IntToken)airplane.get("aircraftId")).intValue()==138)
//            System.out.println("HEY");
        String [] parts=flightMap.split(";")[0].split(",");
        int priorTrack = ((IntToken) airplane.get("priorTrack")).intValue();
        int currentTrack = Integer.valueOf(parts[0].split("/")[0].substring(1)); // this is the rejecting track
        double currentTime=Double.valueOf(parts[0].split("/")[1].substring(0,parts[0].split("/")[1].length()-1));
        int sourceX=priorTrack/dimension;
        int sourceY=priorTrack%dimension;
        int destX=0;
        int destY=0;
        int currentX=currentTrack/dimension;
        int currentY=currentTrack%dimension;
        
        int index=0;
        double delay=0.0;
        
        List<Integer> xyRoute=new ArrayList<>();
        parts=flightMap.replaceAll(";", ",").split(",");
        boolean routeSelected=false;
        for(int i=1;i<parts.length-1;i++) { //It finds the route from the first available neighbor of the prior track
            // to the first place in its route. The route found
            // should be in the same size as the previous one. 
            String destination=parts[i].split("/")[0].substring(1);
            destX=Integer.valueOf(destination).intValue()/dimension;
            destY=Integer.valueOf(destination).intValue()%dimension;
            
            if(sourceY==currentY) {
                if(sourceY<dimension && sourceY!=0)
                    xyRoute=generateRoute(currentTrack, priorTrack,sourceX, sourceY+1, destX, destY);
                else
                    xyRoute=null;
                if(xyRoute!=null && xyRoute.size()==(i+1)) {
                    routeSelected=true;
                    delay=0.0;
                    index=i;
                    break;
                }
                else
                    continue;
            }
            
            if(sourceX==currentX) { // the track who is in the same region as the prior one has the highest priority.
                boolean incX=true;
                boolean decX=true;
                int priority=2; // -1: decX has the higher priority, 1: incX has the higher priority
                                // 0: both are in the same region.
                if(sourceX-1>=0) {
                    decX=false;
                    int id=(sourceX-1)*dimension+sourceY;
                    if(regions.get(regionId-1).tracks.containsKey(id))
                        priority=-1;
                }
                if(sourceX+1<dimension) {
                    incX=false;
                    int id=(sourceX+1)*dimension+sourceY;
                    if(regions.get(regionId-1).tracks.containsKey(id))
                        if(priority==-1)
                            priority=0;
                        else 
                            priority=1;   
                }
                
                while(incX==false || decX==false) {
                    if(priority==0 || priority==-1)
                    {
                        if(decX==false) {
                            xyRoute=generateRoute(currentTrack,priorTrack, sourceX-1, sourceY, destX, destY);
                            if(xyRoute!=null && xyRoute.size()==(i+1)) {
                                routeSelected=true;
                                delay=0.0;
                                index=i;
                                break;
                            }
                            decX=true;
                        }
                        else if(incX==false) {
                            xyRoute=generateRoute(currentTrack, priorTrack, sourceX+1, sourceY, destX, destY);
                            if(xyRoute!=null && xyRoute.size()==(i+1)) {
                                routeSelected=true;
                                delay=0.0;
                                index=i;
                                break;
                            }
                            incX=true;
                        }
                    }
                    else if(priority==1) {
                        if(incX==false) {
                            xyRoute=generateRoute(currentTrack, priorTrack, sourceX+1, sourceY, destX, destY);
                            if(xyRoute!=null && xyRoute.size()==(i+1)) {
                                routeSelected=true;
                                delay=0.0;
                                index=i;
                                break;
                            }
                            incX=true;
                        }
                        else if(decX==false) {
                            xyRoute=generateRoute(currentTrack,priorTrack, sourceX-1, sourceY, destX, destY);
                            if(xyRoute!=null && xyRoute.size()==(i+1)) {
                                routeSelected=true;
                                delay=0.0;
                                index=i;
                                break;
                            }
                            decX=true;
                        }
                        
                    }
                }
                if(routeSelected==true)
                    break;
            }
        }
        
        if(routeSelected==false) {
            if(parts.length==1) { //It only contains the destination
                // Send it again to the destination.
                flightMap=rebuildRouteWithNewTime(currentTime, flightMap);
                Map<String, Token> map = new TreeMap<String, Token>();
                map.put("flightMap", new StringToken(flightMap));
                map.put("route", new IntToken(-1));
                map.put("delay", new DoubleToken(1.0));
//                System.out.println("airplane: Finish");
                return map;
            }
            if(((BooleanToken)_stormyTracks.get(currentTrack)).booleanValue()==true) {
                for(int i=1;i<parts.length-1;i++) {
                    String destination=parts[i].split("/")[0].substring(1);
                    destX=Integer.valueOf(destination).intValue()/dimension;
                    destY=Integer.valueOf(destination).intValue()%dimension;
                    xyRoute=findPath(currentTrack,priorTrack, sourceX, sourceY, destX, destY, regionId);
                    if(xyRoute!=null) {
                        routeSelected=true;
                        delay=1.0;
                        index=i;
                        break;
                    }
                }
            }
            if(routeSelected==false) {
                flightMap=rebuildRouteWithNewTime(currentTime, flightMap);
                Map<String, Token> map = new TreeMap<String, Token>();
                map.put("flightMap", new StringToken(flightMap));
                map.put("route", new IntToken(-1));
                map.put("delay", new DoubleToken(1.0));

                return map;
            }
        }
        //
        int route=-1;
        
        ArrayToken neighborsOfPriorTrack = _neighbors.get(priorTrack);
        for (int i = 0; i < neighborsOfPriorTrack.length(); i++) {
            Token temp = neighborsOfPriorTrack.getElement(i);
            int tempId = ((IntToken) temp).intValue();

            if(tempId==xyRoute.get(0)) {
                route=i;
                break;
            }
        }
        String newMap=rebuildRoute(currentTime, xyRoute, regionId);
        String tempMap=flightMap.substring(flightMap.indexOf(parts[index])+parts[index].length());
        String lastElement=newMap.replaceAll(";", ",").split(",")[newMap.replaceAll(";", ",").split(",").length-1];
        double lastTime=Double.valueOf(lastElement.split("/")[1].substring(0, lastElement.split("/")[1].length()-1));
        String secondPart=rebuildRouteWithNewTime(lastTime,tempMap);
        flightMap=newMap.concat(secondPart);
        
        Map<String, Token> map = new TreeMap<String, Token>();
        map.put("flightMap", new StringToken(flightMap));
        map.put("route", new IntToken(route));
        map.put("delay", new DoubleToken(delay));
//        System.out.println("airplane: Finish");
        return map; 
    }
    
    /**
     * It receive a path and time and rebuild the path starting with the given time
     * @param lastTime
     * @param tempMap
     * @return
     */
    private String rebuildRouteWithNewTime(double lastTime, String tempMap) {
        // TODO Auto-generated method stub
        String[] parts=tempMap.replaceAll(";", ",").split(",");
        String newMap="";
        int count=0;
        for(int i=0;i<parts.length;i++)
        {
            if(!parts[i].equals("")) {
                int trackId=Integer.valueOf(parts[i].split("/")[0].substring(1));
                lastTime+=1;
                newMap+="("+trackId+"/"+lastTime+")";
                count+=parts[i].length();
                if(tempMap.length()-1>count)
                    newMap+=tempMap.charAt(count);
            }
            else {
                newMap+=tempMap.charAt(count);
            }
            count+=1;
        }
        return newMap;
    }

    /**
     * Find a path if there is no available track around the priorTrack from which there
     * is an xyRoute to a track in the initial route of a moving object
     * @param currentTrack
     * @param sourceX
     * @param sourceY
     * @param destX
     * @param destY
     * @param regionId
     * @return
     */
   private List<Integer> findPath(int currentTrack, int priorTrack, int sourceX, int sourceY, int destX, int destY, int regionId){
       
       List<Integer> xyRoute=new ArrayList<>();
       int currentX=currentTrack/dimension;
       int currentY=currentTrack%dimension;
       
       if(sourceY==currentY) {
           if(sourceY<dimension)
               xyRoute=generateTempRoute(currentTrack, priorTrack, sourceX, sourceY+1, destX, destY);
           else
               xyRoute=null;
           if(xyRoute!=null) 
               return xyRoute;
       }
       
       if(sourceX==currentX) {
           boolean incX=true;
           boolean decX=true;
           int priority=2; // -1: decX has the higher priority, 1: incX has the higher priority
                           // 0: both are in the same region.
           if(sourceX-1>=0) {
               decX=false;
               int id=(sourceX-1)*dimension+sourceY;
               if(regions.get(regionId-1).tracks.containsKey(id))
                   priority=-1;
           }
           if(sourceX+1<dimension) {
               incX=false;
               int id=(sourceX+1)*dimension+sourceY;
               if(regions.get(regionId-1).tracks.containsKey(id))
                   if(priority==-1)
                       priority=0;
                   else 
                       priority=1;   
           }
           
           while(incX==false || decX==false) {
               if(priority==0 || priority==-1)
               {
                   if(decX==false) {
                       xyRoute=generateTempRoute(currentTrack,priorTrack, sourceX-1, sourceY, destX, destY);
                       if(xyRoute!=null) {
                           return xyRoute;
                       }
                       decX=true;
                   }
                   else if(incX==false) {
                       xyRoute=generateTempRoute(currentTrack,priorTrack, sourceX+1, sourceY, destX, destY);
                       if(xyRoute!=null ) 
                           return xyRoute;
                       incX=true;
                   }
               }
               else if(priority==1) {
                   if(incX==false) {
                       xyRoute=generateTempRoute(currentTrack,priorTrack, sourceX+1, sourceY, destX, destY);
                       if(xyRoute!=null) 
                           return xyRoute;
                       incX=true;
                   }
                   else if(decX==false) {
                       xyRoute=generateTempRoute(currentTrack,priorTrack, sourceX-1, sourceY, destX, destY);
                       if(xyRoute!=null)
                           return xyRoute;
                       decX=true;
                   }
                   
               }
           }
       }
       return null;
   }
    

    
    /**
     * It receives an xyRoute which is a sequence of integers and generates a route with the times
     * @param xyRoute
     * @param flightMaps
     * @param regionId
     * @return
     * @throws IllegalActionException 
     */
    private String rebuildRoute(double time, List<Integer> xyRoute, int regionId) throws IllegalActionException{
        String newMap="";
        String sign="";
        
        for(int j=0; j<xyRoute.size();j++) {
            newMap+="("+xyRoute.get(j)+"/"+time+")";
            time=time+1;
            if(j+1<xyRoute.size() && regions.get(regionId-1)._containsTrack(regions.get(regionId-1).tracks, xyRoute.get(j+1))==null
                    && !_airportsId.containsKey(xyRoute.get(j+1))){
                sign=";";
                regionId=findRegionofTrack(xyRoute.get(j+1));
                if(regionId==-1)
                    throw new IllegalActionException("No region for this track number");
             }
             else
                 sign=",";
            
            if(j!=xyRoute.size()-1)
                newMap+=sign;
        }
        return newMap;
    }
    
    


    
    public int findRegionofTrack(int trackNum) {
        // TODO Auto-generated method stub

        int numOfRegionsInEachRowOrColumn=dimension/regionDimension;
        int w=trackNum/dimension; // to find the row indexed from 0
        int z=trackNum%dimension; // to find the column indexed from 1

        
        int x=w/regionDimension; // to find row of the region
        int y=z/regionDimension; // to find column of the region
        
        if(z==0) {
            int region=regionDimension;
            if(w%regionDimension==0)
                return (x*numOfRegionsInEachRowOrColumn);
            else if(w%regionDimension!=0)
                return (numOfRegionsInEachRowOrColumn+x*numOfRegionsInEachRowOrColumn);   
        }

        
        if(z%regionDimension==0)
            return x*numOfRegionsInEachRowOrColumn+y;
        else
            return x*numOfRegionsInEachRowOrColumn+y+1;
    }

    /**
     * It generate an xyRoute from a give source to a given destination. It first checks whether the given source is available. 
     * If yes, then it tries to move in the x dimension (north or south). if the track in that direction 
     * is not available, tries to move in the y dimension. This procedure continues to reach the destination. The rouete also
     * Contains the destination. 
     * @param unavailableTrack
     * @param sX
     * @param sY
     * @param desX
     * @param desY
     * @return
     */
    private List<Integer> generateRoute(int unavailableTrack,int priorTrack, int sX, int sY, int desX, int desY){
        List<Integer> route=new ArrayList<Integer>();
        boolean flag=false;
        boolean increaseX=false;
        boolean decreaseX=false;
        if(sY==dimension) {
            sX+=1;
            sY=0;
        }
        int sourceX=sX;
        int sourceY=sY;
        route.add(sourceX*dimension+sourceY);
        while(sourceX-desX!=0 || sourceY-desY!=0) {
            int incX=IncreaseXY(sourceX, sourceY, desX, desY, 0);
            int incY=IncreaseXY(sourceX, sourceY, desX, desY, 1);
            
            int source=sourceX;
            //Check flag:
            if(flag==false) {
                flag=true;
                
                // If this track is generator, it does not matter. Depending on which actor
                // is fired first, different things happen. If the neighbor is fired, the change propagates into its region.
                // Otherwise, this actor is fired and again the change will propagate to the next region. 
                int neighborRegionId=findRegionofTrack(sourceX*dimension+sourceY);
                if(unavailableTrack==(source*dimension+sourceY) || _stormyTracks.get(source*dimension+sourceY).equals(new BooleanToken(true))
                        ||  (regions.get(neighborRegionId-1).tracks.get(sourceX*dimension+sourceY).generator==false &&
                        (_inTransit.get(source*dimension+sourceY) || inputTokensContain(source*dimension+sourceY)))
                        || _airportsId.containsKey(source*dimension+sourceY)){
                    return null;
                }
            }// End of check flag.
            
            if(incX==1) {
                source+=1;
                increaseX=true;
            }
            else if(incX==-1) {
                source-=1;
                decreaseX=true;
            }
            
            //First if:
            if((incX==1 || incX==-1) && (unavailableTrack==(source*dimension+sourceY) || priorTrack==(source*dimension+sourceY)
                    || _stormyTracks.get(source*dimension+sourceY).equals(new BooleanToken(true)))){
                if(source>sourceX) // to know we have increased or decreased.
                    increaseX=false;
                else
                    decreaseX=false;
                if(incY==1 || incY==0) {
                    int x=sourceX;
                    int y=sourceY;
                    if(sourceY+1==dimension) // The last column has y=0 but its x is more than the tracks in its row
                    {
                        x+=1;
                        y=0;
                    }
                    else
                    {
                        y+=1;
                    }
                    if((incY==1 && (unavailableTrack==(x*dimension+y) || priorTrack==(x*dimension+y) || 
                            _stormyTracks.get(x*dimension+y).equals(new BooleanToken(true)))) || incY==0) {
                        return null;
                    }
                }
                
            }//End of First if
            
            //Second if:
            if(incX==0 && incY==1) {
                int x=sourceX;
                int y=sourceY;
                if(sourceY+1==dimension)
                {
                    x+=1;
                    y=0;
                }
                else
                {
                    y+=1;
                }
                if(incX==0 && incY==1 && (unavailableTrack==(x*dimension+y) || priorTrack==(x*dimension+y)
                        || _stormyTracks.get(x*dimension+y).equals(new BooleanToken(true)))) {
                    return null;
                }
            }
            
            
            if(incX==1 && increaseX==true)
            {
                sourceX+=1;
                route.add(sourceX*dimension+sourceY);
                continue;
            }
            else if(incX==-1 && decreaseX==true) 
            {
                sourceX-=1;
                route.add(sourceX*dimension+sourceY);
                continue;
            }
            else if(incY==1)
            {
                sourceY+=1;
                if(sourceY==dimension) {
                    sourceY=0;
                    sourceX++;
                }
                route.add(sourceX*dimension+sourceY);
                continue;
            }
            else
                return null;
        }
        return route;
    }
  
    /**
     * It return an xyRoute from a give source to a give destination. It does not check if the source
     * track is available. 
     * @param sX
     * @param sY
     * @param desX
     * @param desY
     * @return
     */
    private List<Integer> generateTempRoute(int unavailableTrack, int priorTrack, int sX, int sY, int desX, int desY){
        List<Integer> route=new ArrayList<Integer>();
        if(sY==dimension)
        {
            sX+=1;
            sY=0;
        }
        int sourceX=sX;
        int sourceY=sY;
        route.add(sourceX*dimension+sourceY);
        while(sourceX-desX!=0 || sourceY-desY!=0) {
            int incX=IncreaseXY(sourceX, sourceY, desX, desY, 0);
            int incY=IncreaseXY(sourceX, sourceY, desX, desY, 1);
            
            if(incX==1 && unavailableTrack!=(sourceX+1)*dimension+sourceY && priorTrack!=(sourceX+1)*dimension+sourceY 
                    && _stormyTracks.get((sourceX+1)*dimension+sourceY).equals(new BooleanToken(false)))
            {
                sourceX+=1;
                route.add(sourceX*dimension+sourceY);
                continue;
            }
            else if(incX==-1 && unavailableTrack!=(sourceX-1)*dimension+sourceY && priorTrack!=(sourceX-1)*dimension+sourceY
                    && _stormyTracks.get((sourceX-1)*dimension+sourceY).equals(new BooleanToken(false)) ) 
            {
                sourceX-=1;
                route.add(sourceX*dimension+sourceY);
                continue;
            }
            else if(incY==1) {
                int x=sourceX;
                int y=sourceY;
                if(sourceY+1==dimension) {
                    x+=1;
                    y=0;
                }
                else {
                    y+=1;
                }
                if(unavailableTrack!=x*dimension+y && priorTrack!=x*dimension+y && 
                        _stormyTracks.get(x*dimension+y).equals(new BooleanToken(false)))
                {
                    sourceY+=1;
                    if(sourceY==dimension) {
                        sourceY=0;
                        sourceX++;
                    }
                    route.add(sourceX*dimension+sourceY);
                    continue;
                }
            }
            else
                return null;
        }
        return route;
    }
    
    /**
     * The direction of the travel from (sourceX, sourceY) towards (destX, destY)
     * @param sourceX
     * @param sourceY
     * @param destX
     * @param destY
     * @param i
     * @return increaseX or increaseY based on the value of i: -1 for increaseX shows travel towards the north,
     *  0 don't move, 1 for increaseX shows travel towards south, 1 for increaseY shows travel towards east
     */
    private  int IncreaseXY(int sourceX, int sourceY, int destX, int destY, int i) {
        // TODO Auto-generated method stub
        int increaseX=0; // Neither increase nor decrease
        int increaseY=0;
        
        if(sourceX==destX || (sourceX+1==destX && sourceY!=0 && destY==0)){ // Both are in the same row and one in
            // the last column whose index is zero
            increaseX=0; 
        }
        else if(sourceX-1==destX && sourceY==0 && destY!=0) { //Both are in the same row and one in the last column whose index is zero
            increaseX=0;
        }
        else if(sourceX<destX)
            increaseX=1;
        else if(sourceX>destX)
            increaseX=-1;
        
        if(sourceY==destY || sourceY==0){
            increaseY=0;
        }
        else if(sourceY<destY || (sourceY!=destY && destY==0))
            increaseY=1;
        
        if(i==0)
            return increaseX;
        else
            return increaseY;
    }

    /**
     * The magnifier starts here, by focusing on a given region.
     * @param regionId
     */
    private void startMagnifying(int regionId) {
        // TODO Auto-generated method stub
        try {
            startTime=System.currentTimeMillis(); 
            _readFromFile();
            _loadInformationOfRegion(regionId, getCurrentTime(), null,false);
            _informationLoaded=true;
        } catch (NumberFormatException | IllegalActionException | IllegalAccessException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }
//    /**
//     * aircraftsInRegions. For each region contains a map that maps each aircraft to its route
//     * in the region. The route contains the arrival time of the aircraft at each track. 
//     * Therefore we have arrival and departure time of the aircraft to a region. 
//     */
//    private ArrayList<Map<Integer,ArrayToken>> aircraftsInRegions;

    /**
     * Return true if the given track is in the region.
     * @param regionId
     * @param nextTrack
     * @return
     */
    public boolean isInTheRegion(int regionId, int nextTrack) {
        // TODO Auto-generated method stub
        if(regions.get(regionId-1)._containsTrack( regions.get(regionId-1).tracks, nextTrack)!=null)
            return true;
        return false;
    }

    public void setInRegionAirArrived(int aircraftId, int destinationRegionId) {
        // TODO Auto-generated method stub
        regions.get(destinationRegionId-1).aircraftHasArrived.add(aircraftId);
        removeFromUnderAnalysis(aircraftId);
        
    }

    public boolean nextIsAirport(int nextTrack) {
        // TODO Auto-generated method stub
        if(_airportsId.containsKey(nextTrack))
            return true;
        return false;
    }

    /**
     * Returns route of a moving object in a region
     * @param planeId
     * @param destinationRegionId
     * @return
     */
    public String getRoute(int planeId, int destinationRegionId) {
        // TODO Auto-generated method stub
        if(regions.get(destinationRegionId-1).travelingAicrafts.containsKey(planeId))
        return regions.get(destinationRegionId-1).travelingAicrafts.get(planeId);
        else 
            return "";
        
    }
    

    
    public void removeFromUnderAnalysis(int id) {
        if(underAnalysisMO.containsKey(id))
            underAnalysisMO.remove(id);
    }
    
 public boolean inputTokensContain(int trackId) {
        
        for(Entry<String, Map<Integer,Token>> entry: _stateSpace.get(_currentStateIndex).inputTokens.entrySet()) {
            for(Entry<Integer, Token> inEntry: entry.getValue().entrySet()){
                if(inEntry.getValue()!=null) {
                    String flightMap=((StringToken)((RecordToken)inEntry.getValue()).get("flightMap")).stringValue();
                    if(!flightMap.equals(""))
                        if(Integer.valueOf(flightMap.split(",")[0].split("/")[0].substring(1))==trackId)
                            return true;
                }
            }
        }
        return false;
    }
 
 public RecordToken createNewMObject(RecordToken aircraftWithInformation, double consumedFuel) throws IllegalActionException {
     Map<String, Token> newAircraft = new TreeMap<String, Token>();
     newAircraft.put("aircraftId",
             aircraftWithInformation.get("aircraftId"));
     newAircraft.put("aircraftSpeed",
             aircraftWithInformation.get("aircraftSpeed"));
     newAircraft.put("flightMap",
             aircraftWithInformation.get("flightMap"));
     newAircraft.put("priorTrack",
             aircraftWithInformation.get("priorTrack"));
    double fuel=((DoubleToken) aircraftWithInformation.get("fuel")).doubleValue()-consumedFuel;
    if(fuel<=0)
        deadlockDetected=true;
//        throw new IllegalActionException("deadlock: the airplane "+aircraftWithInformation.get("aircraftId")+ " With route "+aircraftWithInformation.get("flightMap"));
     newAircraft.put("fuel", new DoubleToken(fuel));
     return new RecordToken(newAircraft);
 }
 
 /**
  * We assumed that we will run the model for several input files, for instance input1.txt, input2.txt and for each input
  * file we will get two output files, for ins:outputS1.txt, outputT1.txt and ...
  * We assume that the executive director of MagnfierDirector is SDF, we get its current iteration and based on return an index
  * which is used in the name of the input and output files. As we run the model for three different times at which a change happens,
  * we divid the iteration to 3. 
  * @param number
  * @return
  */
 public int iterationCount(int number) {
     int mod=number%3;
     int corr=number/3;
     
     if(mod==0)
         return corr;
     else
         return (corr+1);
 }

public Token findNeighbors(int trackNum) {
    // TODO Auto-generated method stub
    int northNeighbor=trackNum-dimension;
    if(northNeighbor<0)
        northNeighbor=-1;
    
    int southNighbor=trackNum+dimension;
    if(southNighbor>dimension*dimension) {
        if(trackNum==dimension*dimension)
            southNighbor=-1;
        else
            southNighbor=dimension*dimension+dimension+dimension-1+dimension+(trackNum%dimension);
    }
    
    int eastNeighbor=trackNum+1;
    if(trackNum%dimension==0) {
        eastNeighbor=dimension*dimension+dimension+dimension-1+(trackNum/dimension);
    }
    String x="{"+northNeighbor+","+eastNeighbor+","+southNighbor+"}";
    ArrayToken allNeighbors=null;
    try {
        allNeighbors = new ArrayToken(x);
    } catch (IllegalActionException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
    return (Token)allNeighbors;
}

public int returnDimention() {
    int n=0;
    try {
        n = ((IntToken)networkDimension.getToken()).intValue();
    } catch (IllegalActionException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
    return n; 
}


public void removeFromQueue(int id) throws IllegalActionException {
    // TODO Auto-generated method stub
    Object[] objectArray=_eventQueue.toArray();
    for(Object o: objectArray) {
        if(((DEEvent)o).actor() instanceof Track) {
            int trackId=((IntToken)(((Track)((DEEvent)o).actor()).trackId.getToken())).intValue();
            if(trackId==id) {
              _eventQueue.remove((DEEvent)o);
              return;
            }
            }
        }
}

public void addToOtherRegions(Token _inTransit, int destinationRegionId) throws IllegalActionException, IllegalAccessException {
    // TODO Auto-generated method stub
    int planeId=((IntToken)((RecordToken) _inTransit).get("aircraftId")).intValue();
    String[] maps=((StringToken)((RecordToken)_inTransit).get("flightMap")).stringValue().split(";");
    int previousRegion=destinationRegionId;
    for(int i=0;i<maps.length;i++) {
        int trackId=Integer.valueOf(maps[i].split(",")[0].split("/")[0].substring(1));
        double time=Double.valueOf(maps[i].split(",")[0].split("/")[1].substring(0,maps[i].split(",")[0].split("/")[1].length()-1));
        int regionId=findRegionofTrack(trackId)-1;
        if(regions.get(regionId).underAnalysis==true) {
            if(i==0)
                throw new IllegalActionException("This region should not be under analysis");
            int entryTrack=regions.get(regionId).entryTracks.get(planeId);
            
            Track track=null;
            if((track=regions.get(previousRegion)._containsTrack(regions.get(previousRegion).tracks, entryTrack))!=null) {
                
                Map<String, Token> aircraft = new TreeMap<String, Token>();
                aircraft.put("aircraftId", new IntToken(planeId));
                aircraft.put("aircraftSpeed", new IntToken(200));
                aircraft.put("flightMap", new StringToken(regions.get(regionId).travelingAicrafts.get(planeId)));
                aircraft.put("priorTrack", new IntToken(entryTrack));
                
                double fuel=time-arrivalTimeToNet.get(planeId);
                
                if(dimension==15)
                    aircraft.put("fuel", new DoubleToken(325-fuel));
                else if (dimension==12)
                    aircraft.put("fuel", new DoubleToken(244-fuel));
                else if(dimension==9)
                    aircraft.put("fuel", new DoubleToken(200-fuel));
                else
                    aircraft.put("fuel", new DoubleToken(500-fuel));
                
                String direction="";
                if(((BooleanToken)((ArrayToken)track.border.getToken()).contains(new StringToken("N"+(regionId+1)))).booleanValue())
                {
                    direction="South";
                }
                else if(((BooleanToken)((ArrayToken)track.border.getToken()).contains(new StringToken("W"+(regionId+1)))).booleanValue())
                {
                    direction="East";
                }
                else if(((BooleanToken)((ArrayToken)track.border.getToken()).contains(new StringToken("S"+(regionId+1)))).booleanValue())
                {
                    direction="North";
                }
                else if(((BooleanToken)((ArrayToken)track.border.getToken()).contains(new StringToken("E"+(regionId+1)))).booleanValue())
                {
                    direction="West";
                }
                
                if(track.generator==true) {
                    track.putMovingObject(new RecordToken(aircraft),direction,time);

                }
                else
                {
                    track.generator=true; 
                    track.putMovingObject(new RecordToken(aircraft),direction,time);
                    track.startGeneratingInput();
                }

                String value=String.valueOf(entryTrack)+","+time;
                underAnalysisMO.put(planeId, value);
                
             }
            else throw new IllegalActionException("Region does not have this track");
            return;
        }
        previousRegion=regionId;
    }
    
}
    
}
