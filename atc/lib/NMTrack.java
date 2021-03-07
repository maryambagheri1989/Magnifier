/* A model of a track in air traffic control systems.

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

import java.util.Map;
import java.util.TreeMap;

import ptolemy.actor.Director;
import ptolemy.actor.IOPort;
import ptolemy.actor.NoRoomException;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.util.Time;
import ptolemy.data.ArrayToken;
import ptolemy.data.BooleanToken;
import ptolemy.data.DoubleToken;
import ptolemy.data.IntToken;
import ptolemy.data.RecordToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.RecordType;
import ptolemy.data.type.Type;
import ptolemy.domains.atc.kernel.AbstractATCDirector;
import ptolemy.domains.atc.kernel.Rejecting;
import ptolemy.domains.atc.kernel.policy1.MagnifierDirector;
import ptolemy.domains.atc.kernel.policy1.NoMagnifierDirector;
import ptolemy.domains.atc.lib.ObjectsList.MovingObjectCell;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.StringAttribute;
import ptolemy.vergil.icon.EditorIcon;
import ptolemy.vergil.kernel.attributes.EllipseAttribute;
import ptolemy.vergil.kernel.attributes.ResizablePolygonAttribute;

/** A model of a track in air traffic control systems.
 *  This track can have no more than one aircraft in transit.
 *  If there is one in transit, then it rejects all inputs.
 *  @author Maryam Bagheri
 *  @version $Id$
 *  @since Ptolemy II 11.0
 *  @Pt.ProposedRating Red (cxh)
 *  @Pt.AcceptedRating Red (cxh)
 */
public class NMTrack extends TypedAtomicActor implements Rejecting {

    /** Construct an actor with the given container and name.
     *  @param container The container.
     *  @param name The name of this actor.
     *  @exception IllegalActionException If the actor cannot be contained
     *   by the proposed container.
     *  @exception NameDuplicationException If the container already has an
     *   actor with this name.
     */
    public NMTrack(CompositeEntity container, String name)
            throws IllegalActionException, NameDuplicationException {
        super(container, name);

        input = new TypedIOPort(this, "input", true, false);
        input.setMultiport(true);

        northOutput = new TypedIOPort(this, "northOutput", false, true);
        northOutput.setTypeEquals(new RecordType(_labels, _types));
        StringAttribute cardinality = new StringAttribute(northOutput,
                "_cardinal");
        cardinality.setExpression("NORTH");

        eastOutput = new TypedIOPort(this, "eastOutput", false, true);
        eastOutput.setTypeEquals(new RecordType(_labels, _types));
        cardinality = new StringAttribute(eastOutput, "_cardinal");
        cardinality.setExpression("EAST");

        southOutput = new TypedIOPort(this, "southOutput", false, true);
        southOutput.setTypeEquals(new RecordType(_labels, _types));
        cardinality = new StringAttribute(southOutput, "_cardinal");
        cardinality.setExpression("SOUTH");

        trackId = new Parameter(this, "trackId");
        trackId.setTypeEquals(BaseType.INT);
        String n=name.replace("Track", "");
        if(n.equals(""))
            trackId.setExpression("1");
        else {
            trackId.setExpression(n);
        }
        trackId.setTypeEquals(BaseType.INT);
        
//        regionId= new Parameter(this, "regionId");
//        regionId.setDisplayName("regionId, started from 1");
//        regionId.setTypeEquals(BaseType.INT);
//        regionId.setExpression("-1");
        
        border=new Parameter(this, "border");
        border.setDisplayName("e.g. {N3, E5} or EMPTY");
        border.setExpression("");
        border.setTypeEquals(new ArrayType(BaseType.STRING));
        
//        neighbors = new Parameter(this, "neighbors");
//        neighbors.setDisplayName("neighbors{North,East,South");
//        neighbors.setExpression("{-1,-1,-1}");
//        neighbors.setTypeEquals(new ArrayType(BaseType.INT));

        stormy = new Parameter(this, "stormy");
        stormy.setTypeEquals(BaseType.BOOLEAN);
        stormy.setExpression("false");
        
        connectedSourceA= new Parameter(this, "connectedSourceAirport");
        connectedSourceA.setTypeEquals(BaseType.INT);

        _attachText("_iconDescription",
                "<svg> <path d=\"M 194.67321,2.8421709e-14 L 70.641958,53.625 "
                        + "C 60.259688,46.70393 36.441378,32.34961 31.736508,30.17602 C -7.7035221,11.95523 "
                        + "-5.2088921,44.90709 11.387258,54.78122 C 15.926428,57.48187 39.110778,71.95945 "
                        + "54.860708,81.15624 L 72.766958,215.09374 L 94.985708,228.24999 L 106.51696,107.31249 "
                        + "L 178.04821,143.99999 L 181.89196,183.21874 L 196.42321,191.84374 L 207.51696,149.43749 "
                        + "L 207.64196,149.49999 L 238.45446,117.96874 L 223.57946,109.96874 L 187.95446,126.87499 "
                        + "L 119.67321,84.43749 L 217.36071,12.25 L 194.67321,2.8421709e-14 z\" "
                        + "style=\"fill:#000000;fill-opacity:1;fill-rule:evenodd;stroke:none;stroke-width:1px;stroke-linecap:butt;"
                        + "stroke-linejoin:miter;stroke-opacity:1\" id=\"path5724\"/></svg>");

        // Create an icon for this sensor node.
        EditorIcon node_icon = new EditorIcon(this, "_icon");

        _circle = new EllipseAttribute(node_icon, "_circleShap");
        _circle.centered.setToken("true");
        _circle.width.setToken("40");
        _circle.height.setToken("40");
        _circle.fillColor.setToken("{0.0, 0.0, 0.0, 0.0}");
        _circle.lineColor.setToken("{0.0, 0.0, 0.0, 0.0}");

        _shape = new ResizablePolygonAttribute(node_icon, "_trackShape");
        _shape.centered.setToken("true");
        _shape.width.setToken("40");
        _shape.height.setToken("40");
        _shape.vertices
                .setExpression("{194.67321,2.8421709e-14, 70.641958,53.625, "
                        + "60.259688,46.70393, 36.441378,32.34961, 31.736508,30.17602, -7.7035221,11.95523, "
                        + "-5.2088921,44.90709, 11.387258,54.78122, 15.926428,57.48187, 39.110778,71.95945, "
                        + "54.860708,81.15624, 72.766958,215.09374, 94.985708,228.24999, 106.51696,107.31249, "
                        + "178.04821,143.99999, 181.89196,183.21874, 196.42321,191.84374, 207.51696,149.43749, "
                        + "207.64196,149.49999, 238.45446,117.96874, 223.57946,109.96874, 187.95446,126.87499, "
                        + "119.67321,84.43749, 217.36071,12.25, 194.67321,2.8421709e-14}");
        _shape.fillColor.setToken("{1.0, 1.0, 1.0, 1.0}");
    }

    /** The input, which is a multiport. */
    public TypedIOPort input;

    /** The north output. */
    public TypedIOPort northOutput;

    /** The east output. */
    public TypedIOPort eastOutput;

    /** The south output. */
    public TypedIOPort southOutput;
    //westOutputToPrior;

    /** The trackId. The initial default is an integer with a value of
     * -1.
     */
    public Parameter trackId;
    
    /**
     * The region Id is the identifier of the region containing the track.
     */
//    public Parameter regionId;
    
    /**
     * Whether the track is placed in a border? 
     */
    public Parameter border;

    /** The neighbors.  The initial value is an array with
     *  values {-1, -1, -1}.
     */
//    public Parameter neighbors;

    /** A boolean indicating if it is stormy. */
    public Parameter stormy;
    
    /** Which source airport is connected to this track*/
    public Parameter connectedSourceA;

    /** Return true if the token cannot be accepted at the specified port.
     *  @param token The token that may be rejected.
     *  @param port The port.
     *  @return True to reject the token.
     */
    @Override
    public boolean reject(Token token, IOPort port) {
        boolean unAvailable = (_inTransit != null
                || ((BooleanToken) _isStormy).booleanValue());
        if (unAvailable == true) {
            return true;
        }
        if (_called == false) {
            _called = true;
            return (_inTransit != null
                    || ((BooleanToken) _isStormy).booleanValue());
        } else {
            return true;
        }
    }

    /** If the specified attribute is <i>stormy</i> and there is an
     *  open file being written, then close that file.  The new file will
     *  be opened or created when it is next written to.
     *  @param attribute The attribute that has changed.
     *  @exception IllegalActionException If the specified attribute
     *   is <i>fileName</i> and the previously
     *   opened file cannot be closed.
     */
    @Override
    public void attributeChanged(Attribute attribute)
            throws IllegalActionException {
        Director director = getDirector();
        if (attribute == stormy) {
            if (stormy.getToken() != null) {
                _isStormy = stormy.getToken();
                //change color of the storm zone
                if (((BooleanToken) _isStormy).booleanValue() == true) {
                    _circle.fillColor.setToken("{1.0,0.2,0.2,1.0}");
                } else {
                    _circle.fillColor.setToken("{0.0, 0.0, 0.0, 0.0}");
                }
                //
                ((AbstractATCDirector) director)
                        .handleTrackAttributeChanged(this);
            }
        } else {
            super.attributeChanged(attribute);
        }
    }

    @Override
    public void declareDelayDependency() throws IllegalActionException {
        _declareDelayDependency(input, northOutput, 0.0);
        _declareDelayDependency(input, eastOutput, 0.0);
        _declareDelayDependency(input, southOutput, 0.0);
    }

    @Override
    public void fire() throws IllegalActionException {
        super.fire();
        Time currentTime = _director.getModelTime();
        
        /** Send to the destination. 
         * remove from underAnalysisAircraft when the aircraft departs from the destination airport
         */
        
        int nextTrack=-1;
        int destinationRegionId=0;
        int planeId=-1;
        if(_inTransit!=null) {
            nextTrack=Integer.valueOf(((StringToken)((RecordToken)_inTransit).get("flightMap")).stringValue().split(";")[0].split(",")[0].split("/")[0].substring(1));
            planeId=((IntToken)((RecordToken) _inTransit).get("aircraftId")).intValue();
//            if(planeId==78)
//                System.out.println("air");
        }
        
     
        
        if(!((NoMagnifierDirector)_director).nextIsAirport(nextTrack))
            
            if(border.getToken()!=null && currentTime.equals(_transitExpires) && _inTransit != null) {
                if(!((NoMagnifierDirector)_director).isInTheRegion(((IntToken)this.regionId).intValue(),nextTrack)) {
                // if the track is on border and the next track is in the next region
                destinationRegionId=0;
                switch(_OutRoute) {
                    case 0:
                    {
                        IOPort port=(IOPort)northOutput.connectedPortList().get(0);
                        destinationRegionId=((IntToken)(((NMTrack)port.getContainer()).regionId)).intValue();
                        break;
                    }
                    case 1:
                    {
                        IOPort port=(IOPort)eastOutput.connectedPortList().get(0);
                        destinationRegionId=((IntToken)(((NMTrack)port.getContainer()).regionId)).intValue();
                        break;
                    }
                    case 2:
                    {
                        IOPort port=(IOPort)southOutput.connectedPortList().get(0);
                        destinationRegionId=((IntToken)(((NMTrack)port.getContainer()).regionId)).intValue();
                    }
                }
                
//                if(destinationRegionId==2)
//                    System.out.println("Hi");
                
                if(!((NoMagnifierDirector)_director).getRegionStatus(destinationRegionId)
                        && !((NoMagnifierDirector)_director).getRegionStatus(((IntToken)this.regionId).intValue())) {
                    // Send from a non-affected area to a non-affected area
                    // if it is successfully done, add the aircraft to isAircraftArrived of the target region
                    // the send should be successfully done.
                    ((NoMagnifierDirector)_director).setInRegionAirArrived(planeId,destinationRegionId);
                    try {
                        if (_OutRoute == 0) {
                            northOutput.send(0, _inTransit);
                        } else if (_OutRoute == 1) {
                            eastOutput.send(0, _inTransit);
                        } else {
                            southOutput.send(0, _inTransit);
                        }
                        _setIcon(-1);
                        _inTransit = null;
                        _called = false;
                        _transitExpires=null;
                        ((AbstractATCDirector) _director).setInTransitStatusOfTrack(_id, false);
                        return;
                    } catch (NoRoomException ex) {
                        throw new IllegalActionException(this, "send of "+planeId+" should be successfully performed "+((NoMagnifierDirector)_director).getCurrentState() );
                    }
                }
                else if(!((NoMagnifierDirector)_director).getRegionStatus(destinationRegionId)
                        && ((NoMagnifierDirector)_director).getRegionStatus(((IntToken)this.regionId).intValue())) {
                    
                    /** Send from an affected area to a non-affected area
                     * if it arrives at its pre-specified time and track and the send is successfully done,
                     * add it to isAircraftArrived of the target region, and removes it from under analysis
                     * moving objects (send in this case should be successfully done). 
                     * if it does not arrive at its prespecified time, the change propagates forward and add 
                     * the aircraft in the target region to the under analysis aircraft. Also add the target region
                     * to under analysis region. 
                     * 
                     */
                    planeId=((IntToken)((RecordToken) _inTransit).get("aircraftId")).intValue();
                    int regionId=((IntToken)this.regionId).intValue();
                    mainDepartureTrack=((NoMagnifierDirector)_director).getDepartureTrack(planeId, regionId);
                    mainTimeToSendM=((NoMagnifierDirector)_director).getDepartureTime(planeId, regionId);
                    boolean flag=true;
                    if(mainDepartureTrack==-1 && mainTimeToSendM==-1)
                    {
                        // The moving object was not supposed to travel through this region
                        // but it is supposed to travel through the destinationRegionId if route is not empty
                        String route=((NoMagnifierDirector)_director).getRoute(planeId, destinationRegionId);
                        if(!route.equals("")) {
                            String[] entranceTrack=route.replaceAll(";", ",").split(",")[0].split("/");
                        //    String[] entranceTrack=route.split(",")[0].split("/");
                            int mainArrivalTrack=Integer.valueOf(entranceTrack[0].substring(1));
                            double mainArrivalTime=Double.valueOf(entranceTrack[1].substring(0,entranceTrack[1].length()-1));
                            if(nextTrack!=mainArrivalTrack || currentTime.getDoubleValue()!=mainArrivalTime)
                                flag=false;// Forward change propagation
                        }
                        else
                            flag=false;
                    }
                     if(flag==false || (flag==true && mainDepartureTrack!=-1 && mainTimeToSendM!=-1 && 
                             (currentTime.compareTo(new Time(_director, mainTimeToSendM))!=0 ||  nextTrack!=mainDepartureTrack))) {
                         // The moving object is not sent based on its schedule
                         try {
                             ((NoMagnifierDirector)_director).handleChangPropagation(this, destinationRegionId);
                         } catch (NumberFormatException e) {
                             // TODO Auto-generated catch block
                             e.printStackTrace();
                         }
                         _director.fireAt(this, currentTime);
                         return;
                     }
                     else if((flag==true && mainDepartureTrack==-1 && mainTimeToSendM==-1) || (flag==true && currentTime.compareTo(new Time(_director, mainTimeToSendM))==0 && nextTrack==mainDepartureTrack)) {
                        // It seems that the aircraft is sent successfully, and does not need
                        // to activate the next region. 
                         ((NoMagnifierDirector)_director).setInRegionAirArrived(planeId,destinationRegionId);
                         try{
                         if (_OutRoute == 0) {
                             northOutput.send(0, _inTransit);
                         } else if (_OutRoute == 1) {
                             eastOutput.send(0, _inTransit);
                         } else {
                             southOutput.send(0, _inTransit);
                         }
                         _setIcon(-1);
                         _inTransit = null;
                         _called = false;
                         _transitExpires=null;
                         ((AbstractATCDirector) _director).setInTransitStatusOfTrack(_id, false);
                         return;
                     } catch (NoRoomException ex) {
                         throw new IllegalActionException(this, "send of "+planeId+" should be successfully performed");
                     }
                         
                     }
  
                }
                
                else if(((NoMagnifierDirector)_director).getRegionStatus(destinationRegionId)
                        && !((NoMagnifierDirector)_director).getRegionStatus(((IntToken)this.regionId).intValue())) {
                    
                    /** Send from a non-affected area to an affected area
                     * if it is successfully done, put the aircraft to the aircraft under analysis, also 
                     * add the aircraft to isAircraftArrived of the target region
                     * if the send is not successful, the change  propagates backward,
                     * Therefore, put this area under analysis, put the aircraft in the current region under
                     * analysis. 
                     */
                    try {
                        if (_OutRoute == 0) {
                            northOutput.send(0, _inTransit);
                        } else if (_OutRoute == 1) {
                            eastOutput.send(0, _inTransit);
                        } else {
                            southOutput.send(0, _inTransit);
                        }
                        _setIcon(-1);
                    } catch (NoRoomException ex) {
                        
                        // Token rejected by the destination.
                        if (!(_director instanceof AbstractATCDirector)) {
                            throw new IllegalActionException(this,
                                    "Track must be used with an ATCDirector.");
                        }
                        
                        Map<String, Token> temp = new TreeMap<>();
                            
                        temp = ((AbstractATCDirector) _director)
                                .rerouteUnacceptedAircraft(_inTransit,((IntToken)regionId).intValue());
                        
                        Map<String, Token> newAircraft = new TreeMap<String, Token>();
                        newAircraft.put("aircraftId",
                                ((RecordToken) _inTransit).get("aircraftId"));
                        newAircraft.put("aircraftSpeed",
                                ((RecordToken) _inTransit).get("aircraftSpeed"));
                        newAircraft.put("flightMap", temp.get("flightMap"));
                        newAircraft.put("priorTrack",
                                ((RecordToken) _inTransit).get("priorTrack"));
                        newAircraft.put("fuel",
                                ((RecordToken) _inTransit).get("fuel"));
                        RecordToken transmitedAircraft = new RecordToken(newAircraft);
                        
                        if (((IntToken) temp.get("route")).intValue() == -1) { // Stay in track
                            double additionalDelay = ((AbstractATCDirector) _director)
                                    .handleRejectionWithDelay(this);
                            if (additionalDelay < 0.0) {
                                throw new IllegalActionException(this,
                                        "Unable to handle rejection.");
                            }
                            _transitExpires = _transitExpires.add(additionalDelay);
                            _inTransit =((NoMagnifierDirector)_director).createNewMObject(transmitedAircraft, additionalDelay);
                        } 
                        else {// Send airplane through another route
                            _transitExpires = _transitExpires.add(
                                    ((DoubleToken) temp.get("delay")).doubleValue());
                            _OutRoute = ((IntToken) temp.get("route")).intValue();
                            _inTransit=((NoMagnifierDirector)_director).createNewMObject(transmitedAircraft, ((DoubleToken) temp.get("delay")).doubleValue());
                        } //end of else
                   
                        _director.fireAt(this, _transitExpires);
                        ((NoMagnifierDirector)_director).handleChangPropagation(this, ((IntToken)this.regionId).intValue());
                        return;
                    } //End of catch
                    // Token has been sent successfully
                    _inTransit = null;
                    _called = false;
                    _transitExpires=null;
                    ((AbstractATCDirector) _director).setInTransitStatusOfTrack(_id,
                            false);
                    ((NoMagnifierDirector)_director).setInRegionAirArrived(planeId,destinationRegionId);
                    ((NoMagnifierDirector)_director).addToUnderAnalysisMO(planeId);
                }
                
                }
            }
     // Send from an affected area to an affected area or an internal send
        if (_transitExpires!=null && currentTime.equals(_transitExpires) && _inTransit != null) {
            try {
                if (_OutRoute == 0) {
                    northOutput.send(0, _inTransit);
                } else if (_OutRoute == 1) {
                    eastOutput.send(0, _inTransit);
                } else {
                    southOutput.send(0, _inTransit);
                }
                _setIcon(-1);
            } catch (NoRoomException ex) {
                
                // Token rejected by the destination.
                if (!(_director instanceof AbstractATCDirector)) {
                    throw new IllegalActionException(this,
                            "Track must be used with an ATCDirector.");
                }
                
                Map<String, Token> temp = new TreeMap<>();
                    
                temp = ((AbstractATCDirector) _director)
                        .rerouteUnacceptedAircraft(_inTransit,((IntToken)regionId).intValue());
                
                Map<String, Token> newAircraft = new TreeMap<String, Token>();
                newAircraft.put("aircraftId",
                        ((RecordToken) _inTransit).get("aircraftId"));
                newAircraft.put("aircraftSpeed",
                        ((RecordToken) _inTransit).get("aircraftSpeed"));
                newAircraft.put("flightMap", temp.get("flightMap"));
                newAircraft.put("priorTrack",
                        ((RecordToken) _inTransit).get("priorTrack"));
                newAircraft.put("fuel",
                        ((RecordToken) _inTransit).get("fuel"));
                RecordToken transmitedAircraft = new RecordToken(newAircraft);
                
                if (((IntToken) temp.get("route")).intValue() == -1) { // Stay in track
                    double additionalDelay = ((AbstractATCDirector) _director)
                            .handleRejectionWithDelay(this);
                    if (additionalDelay < 0.0) {
                        throw new IllegalActionException(this,
                                "Unable to handle rejection.");
                    }
                    _transitExpires = _transitExpires.add(additionalDelay);
                    _inTransit =((NoMagnifierDirector)_director).createNewMObject(transmitedAircraft, additionalDelay);
                } 
                else {// Send airplane through another route
                    _transitExpires = _transitExpires.add(
                            ((DoubleToken) temp.get("delay")).doubleValue());
                    _OutRoute = ((IntToken) temp.get("route")).intValue();
                    _inTransit=((NoMagnifierDirector)_director).createNewMObject(transmitedAircraft, ((DoubleToken) temp.get("delay")).doubleValue());
                } //end of else
           
                _director.fireAt(this, _transitExpires);
                return;
            } //End of catch
            // Token has been sent successfully
            _inTransit = null;
            _called = false;
            _transitExpires=null;
            ((AbstractATCDirector) _director).setInTransitStatusOfTrack(_id,
                    false);
        }

        // Handle any input that have been accepted.
        for (int i = 0; i < input.getWidth(); i++) {
            if (input.hasNewToken(i)) {
                // This if is for chacking safety. Instead of throwing exception we can write a record to the file.
                if (_inTransit != null) {
                    throw new IllegalActionException(
                            "two airplanes in one track");
                }
                //
                Token inputAircraft = input.get(i);
                
                //From source airport to an affected area
               if(((NoMagnifierDirector)_director).isAirport(((IntToken)((RecordToken) inputAircraft).get("priorTrack")).intValue()))
               {
                   if(((NoMagnifierDirector)_director).getRegionStatus(((IntToken)regionId).intValue()))
                       ((NoMagnifierDirector)_director).addToUnderAnalysisMO(((IntToken)((RecordToken) inputAircraft).get("aircraftId")).intValue());
               }
                     

                _setIcon(((IntToken)((RecordToken) inputAircraft)
                      .get("aircraftId")).intValue());
                
                ((AbstractATCDirector) _director).setInTransitStatusOfTrack(_id,
                        true);
                
                RecordToken aircraftWithInformation = ((AbstractATCDirector) _director)
                        .routing(inputAircraft, _id);
                _transitExpires = currentTime.add(
                        ((DoubleToken) aircraftWithInformation.get("delay"))
                                .doubleValue());
                _OutRoute = ((IntToken) aircraftWithInformation.get("route"))
                        .intValue();

                //creating a new aircraft to sent to output from aircraftWighInformation
                
                _inTransit = ((NoMagnifierDirector)_director).createNewMObject(aircraftWithInformation, 1.0);
                _director.fireAt(this, _transitExpires);

            }
        }

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
        generator=false;
        movingObjectsList=new ObjectsList();
        _director = getDirector();
        _inTransit = null;
        mainTimeToSendM=-1;
        _OutRoute = -1;
        _id = trackId.getToken();
        _isStormy = stormy.getToken();
        neighbors=((NoMagnifierDirector)_director).findNeighbors(((IntToken)_id).intValue());
        regionId=new IntToken(((NoMagnifierDirector)_director).findRegionofTrack(((IntToken)_id).intValue()));
        ((AbstractATCDirector) _director).handleInitializedTrack(this);
        _called = false;
        _setIcon(-1);
        mObjectListHead=null;
    }
    
    public void cleanTrack() throws IllegalActionException{
        _inTransit = null;
        generator=false;
        _transitExpires=null;
        mainTimeToSendM=-1;
        _OutRoute = -1;
        _called = false;
        //mObjectListHead=null; should not null this here
        _setIcon(-1);
        ((AbstractATCDirector) _director).setInTransitStatusOfTrack(_id,
                false);
    }

    /** Set the visual indication of the icon for the specified ID.
     *  @param id The aircraft ID or -1 to indicate no aircraft.
     *  @exception IllegalActionException
     */
    protected void _setIcon(int id) throws IllegalActionException {
        ArrayToken color = _noAircraftColor;
        if (id > -1) {
            color = ((AbstractATCDirector) _director).handleAirplaneColor(id);
        }
        _shape.fillColor.setToken(color);
    }
    
    public boolean equal(int e) throws IllegalActionException {
        if(((IntToken)this.trackId.getToken()).intValue()==e)
            return true;
        return false;
    }
    

    public Token neighbors;
    public Token regionId;
    private EllipseAttribute _circle;
    private ResizablePolygonAttribute _shape;
    private DoubleToken _one = new DoubleToken(1.0);
    private Token[] _white = { _one, _one, _one, _one };
    private ArrayToken _noAircraftColor = new ArrayToken(_white);
    public Token _inTransit;
    /** The mode of an actor: if it is true then the actor has to generate inputs.*/
    public boolean generator; 
    /** The list of moving objects that are generated through this track.*/
    public ObjectsList movingObjectsList;
    /** The main time that a message has to be sent out.*/
    public double mainTimeToSendM;
    /** The track which the aircraft is supposed to enter into in the next region*/
    public int mainDepartureTrack;
    public Time _transitExpires;
    private Token _isStormy;
    public int _OutRoute;
    public Token _id;
    private Director _director;
    public boolean _called;
    public MovingObjectCell mObjectListHead;
    private String[] _labels = { "aircraftId", "aircraftSpeed", "flightMap",
            "priorTrack", "fuel" };
    private Type[] _types = { BaseType.INT, BaseType.INT,
           BaseType.STRING, BaseType.INT, BaseType.DOUBLE };


}
