/* A model of a source airport in air traffic control systems.

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
package ptolemy.domains.atc.lib;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import ptolemy.actor.Director;
import ptolemy.actor.NoRoomException;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.util.Time;
import ptolemy.data.ArrayToken;
import ptolemy.data.DoubleToken;
import ptolemy.data.IntToken;
import ptolemy.data.RecordToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.domains.atc.kernel.AbstractATCDirector;
import ptolemy.domains.atc.kernel.policy1.MagnifierDirector;
import ptolemy.domains.atc.kernel.policy1.NoMagnifierDirector;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/** This actor receives a record token which shows an airplane decides to fly.
 * Therefore, this actor just sends that to a proper direction based on the neighbors (of the airport)
 * If the destination track (first track in the airplane's flight map is unavailable,
 * then airport try to send it after a period of time.
 *  @author Maryam Bagheri
 *  @version $Id$
 *  @since Ptolemy II 11.0
 *  @Pt.ProposedRating Red (cxh)
 *  @Pt.AcceptedRating Red (cxh)
 */
public class Airport extends TypedAtomicActor {

    /** Construct an actor with the given container and name.
     *  @param container The container.
     *  @param name The name of this actor.
     *  @exception IllegalActionException If the actor cannot be contained
     *   by the proposed container.
     *  @exception NameDuplicationException If the container already has an
     *   actor with this name.
     */
    public Airport(CompositeEntity container, String name)
            throws IllegalActionException, NameDuplicationException {
        super(container, name);

        //input = new TypedIOPort(this, "input", true, false);
        //input.setTypeEquals(BaseType.RECORD);

        output = new TypedIOPort(this, "output", false, true);
        output.setTypeEquals(BaseType.RECORD);
        output.setMultiport(true);

        delay = new Parameter(this, "delay");
        delay.setTypeEquals(BaseType.DOUBLE);
        delay.setExpression("1");

        takeOff = new Parameter(this, "takeOff");
        takeOff.setTypeEquals(BaseType.DOUBLE);
        takeOff.setExpression("1");

        airportId = new Parameter(this, "airportId");
        airportId.setTypeEquals(BaseType.INT);
        String n=name.replace("Airport", "");
        int number=0;
        if(n.equals(""))
           number=1;
        else 
           number=Integer.valueOf(n);
        Director dir=getDirector();
        
        if(dir instanceof MagnifierDirector) {
            number+=((MagnifierDirector)dir).returnDimention()*((MagnifierDirector)dir).returnDimention();
        }
        else if (dir instanceof NoMagnifierDirector) {
            number+=((NoMagnifierDirector)dir).returnDimention()*((NoMagnifierDirector)dir).returnDimention();
        }
        
            
        n=String.valueOf(number);
        airportId.setExpression(n);

        connectedTracks = new Parameter(this, "connectedTracks");
        connectedTracks.setExpression("{}");
        connectedTracks.setTypeEquals(new ArrayType(BaseType.INT));

    }

    /** The input port, which is of type record token. */
    //public TypedIOPort input;

    /** The output port, which is of type record token. */
    public TypedIOPort output;

    /** An array indicating the connected tracks. */
    public Parameter connectedTracks;

    /** The delay. */
    public Parameter delay;

    /** The airport Id. */
    public Parameter airportId;

    /** A double with the initial default value of 1. */
    public Parameter takeOff;

    /** Fire the actor.
     *  @exception IllegalActionException If thrown by the baseclass
     *  or if there is a problem accessing the ports or parameters.
     */
    @Override
    public void fire() throws IllegalActionException {
        super.fire();
        Time currentTime = _director.getModelTime();
        if (currentTime.equals(_transitExpires) && _inTransit != null) {
            try {
                //((IntToken)(((RecordToken)_inTransit).get("aircraftId"))).intValue() 
                int i = _findDirection((RecordToken)_inTransit);
                output.send(i, _inTransit);
                _inTransit = null;
            } catch (NoRoomException ex) {
                double additionalDelay = ((DoubleToken) delay.getToken())
                        .doubleValue();
                if (additionalDelay < 0.0) {
                    throw new IllegalActionException(this,
                            "Unable to handle rejection.");
                }
                _transitExpires = _transitExpires.add(additionalDelay);
                _director.fireAt(this, _transitExpires);
            }

            if (_inTransit == null && _airplanes.size() != 0) {
                Map<String, Token> aircraft = new TreeMap<String, Token>();
                aircraft.put("aircraftId", ((RecordToken)_airplanes.get(0)).get("aircraftId"));
                aircraft.put("aircraftSpeed", ((RecordToken)_airplanes.get(0)).get("aircraftSpeed"));
                aircraft.put("flightMap", ((RecordToken)_airplanes.get(0)).get("flightMap"));
                aircraft.put("priorTrack",((RecordToken)_airplanes.get(0)).get("priorTrack"));
                aircraft.put("fuel", ((RecordToken)_airplanes.get(0)).get("fuel"));
                _inTransit=new RecordToken(aircraft);
                destActor=findDest(_inTransit);
                
                if(currentTime.getDoubleValue()>(((DoubleToken)((RecordToken)_airplanes.get(0)).get("departureTime"))).doubleValue())
                {
                    _transitExpires=currentTime;
                }
                else {
                    _transitExpires=new Time(_director,(((DoubleToken)((RecordToken)_airplanes.get(0)).get("departureTime"))).doubleValue());
                }
                 _airplanes.remove(0);
                _director.fireAt(this, _transitExpires);
//                if(_director instanceof MagnifierDirector)
//                    ((MagnifierDirector)_director).addToUnderAnalysisMO(((IntToken)((RecordToken) _inTransit).get("aircraftId")).intValue());
            }
            return;
        }
        
        double additionalDelay = ((DoubleToken) takeOff.getToken())
                .doubleValue();
        if (additionalDelay < 0.0) {
            throw new IllegalActionException(this,
                    "Delay is negative in airport.");
        }
        
        _inTransit = _airplanes.get(0);
        destActor=findDest(_inTransit);
        _transitExpires = currentTime.add(additionalDelay);
        _director.fireAt(this, _transitExpires);
        
      }

    private int findDest(Token inTransit) {
        // TODO Auto-generated method stub
        RecordToken airplane=(RecordToken)(inTransit);
        String flightMap = ((StringToken)airplane.get("flightMap")).stringValue();
        flightMap.replaceAll(";", ",");
        int trackId=Integer.valueOf(flightMap.split(",")[0].split("/")[0].substring(1));
        return trackId;
    }

    /** Initialize this actor.  Derived classes override this method
     *  to perform actions that should occur once at the beginning of
     *  an execution, but after type resolution.  Derived classes can
     *  produce output data and schedule events.
     *  @exception IllegalActionException If a derived class throws it.
     */
    @Override
    public void initialize() throws IllegalActionException {
        super.initialize();
        _director = getDirector();
        _airplanes=new ArrayList<>();
        _transitExpires=null;
        ((AbstractATCDirector) _director).handleInitializedAirport(this);
        _inTransit = null;
        _Tracks = (ArrayToken) connectedTracks.getToken();
        destActor=-1;
        if (_Tracks.length() == 0) {
            throw new IllegalActionException(
                    "there is no connected track to the airport in the airport's parameters ");
        }
    }


    
    public void putMovingObject(Map<String, Token> airplane) {
        try {
            _airplanes.add(new RecordToken(airplane));
            if(_inTransit==null) {
                Map<String, Token> aircraft = new TreeMap<String, Token>();
                aircraft.put("aircraftId", ((RecordToken)_airplanes.get(0)).get("aircraftId"));
                aircraft.put("aircraftSpeed", ((RecordToken)_airplanes.get(0)).get("aircraftSpeed"));
                aircraft.put("flightMap", ((RecordToken)_airplanes.get(0)).get("flightMap"));
                aircraft.put("priorTrack",((RecordToken)_airplanes.get(0)).get("priorTrack"));
                aircraft.put("fuel", ((RecordToken)_airplanes.get(0)).get("fuel"));
                _inTransit=new RecordToken(aircraft);
                destActor=findDest(_inTransit);
                _transitExpires=new Time(_director,(((DoubleToken)((RecordToken)_airplanes.get(0)).get("departureTime"))).doubleValue());
                _director.fireAt(this, _transitExpires);
                _airplanes.remove(0);
//                if(_director instanceof MagnifierDirector)
//                    ((MagnifierDirector)_director).addToUnderAnalysisMO(((IntToken)((RecordToken) _inTransit).get("aircraftId")).intValue());
            }
        } catch (IllegalActionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    

    private int _findDirection(RecordToken airplane)
            throws IllegalActionException {
        String flightMap = ((StringToken)airplane.get("flightMap")).stringValue();
        flightMap.replaceAll(";", ",");
        int trackId=Integer.valueOf(flightMap.split(",")[0].split("/")[0].substring(1));
        boolean finded = false;
        for (int i = 0; i < _Tracks.length(); i++) {
            if (trackId==((IntToken)_Tracks.getElement(i)).intValue()) {
                finded = true;
                return i;
            }
        }
        throw new IllegalActionException(
                "There is no route from the airport to the first track in flightMap");
    }

    public Token _inTransit;
    public Time _transitExpires;
    private Director _director;
    private ArrayToken _Tracks;
    public ArrayList<RecordToken> _airplanes;
    
    public int destActor;
}
