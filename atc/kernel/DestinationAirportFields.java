package ptolemy.domains.atc.kernel;

import ptolemy.actor.util.Time;
import ptolemy.data.IntToken;
import ptolemy.data.RecordToken;
import ptolemy.data.Token;

public class DestinationAirportFields {
    public DestinationAirportFields(Token _inTransit, Time _transitExpires, boolean _called) {
        super();
        this._inTransit = _inTransit;
        this._transitExpires = _transitExpires;
        this._called = _called;
    }
    public Token _inTransit;
    public Time _transitExpires;
    public boolean _called;
    
    
//    @Override
//    public int hashCode() {
//        final int prime = 31;
//        int result = 1;
//        result = prime * result + (_called ? 1231 : 1237);
//        result = prime * result + ((_inTransit == null) ? 0 : ((IntToken)((RecordToken)_inTransit).get("aircraftId")).intValue());
//        result = prime * result + ((_transitExpires == null) ? 0 : _transitExpires.hashCode());
//        return result;
//    }


    @Override
    public boolean equals(Object e) {
        if(e instanceof DestinationAirportFields) {
            
            if((((DestinationAirportFields) e)._inTransit!=null && this._inTransit==null)||
                    (((DestinationAirportFields) e)._inTransit==null && this._inTransit!=null))
                return false;
            if((((DestinationAirportFields) e)._inTransit!=null && this._inTransit!=null)
                    && !((DestinationAirportFields) e)._inTransit.equals(this._inTransit))
                    return false;
//                {
//                if(((IntToken)(((RecordToken)((DestinationAirportFields) e)._inTransit).get("aircraftId"))).intValue()!=((IntToken)((RecordToken)this._inTransit).get("aircraftId")).intValue())
//                    return false;
//                if(!((RecordToken)((DestinationAirportFields) e)._inTransit).get("flightMap").equals(((RecordToken)this._inTransit).get("flightMap")))
//                    return false;
//                }
                    

            if((((DestinationAirportFields) e)._transitExpires!=null && this._transitExpires==null)
                    ||(((DestinationAirportFields) e)._transitExpires==null && this._transitExpires!=null))
                return false;
            if((((DestinationAirportFields) e)._transitExpires!=null && this._transitExpires!=null) &&
                    !((DestinationAirportFields) e)._transitExpires.equals(this._transitExpires))
                return false;
            if(((DestinationAirportFields) e)._called!=this._called)
                return false;
        }
        return true;
    }

    
}
