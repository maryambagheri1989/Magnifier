/* A director for modeling air traffic control systems.

 Copyright (c) 2015-2016 The Regents of the University of California.
 All rights reserved.
 Permission is hereby granted, without written agreement and without
 license or royalty fees, to use, copy, modify, and distribute this
 software and its documentation for any purpose, provided that the above
 copyright notice and the following two paragraphs appear in all copies
 of this software.

 IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
 FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
 THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
 SUCH DAMAGE.

 THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
 CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 ENHANCEMENTS, OR MODIFICATIONS.

 PT_COPYRIGHT_VERSION_2
 COPYRIGHTENDKEY

 */
package ptolemy.domains.atc.kernel.policy1;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;
import ptolemy.actor.IOPort;
import ptolemy.actor.Receiver;
import ptolemy.data.ArrayToken;
import ptolemy.data.BooleanToken;
import ptolemy.data.DoubleToken;
import ptolemy.data.IntToken;
import ptolemy.data.RecordToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.domains.atc.kernel.ATCReceiver;
import ptolemy.domains.atc.kernel.AbstractATCDirector;
import ptolemy.domains.atc.kernel.AirportFeilds;
import ptolemy.domains.atc.kernel.DestinationAirportFields;
import ptolemy.domains.atc.lib.Airport;
import ptolemy.domains.atc.lib.DestinationAirport;
import ptolemy.domains.atc.lib.NMTrack;
import ptolemy.domains.atc.lib.Track;
import ptolemy.domains.de.kernel.DEEvent;
import ptolemy.domains.de.kernel.DEEventQueue;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;

/** A director for modeling air traffic control systems.
 *  This director provides a receiver that consults the destination actor
 *  to determine whether it can accept an input, and provides mechanisms
 *  for handling rejection of an input.
 *  @author Maryam Bagheri
 *  @version $Id$
 *  @since Ptolemy II 11.0
 */
public class ATCDirector extends AbstractATCDirector {

    // FIXME: This class and policy1/ATCDirector.java have quite a bit
    // of duplicated code.  It would be better to create a common base
    // class.

    /** Create a new director in the specified container with the specified
     *  name.  The name must be unique within the container or an exception
     *  is thrown. The container argument must not be null, or a
     *  NullPointerException will be thrown.
     *
     *  @param container The container.
     *  @param name The name of this actor within the container.
     *  @exception IllegalActionException If this actor cannot be contained
     *   by the proposed container (see the setContainer() method).
     *  @exception NameDuplicationException If the name coincides with
     *   an entity already in the container.
     */
    public ATCDirector(CompositeEntity container, String name)
            throws IllegalActionException, NameDuplicationException {
        super(container, name);
        // TODO Auto-generated constructor stub
    }

    protected void _fillDestinationAirport(NamedObj actor, DestinationAirportFields destinationAirportFields) {
        // TODO Auto-generated method stub
        ((DestinationAirport)actor)._inTransit=destinationAirportFields._inTransit;
        ((DestinationAirport)actor)._called=destinationAirportFields._called;
        ((DestinationAirport)actor)._transitExpires=destinationAirportFields._transitExpires;
        
    }

    protected void _fillInputPort(DEEvent event, Map<Integer, Token> inputTokens) throws IllegalActionException {
        // TODO Auto-generated method stub
        IOPort port=event.ioPort();
        Receiver[][] receivers=port.getReceivers();
        for(Entry<Integer, Token> entry: inputTokens.entrySet()){
            ((ATCReceiver)receivers[entry.getKey()][0]).putToken(entry.getValue());
        }
    }

    protected void _fillAirport(NamedObj actor, AirportFeilds airportFeilds) {
        // TODO Auto-generated method stub
        ((Airport)actor)._airplanes.clear();
        for(int i=0;i<airportFeilds._airplanes.size();i++)
            ((Airport)actor)._airplanes.add(airportFeilds._airplanes.get(i));
        ((Airport)actor)._inTransit=airportFeilds._inTransit;
        ((Airport)actor)._transitExpires=airportFeilds._transitExpires;
    }

    
    public DEEventQueue getEventQueue() {
        synchronized(_eventQueue){
            return _eventQueue;
        }
    }
    
    
    /** Return airplane's color. If the airplane has not color, set a color for that and store it.
     *  @param id id of the airplane
     *  @return The airplane's color.x
     *  @exception IllegalActionException If thrown while creating an ArrayToken.
     */
    @Override
    public ArrayToken handleAirplaneColor(int id)
            throws IllegalActionException {
        ArrayToken color = _airplanesColor.get(id);

        if (color == null) {
            Token[] colorSpec = new DoubleToken[4];
            colorSpec[0] = new DoubleToken(_random.nextDouble());
            colorSpec[1] = new DoubleToken(_random.nextDouble());
            colorSpec[2] = new DoubleToken(_random.nextDouble());
            colorSpec[3] = new DoubleToken(1.0);
            color = new ArrayToken(colorSpec);
            _airplanesColor.put(id, color);
        }

        return color;
    }

    /** Handle initializing of an airport.
     *  @param airport The airport.
     *  @exception IllegalActionException If the id is invalid.
     */
    @Override
    public void handleInitializedAirport(NamedObj airp)
            throws IllegalActionException {
        Airport airport=(Airport)airp;
        int airportId = ((IntToken) airport.airportId.getToken()).intValue();
        //        if (airportId == -1)
        //            throw new IllegalActionException("invalid id for airplane");
        //                if (_airplanesId.contains(airplaneId))
        //            throw new IllegalActionException("duplication in  airplanes id");
        //        _airplanesId.add(airplaneId);
        if (airportId == -1) {
            throw new IllegalActionException("Invalid id for source airport");
        }
        if (_stormyTracks.containsKey(airportId)) {
            throw new IllegalActionException("Airport id is same as track id");
        }
        if (!_airportsId.containsKey(airportId)) {
            _airportsId.put(airportId,airport);
        }

        //        if (((ArrayToken)airplane.flightMap.getToken()) == null)
        //            throw new IllegalActionException("flightMap is empty");
    }

    /** Handle initializing of a destination airport. This function stores airport id in _airportsId
     *  @param destinationAirport The destination airport.
     *  @exception IllegalActionException If the id is invalid, the id is
     *  a duplicate of the idea of another airport or if the airport
     *  id is the same as the a track id.
     */
    @Override
    public void handleInitializedDestination(
            NamedObj destinationAirport)
            throws IllegalActionException {
        // TODO Auto-generated method stub
        int airportId = ((IntToken) ((DestinationAirport)destinationAirport).airportId.getToken())
                .intValue();
        if (airportId == -1) {
            throw new IllegalActionException(
                    "Invalid id for destination airport");
        }
        if (_airportsId.containsKey(airportId)) {
            throw new IllegalActionException("Duplication in airports id");
        }
        if (_stormyTracks.containsKey(airportId)) {
            throw new IllegalActionException("Airport id is same as track id");
        }
        _airportsId.put(airportId, null);
        _desAirports.put(airportId, (DestinationAirport)destinationAirport);
    }

    /** Put an entry for neighbors, stormyTrack and inTransit for the initialized track.
     *  @param track The track.
     *  @exception IllegalActionException If there track is invalid.
     */
    @Override
    public void handleInitializedTrack(NamedObj track)
            throws IllegalActionException {
        int id=-1;
        if(track instanceof Track) {
            id = ((IntToken)( (Track)track).trackId.getToken()).intValue();
        }
        else if(track instanceof NMTrack) {
            id = ((IntToken)( (NMTrack)track).trackId.getToken()).intValue();
        }
       
        if (id == -1) {
            throw new IllegalActionException(
                    "Id of the track " + id + " is invalid (-1)");
        }
        if (_stormyTracks.containsKey(id)) {
            throw new IllegalActionException(
                    "Track with the id " + id + " has been duplicated");
        }
        if (_airportsId.containsKey(id)) {
            throw new IllegalActionException("Track id is same as airport id");
        } else {
            if ((track instanceof Track) && ((Track)track).stormy.getToken() == null) {
                throw new IllegalActionException("Stormy parameter of track "
                        + id + " has not been filled");
            }
            if ((track instanceof NMTrack) && ((NMTrack)track).stormy.getToken() == null) {
                throw new IllegalActionException("Stormy parameter of track "
                        + id + " has not been filled");
            }
          
            if(track instanceof NMTrack)
                _stormyTracks.put(id, ((NMTrack)track).stormy.getToken());
            else if(track instanceof Track)
                _stormyTracks.put(id, ((Track)track).stormy.getToken());
        }

        _inTransit.put(id, false);
        
        if(track instanceof Track)
            _neighbors.put(id, (ArrayToken) ((Track)track).neighbors);
        else if(track instanceof NMTrack)
            _neighbors.put(id, (ArrayToken) ((NMTrack)track).neighbors);
    }

    /** Return an additional delay for a track to keep an aircraft in
     *  transit.
     *  @param track The track
     *  @return An additional delay, or -1.0 to indicate that a rerouting is possible.
     *  @exception IllegalActionException Not thrown in this method.
     */
    @Override
    public double handleRejectionWithDelay(NamedObj track)
            throws IllegalActionException {
        // FIXME: what value should be returned here?
        return 1.0;
    }

    /** Initialize the state of this director.
     *  @exception IllegalActionException If thrown by the parent method.
     */
    @Override
    public void initialize() throws IllegalActionException {
        _stormyTracks = new TreeMap<>();
        _neighbors = new TreeMap<>();
        _inTransit = new TreeMap<>();
        _airportsId = new TreeMap<>();
        _desAirports=new TreeMap<>();
        _airplanesId = new ArrayList<>();
        _airplanesColor = new HashMap<Integer, ArrayToken>();
        super.initialize();
    }

    /** Update _stormyTracks array because of a change in condition of a track.
     *  @param track The track
     *  @exception IllegalActionException If the entry for the track has
     *  not been set in the the stormyTrack array.
     */
    @Override
    public void handleTrackAttributeChanged(NamedObj track)
            throws IllegalActionException {
        int id=-1;
        if(track instanceof Track)
            id = ((IntToken) ((Track)track).trackId.getToken()).intValue();
        else if(track instanceof NMTrack)
            id = ((IntToken) ((NMTrack)track).trackId.getToken()).intValue();
        
        if (_stormyTracks.size() != 0) {
            if (_stormyTracks.containsKey(id)) {
                if(track instanceof Track)
                    _stormyTracks.put(id, ((Track)track).stormy.getToken());
                else if(track instanceof NMTrack)
                    _stormyTracks.put(id, ((NMTrack)track).stormy.getToken());
                
            } else {
                throw new IllegalActionException(
                        "The entry for this track has not been set in stormyTrack array ");
            }
        }
    }





    /** Routing an aircraft based on its flight map. It removes the first entry from the route of the aircraft
     *  @param aircraft (this token is a record of "aircraftId","aircraftSpeed","flightMap" and "priorTrack"and ...)
     *  @param trackId the track id.
     *  @return The routing.
     *  @exception IllegalActionException If there is a routing problem.
     */
    @Override
    public RecordToken routing(Token aircraft, Token trackId)
            throws IllegalActionException {
      
        RecordToken airplane = (RecordToken) aircraft;
        String flightMap = ((StringToken) airplane.get("flightMap")).stringValue();
        int id = ((IntToken) trackId).intValue();
        
        if (Integer.valueOf(flightMap.split(";")[0].split(",")[0].split("/")[0].substring(1))!=((IntToken)trackId).intValue()) {
            throw new IllegalActionException(
                    "There is a mistake in routing: mismatch of track id " + id
                            + " with first element in flight map "
                            + Integer.valueOf(flightMap.split(";")[0].split(",")[0].split("/")[0].substring(1)));
        }
//      if(flightMap.equals("(6/7.0),(9/8.0);(16/9.0),(17/10.0),(18/11.0);(25/11),(26/12),(27/13),(28/14)"))
//          flightMap=flightMap;
         String tempFlighMap=flightMap.replace(";", ",");
         if(tempFlighMap.split(",").length<=1)
             tempFlighMap=flightMap.replace(";", ",");
        int nextTrackInFlight = Integer.valueOf(tempFlighMap.split(",")[1].split("/")[0].substring(1));
        int route = -1;
        if (_neighbors.containsKey(id)) {
            ArrayToken trackNeighbors = _neighbors.get(id);
            for (int i = 0; i < trackNeighbors.length(); i++) {
                if (((IntToken)trackNeighbors.getElement(i)).intValue()==nextTrackInFlight) {
                    route = i;
                    break;
                }
            }
            if (route == -1) {
                throw new IllegalActionException("Mistake in routing of aircraft:"+((IntToken)airplane.get("aircraftId")).intValue()+". track "
                        + id + " has not neighbor track " + nextTrackInFlight +" Route: "+flightMap);
            }
        } else {
            throw new IllegalActionException(
                    "Neighbors of the current track with id " + id
                            + " have not been set.");
        }
        String newFlightMap="";
        if(flightMap.length()>1) {
            String temp=flightMap.replace(";", ",");
            newFlightMap=flightMap.substring(temp.split(",")[0].length()+1);
            }
       
        //creating a new airplane record
        Map<String, Token> newAirplane = new TreeMap<String, Token>();
        newAirplane.put("aircraftId", airplane.get("aircraftId"));
        newAirplane.put("aircraftSpeed", airplane.get("aircraftSpeed"));
        newAirplane.put("flightMap",new StringToken(newFlightMap));
        newAirplane.put("priorTrack", (new IntToken(id)));
        newAirplane.put("fuel", airplane.get("fuel"));
        newAirplane.put("delay", new DoubleToken(1.0));
        newAirplane.put("route", new IntToken(route));
        return (new RecordToken(newAirplane));
    }

    /** Return status of the track.
     *  @param trackId The track Id.
     *  @return The status
     */
    @Override
    public boolean returnTrackStatus(Token trackId) {
        int id = ((IntToken) trackId).intValue();
        return (_inTransit.get(id)
                || ((BooleanToken) _stormyTracks.get(id)).booleanValue());
    }

    /** Update inTransit status of a track.
     *  @param trackId The track id
     *  @param trackStatus The track status
     *  @exception IllegalActionException If thrown while getting the track Id.
     */
    @Override
    public void setInTransitStatusOfTrack(Token trackId, boolean trackStatus)
            throws IllegalActionException {
        int id = ((IntToken) trackId).intValue();
        if (_inTransit.containsKey(id)) {
            _inTransit.put(id, trackStatus);
        } else if (!_airportsId.containsKey(id)) {
            throw new IllegalActionException("There is no track with id " + id);
        }
    }

    

    /** Return a new ATCReceiver.
     *  @return a new ATCReceiver.
     */
    @Override
    public Receiver newReceiver() {
        return new ATCReceiver();
    }

    ///////////////////////////////////////////////////////////////////
    ////                     private fields                        ////

    //private variables which show situation of tracks
    private Random _random = new Random();

    /**  Which track is stormy:first element is
     *  id of the track and last is a boolean token.
     */
    protected Map<Integer, Token> _stormyTracks = new TreeMap<>();

    /**  Neighbors of each track:first element is id of the track and
     *  last is array of its neighbors.
     */
    protected Map<Integer, ArrayToken> _neighbors = new TreeMap<>();

    /** The existance of one aircraft in the track: first element is
     * id and last is a boolean.
     */
    protected Map<Integer, Boolean> _inTransit = new TreeMap<>();

    /** The id of the source or destination airport is the key. For the destination airports
     * the value is null. */
    protected Map<Integer, Airport> _airportsId = new TreeMap();
    
    /**
     * The id of the destination airport is the key.
     */
    protected Map<Integer, DestinationAirport> _desAirports=new TreeMap<>();

    /** The id of the airplane. */
    private ArrayList<Integer> _airplanesId = new ArrayList<>();

    /** A color for each airplane. */
    private Map<Integer, ArrayToken> _airplanesColor = new HashMap<Integer, ArrayToken>();

    @Override
    public Map<String, Token> rerouteUnacceptedAircraft(Token aircraft,int regionId)
            throws IllegalActionException {
        // TODO Auto-generated method stub
        return null;
    }
    
    
}
