
/**
 * This class determines different kinds of chains... A chain can be changed from type T0 to 
 * T2 and vice versa.
 */
package ptolemy.domains.atc.lib;

import java.util.ArrayList;

public class ChainClass {

    public int areaId;
    
    public ArrayList<ArrayList> chainT0; //This type includes the internal chains 
    public ArrayList<ArrayList> chainT1; //This type includes the chains whose last element 
    // refers to a boundary actor, and the last element is also dependent.
    public ArrayList<ArrayList> chainT2; // This type includes the chains whose last element
    // refers to a boundary actor which is not dependent, or a pre-boundary actor which 
    // will send to a boundary actor, or a boundary actor which will send to a boundary actor.
    
    public ChainClass(int areaId) {
        this.areaId=areaId;
        chainT0=new ArrayList<>();
        chainT1=new ArrayList<>();
        chainT2=new ArrayList<>();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + areaId;
        result = prime * result + ((chainT0 == null) ? 0 : hashCodeChainT0());
        result = prime * result + ((chainT1 == null) ? 0 : hashCodeChainT1());
        result = prime * result + ((chainT2 == null) ? 0 : hashCodeChainT2());
        return result;
    }

    private int hashCodeChainT2() {
        // TODO Auto-generated method stub
        int result=chainT2.size();
        for(int i=0;i<chainT2.size();i++)
            result+=chainT2.get(i).size();
        return result;
    }

    private int hashCodeChainT1() {
        // TODO Auto-generated method stub
        int result=chainT1.size();
        for(int i=0;i<chainT1.size();i++)
            result+=chainT1.get(i).size();
        return result;
    }

    private int hashCodeChainT0() {
        // TODO Auto-generated method stub
        int result=chainT0.size();
        for(int i=0;i<chainT0.size();i++)
            result+=chainT0.get(i).size();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ChainClass other = (ChainClass) obj;
        if (areaId != other.areaId)
            return false;
        if (chainT0 == null) {
            if (other.chainT0 != null)
                return false;
        } else if (!chainT0.equals(other.chainT0))
            return false;
        if (chainT1 == null) {
            if (other.chainT1 != null)
                return false;
        } else if (!chainT1.equals(other.chainT1))
            return false;
        if (chainT2 == null) {
            if (other.chainT2 != null)
                return false;
        } else if (!chainT2.equals(other.chainT2))
            return false;
        return true;
    }
    
    
}
