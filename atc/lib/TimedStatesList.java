package ptolemy.domains.atc.lib;

import ptolemy.actor.util.Time;

public class TimedStatesList {
    
    public class TimedState{
        
        public TimedState(int stateIndex,Time stateTime ,TimedState next, TimedState prev) {
            super();
            this.stateIndex = stateIndex;
            this.stateTime=stateTime;
            this.next = next;
            this.prev=prev;
            
        }
        
        /**
         * returns 0 if both objects have the same time;
         * returns 1 if this has less time value; otherwise returns -1; 
         * @param e
         * @return
         */
        public int comparedTo(TimedState e) {
            
            if(this.stateTime.equals(e.stateTime))
                return 0;
            else if(this.stateTime.getDoubleValue()<e.stateTime.getDoubleValue())
                return 1;
            else return -1;
        }
        public int stateIndex;
        Time stateTime;
        TimedState next;
        TimedState prev;
    }
    
    public TimedStatesList(TimedState head, TimedState tail) {
        super();
        this.head = head;
        this.tail = tail;
    }
    
    public void add(int stateIndex, Time stateTime) {
        
        TimedState newObject=new TimedState(stateIndex, stateTime, null,null);
        if(head==null) {
            head=newObject;
            tail=newObject;
            return;
        }
        else {
            TimedState prevObject=tail;
            if(newObject.comparedTo(tail)==0 || newObject.comparedTo(tail)==-1) {
                // add the object to the tail since it has greater time
                newObject.prev=tail;
                tail=newObject;
                prevObject.next=tail;
                return;
            }
            TimedState current=prevObject.prev;
            
            while(current!=null) {
                if(newObject.comparedTo(current)==0 || newObject.comparedTo(current)==-1) {
                    prevObject.prev=newObject;
                    newObject.next=prevObject;
                    newObject.prev=current;
                    current.next=newObject;
                    return;
                }
                else {
                    prevObject=current;
                    current=prevObject.prev;
                }
            }
            prevObject.prev=newObject;
            head=newObject;
            return;
        }
        
    }
    
    public TimedState get() throws IllegalAccessException {
        if(head==null)
            throw new IllegalAccessException("List is empty");
        return head;
    }
    
    public TimedState take() throws IllegalAccessException {
        if(head==null)
            throw new IllegalAccessException("List is empty");
        TimedState temp=head;
        if(temp.next!=null)
            head=temp.next;
        else {
            head=null;
            tail=null;
        }
        return temp;
    }
    
    public boolean isEmpty() {
        if(head==null)
            return true;
        return false;
    }
    
    public void clear() {
        head=null;
    }
    
    TimedState head;
    TimedState tail;

}
