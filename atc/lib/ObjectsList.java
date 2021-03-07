package ptolemy.domains.atc.lib;

import java.util.ArrayList;

import ptolemy.data.IntToken;
import ptolemy.data.RecordToken;
import ptolemy.data.Token;

public class ObjectsList {
    public class MovingObjectCell {
        public MovingObjectCell(Token movingObject, double timeOfDeparting, int departureDirection, MovingObjectCell next) {
            super();
            this.movingObject = movingObject;
            this.timeOfDeparting = timeOfDeparting;
            this.departureDirection = departureDirection;
            this.next = next;
        }
        
        public MovingObjectCell() {
            // TODO Auto-generated constructor stub
            next=null;
        }
        
        public MovingObjectCell(Token movingObject2, double time, int dir) {
            // TODO Auto-generated constructor stub
            this.movingObject=movingObject2;
            this.timeOfDeparting=time;
            this.departureDirection=dir;
            next=null;
        }
        
        public int compareTo(Object e) {
            if(e instanceof MovingObjectCell) {
                if(this.timeOfDeparting<((MovingObjectCell)e).timeOfDeparting)
                    return 1;
                else if(this.timeOfDeparting>((MovingObjectCell)e).timeOfDeparting)
                    return -1;
            }
            return 0;
        }
        
//        @Override
        public boolean equal(Token e) {
//            if(e instanceof Token)
            if(this.movingObject.equals((Token)e))
                return true;
            return false;
        }
        public Token movingObject;
        public double timeOfDeparting;
        public int departureDirection;
        public MovingObjectCell next;
        
//        @Override
//        public int hashCode() {
//            final int prime = 31;
//            int result = 1;
////            result = prime * result + getOuterType().hashCode();
//            result = prime * result + departureDirection;
//            result = prime * result + ((movingObject == null) ? 0 : ((IntToken)((RecordToken)movingObject).get("aircraftId")).intValue());
//            result = prime * result + ((next == null) ? 0 : next.hashCode());
//            long temp;
//            temp = Double.doubleToLongBits(timeOfDeparting);
//            result = prime * result + (int) (temp ^ (temp >>> 32));
//            return result;
//        }
        
//
//        private ObjectsList getOuterType() {
//            return ObjectsList.this;
//        }
        
        
    }
    
    public ObjectsList() {
        super();
        head=null;
    }
    
    public void add(Token movingObject, double time, int dir) {
        MovingObjectCell newObject=new MovingObjectCell(movingObject, time, dir);
        if(head==null) {
            head=newObject;
            return;
        }
        else {
            MovingObjectCell prevObject=head;
            if(newObject.compareTo(head)==1) {
                // time is smaller than head's one
                head=newObject;
                newObject.next=prevObject;
                return;
            }
            MovingObjectCell current=prevObject.next;
            
            while(current!=null) {
                if(newObject.compareTo(current)==1) {
                    prevObject.next=newObject;
                    newObject.next=current;
                    return;
                }
                else {
                    prevObject=current;
                    current=prevObject.next;
                }
            }
            prevObject.next=newObject;
            return;
        }
        
    }
    
    
    public MovingObjectCell remove(int aircraftId) throws IllegalAccessException {
        // TODO Auto-generated method stub
      if(head==null)
      {
           throw new IllegalAccessException("List is empty");
      }
      MovingObjectCell temp=head;
      int planeId=((IntToken)((RecordToken) head.movingObject).get("aircraftId")).intValue();
      if(planeId==aircraftId) {
          head=temp.next;
          return temp;
      }
      
      MovingObjectCell prevObject=head;
      temp=prevObject.next;
      while(temp!=null) {
          planeId=((IntToken)((RecordToken) temp.movingObject).get("aircraftId")).intValue();
          if(planeId==aircraftId) {
              prevObject.next=temp.next;
              return temp;
          }
          else
          {
              prevObject=temp;
              temp=prevObject.next;
          }
      }
//      
//      
    return null;
    }
    
//    public void remove(Token movingObject) throws IllegalAccessException {
//        if(head==null)
//        {
//             throw new IllegalAccessException("List is empty");
//        }
//        MovingObjectCell temp=head;
//        if(head.equal(movingObject)) {
//            head=temp.next;
//            return;
//        }
//        MovingObjectCell prevObject=head;
//        temp=prevObject.next;
//        if(temp.equal(movingObject)) {
//            prevObject.next=temp.next;
//            return;
//        }
//        else
//        {
//            prevObject=temp;
//            temp=prevObject.next;
//        }
//        
//        
//    }
    
    public boolean contain(Token movingObject) {
        if(head==null) {
            return false;
        }
        MovingObjectCell temp=head;
        while(temp!=null) {
            if(temp.equal(movingObject)) {
                return true;
            } 
            temp=temp.next;
        }
        return false;
    }
    
    
    public MovingObjectCell take() throws IllegalAccessException {
        if(head==null)
            throw new IllegalAccessException("List is empty");
        MovingObjectCell temp=head;
        if(temp.next!=null)
            head=temp.next;
        else
            head=null;
        return temp;
    }
    
    public MovingObjectCell get() throws IllegalAccessException {
        if(head==null)
            throw new IllegalAccessException("List is empty");
        return head;
    }
    
    public ArrayList<Integer> getList(){
        ArrayList<Integer> entities=new ArrayList<>();
        MovingObjectCell temp=head;
        while(temp!=null) {
            entities.add(((IntToken)((RecordToken) temp.movingObject).get("aircraftId")).intValue());
            temp=temp.next;
        }
        return entities;
    }

    public void clear() {
        // TODO Auto-generated method stub
        head=null;
        
    }
    public boolean isEmpty() {
        if(head==null)
            return true;
        return false;
    }
    
    public MovingObjectCell head;

    
}
