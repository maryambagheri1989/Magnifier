import java.util.List;


public class Node {
    String source;
    String destination;
    double depFromSource;
    List<String> schedule;
    
    public Node(String source, String destination, double depFromSource, List<String> schedule) {
        super();
        this.source = source;
        this.destination = destination;
        this.depFromSource = depFromSource;
        this.schedule = schedule;
    }
    
    

}
