package ptolemy.domains.atc.kernel;

import java.util.ArrayList;

import ptolemy.actor.util.Time;
import ptolemy.data.IntToken;
import ptolemy.data.RecordToken;
import ptolemy.data.Token;

public class AirportFeilds {
    public AirportFeilds(ArrayList<RecordToken> _airplanes, Token _inTransit, Time _transitExpires) {
        super();
        this._airplanes=new ArrayList<RecordToken>();
        for(int i=0;i<_airplanes.size();i++)
            this._airplanes.add(_airplanes.get(i));
        this._inTransit = _inTransit;
        this._transitExpires = _transitExpires;
    }
    public ArrayList<RecordToken> _airplanes;
    public Token _inTransit;
    public Time _transitExpires;
    


    @Override
    public boolean equals(Object e) {
        if(e instanceof AirportFeilds) {
            if((((AirportFeilds) e)._inTransit!=null && this._inTransit==null)||
                    (((AirportFeilds) e)._inTransit==null && this._inTransit!=null))
                return false;
            if((((AirportFeilds) e)._inTransit!=null && this._inTransit!=null) &&
                    !((AirportFeilds) e)._inTransit.equals(this._inTransit))
              return false;
               

            if((((AirportFeilds) e)._transitExpires!=null && this._transitExpires==null)
                    ||(((AirportFeilds) e)._transitExpires==null && this._transitExpires!=null))
                return false;
            if((((AirportFeilds) e)._transitExpires!=null && this._transitExpires!=null) &&
                    !((AirportFeilds) e)._transitExpires.equals(this._transitExpires))
                return false;
            if(this._airplanes.size()!= ((AirportFeilds) e)._airplanes.size())
                return false;
            for(int i=0;i<this._airplanes.size();i++)
                if(!((AirportFeilds) e)._airplanes.contains(this._airplanes.get(i)))
                    return false;
//            if(!this._airplanes.equals(((AirportFeilds) e)._airplanes))
//                return false;
        }
        return true;
    }
    
    
    
}
