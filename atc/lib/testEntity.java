
package ptolemy.domains.atc.lib;


import ptolemy.actor.IOPort;
import ptolemy.actor.TypedCompositeActor;
import ptolemy.actor.TypedIOPort;

import ptolemy.actor.util.CausalityInterfaceForComposites;

import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;





public class testEntity extends TypedCompositeActor{
   
    public testEntity(CompositeEntity container, String name) throws IllegalActionException, NameDuplicationException {
        super(container, name);
        // TODO Auto-generated constructor stub
        setClassName("ptolemy.domains.atc.lib.testEntity");
        input1 = new TypedIOPort(this, "input1", true, false);
        output1 = new TypedIOPort(this, "output1", false, true);
        
    }
    
    /** The input ports*/
    public TypedIOPort input1;

    /** The output ports. */
    public TypedIOPort output1;
    
    
    @Override
    public void preinitialize()  throws IllegalActionException {
        declareDelayDependency();
        super.preinitialize();
    }
    
    protected void _declareDelayDependency(IOPort input, IOPort output,
            double timeDelay) throws IllegalActionException {
        CausalityInterfaceForComposites causality=(CausalityInterfaceForComposites)getCausalityInterface();
        if (timeDelay == 0.0) {
            causality.declareDelayDependency(input, output, 0.0, 1);
        } else {
            causality.declareDelayDependency(input, output, timeDelay, 0);
        }
    }
   
    public void declareDelayDependency() throws IllegalActionException {
        _declareDelayDependency(input1, output1,0.0);
        
    }
}
