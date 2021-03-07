package ptolemy.domains.atc.kernel;

public class DEEventElements {
    public DEEventElements(String name, double modelTime,int index) {
        super();
        this.name = name;
        this.modelTime = modelTime;
        this.index=index;
        
    }
    public String name;
    public double modelTime;
    public int index;
    
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(modelTime);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }


    @Override
    public boolean equals(Object e) {
        if(e instanceof DEEventElements) {
            if(((DEEventElements)e).name!=this.name)
                return false;
//            if(((DEEventElements)e).index!=this.index)
//                return false;
            if(((DEEventElements)e).modelTime!=this.modelTime)
                return false;
                
        }
        return true;
    }
    
}
