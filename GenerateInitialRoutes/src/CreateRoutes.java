//This program is completely correct: 10/28/2018
/**
 * For a 3*3 mesh map
 *             sourceY
 *             0  1   2
 *             ---------
 *         0 - 0   1   2 -0
 * sourceX 1 - 3   4   5 -1 destX
 *         2 - 6   7   8 -2
 *             ---------
 *             0   1   2
 *              destY
 * sourceX shows three entrance points
 * sourceY shows three entrance points
 * destX shows three departure points
 * destY shows three departure points
 * 
 * Index of the sources in sourceOfFligh array: first three sourceX and then three sourceY
 */


// You can remove concurrency to arrival time is equal to the departure time in containTime
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class CreateRoutes {
    
    static double landa; //Arrival rate
    static int flights; // Number of moving objects
    static int n;       // Dimension of the network
    //static int m;
    static int dimentionOfRegion;
    static List[] sourceOfFlight; // Source of the flight
    static double MFD; //Minimum flight duration
    static List<Double>[] departureTimesInTracks;
//    static List<Integer>[] regions;
    
    
    
    public static void main(String[] args) throws IOException {
        // TODO Auto-generated method stub
        landa=0.5;
//        flights=1000;
       
        flights=5500;
//        n=15;
        n=18;
  
        MFD=1.00;
        dimentionOfRegion=9;
//        dimentionOfRegion=5;
        for (int k=1;k<=1;k++){
        String file="input"+k+".txt";
        BufferedWriter bufferedWriter=null;
        try {
            FileWriter fileWriter = new FileWriter(file);
            bufferedWriter=new BufferedWriter(fileWriter);
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        
        
//        regions= new List[n];
//        fillRegions();
        
        departureTimesInTracks=new List[n*n];
        
        List<Node> timeTable=new ArrayList<Node>(); // List of the moving objects with their time schedules
        sourceOfFlight=new List[2*n];

        // Gradually adds moving objects and finds a time-conflict free route for the new added moving object
      
        int j=0;
        while(j<flights){
        
            String source=generateSource();
            
            System.out.println("Source: "+source+",");

            int sourceX=Integer.valueOf(source.split(",")[0]);
            int sourceY=Integer.valueOf(source.split(",")[1]);
            
            String destination="";
            do{
                destination=generateDestination(sourceX,sourceY);
            }while(source.equals(destination));
            
            int destX=Integer.valueOf(destination.split(",")[0]);
            int destY=Integer.valueOf(destination.split(",")[1]);
            
            double departureTime=generateDepartureTime(sourceX, sourceY);
            departureTime=round(departureTime, 2);
            
            List<Integer> route;

            route=new ArrayList<Integer>();
            route=generateRoute(departureTime, sourceX, sourceY, destX, destY, route);
            
             if(route!=null){
                //Add to the list, sort departure time...
                 if(sourceX==0 && sourceY==0){
                     if(sourceOfFlight[0]==null)
                     {
                         sourceOfFlight[0]=new ArrayList<Double>();
                         sourceOfFlight[0].add(departureTime);
//                         System.out.println("Index: "+0+"\n");
                     }
                     else{
                         sortAndAdd(departureTime,0);
//                         System.out.println("Index: "+0+"\n");
                     }
                 }
                 else if(sourceX==0 && sourceY!=0){
                    if(sourceOfFlight[n+sourceY]==null)
                    {
                        sourceOfFlight[n+sourceY]=new ArrayList<Double>();
                        sourceOfFlight[n+sourceY].add(departureTime);
//                        System.out.println("Index: "+(n+sourceY)+"\n");
                    }
                    else{
                        sortAndAdd(departureTime,n+sourceY);
//                        System.out.println("Index: "+(n+sourceY)+"\n");
                    }
                }
                else if(sourceY==0 && sourceX!=0){
                    if(sourceOfFlight[sourceX]==null)
                    {
                        sourceOfFlight[sourceX]=new ArrayList<Double>();
                        sourceOfFlight[sourceX].add(departureTime);
//                        System.out.println("Index: "+sourceX+"\n");
                    }
                    else{
                        sortAndAdd(departureTime,sourceX);
//                        System.out.println("Index: "+sourceX+"\n");
                    }
                }
                
                List<String> schedule=new ArrayList<String>();
                double dep=0;
                double tempDepTime=departureTime;
                for(int i=0;i<route.size();i++){
                    String x="("+(route.get(i)+1)+"/"+departureTime+")";
                    schedule.add(x);
                    dep=departureTime+MFD;
                    if(departureTimesInTracks[route.get(i)]==null)
                        departureTimesInTracks[route.get(i)]=new ArrayList<Double>();
                    departureTimesInTracks[route.get(i)].add(dep);
                    departureTime=dep;
                }
                
                Node travelRoute=new Node(source, destination,tempDepTime , schedule);
                timeTable.add(travelRoute);
                j++;
//                writeIntoFileInputARoute(travelRoute, bufferedWriter);
             }
        }
        
        try {
            addDestinations(timeTable);

            writeIntoFileInput(timeTable,bufferedWriter);

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        }
    }
    
    
    private static void writeIntoFileInputARoute(Node travelRoute,BufferedWriter bufferedWriter) throws IOException {
        // TODO Auto-generated method stub
            String x="{";
            int region=0;
            for(int j=0;j<travelRoute.schedule.size();j++){
                String trackN=travelRoute.schedule.get(j).split("/")[0].substring(1);
                if(findRegion(Integer.valueOf(trackN))!=region){
                    if(j==0)
                        x+=findRegion(Integer.valueOf(trackN))+",";
                    else if(j!=travelRoute.schedule.size()-1)
                    {
                        x+=";"+findRegion(Integer.valueOf(trackN))+",";
                    }
                    else
                        x+=",";
                    region=findRegion(Integer.valueOf(trackN));
                }
                x+=travelRoute.schedule.get(j);
                if(j<travelRoute.schedule.size()-1 && region==findRegion(Integer.valueOf(travelRoute.schedule.get(j+1).split("/")[0].substring(1))))
                    x+=",";
            }
            bufferedWriter.write(x+"}\n");
            bufferedWriter.flush();
        
    }


    private static void addDestinations(List<Node> timeTable) throws IOException {
        // TODO Auto-generated method stub
        BufferedReader bufferedReader =  new BufferedReader(new FileReader("destinations"));
        String line="";
        List<String> tracks=new ArrayList<String>();
        List<String> destinations=new ArrayList<String>();
        while((line=bufferedReader.readLine())!=null)
        {
            tracks.add(line.split(",")[0]);
            destinations.add(line.split(",")[1]);
        }
        
        for(int i=0;i<timeTable.size();i++)
        {
            String x=timeTable.get(i).schedule.get(timeTable.get(i).schedule.size()-1);
            double time=Double.valueOf(x.split("/")[1].substring(0,x.split("/")[1].length()-1));
            time+=MFD;
            int j=tracks.indexOf(x.split("/")[0].substring(1));
            if(j==-1)
                j=-1;
            String des=destinations.get(j);
            timeTable.get(i).schedule.add("("+des+"/"+time+")");
        }
    }


    private static int findRegion(int trackNum) {
        // TODO Auto-generated method stub
        int numOfRegionsInEachRowOrColumn=n/dimentionOfRegion;
        int w=trackNum/n; // to find the row indexed from 0
        int z=trackNum%n; // to find the column indexed from 1

        
        int x=w/dimentionOfRegion; // to find row of the region
        int y=z/dimentionOfRegion; // to find column of the region
        
        if(z==0) {
            int region=dimentionOfRegion;
            if(w%dimentionOfRegion==0)
                return (x*numOfRegionsInEachRowOrColumn);
            else if(w%dimentionOfRegion!=0)
                return (numOfRegionsInEachRowOrColumn+x*numOfRegionsInEachRowOrColumn);   
        }
//        if(z==0 && w<dimentionOfRegion)
//            x=1;
        
        if(z%dimentionOfRegion==0)
            return x*numOfRegionsInEachRowOrColumn+y;
        else
            return x*numOfRegionsInEachRowOrColumn+y+1;
    }


    private static void writeIntoFileOutput(List<Node> timeTable) throws IOException {
        // TODO Auto-generated method stub
        String file="output.txt";
        FileWriter fileWriter=new FileWriter(file);
        BufferedWriter bufferedWriter=new BufferedWriter(fileWriter);
        for(int i=0;i<timeTable.size();i++){
            bufferedWriter.write("Source: "+timeTable.get(i).source+"\n");
            bufferedWriter.write("Destination: "+timeTable.get(i).destination+"\n");
            bufferedWriter.write("departureFromSource: "+timeTable.get(i).depFromSource+"\n");
            String x="";
            for(int j=0;j<timeTable.get(i).schedule.size();j++)
                x+=timeTable.get(i).schedule.get(j)+",";
            bufferedWriter.write(x+"\n");
            bufferedWriter.flush();
        }
        bufferedWriter.close();
            
    }
    
    private static void writeIntoFileInput(List<Node> timeTable, BufferedWriter bufferedWriter) throws IOException {
        // TODO Auto-generated method stub
        
        for(int i=0;i<timeTable.size();i++){
            String x="{";
            int region=0;
            for(int j=0;j<timeTable.get(i).schedule.size();j++){
                String trackN=timeTable.get(i).schedule.get(j).split("/")[0].substring(1);
                if(findRegion(Integer.valueOf(trackN))!=region){
                    if(j==0)
                        x+=findRegion(Integer.valueOf(trackN))+",";
                    else if(j!=timeTable.get(i).schedule.size()-1)
                    {
                        x+=";"+findRegion(Integer.valueOf(trackN))+",";
                    }
                    else
                        x+=",";
                    region=findRegion(Integer.valueOf(trackN));
                }
                x+=timeTable.get(i).schedule.get(j);
                if(j<timeTable.get(i).schedule.size()-1 && region==findRegion(Integer.valueOf(timeTable.get(i).schedule.get(j+1).split("/")[0].substring(1))))
                    x+=",";
            }
            if(i==timeTable.size()-1)
                bufferedWriter.write(x+"}");
            else
                bufferedWriter.write(x+"}\n");
            bufferedWriter.flush();
        }
        bufferedWriter.close();
        System.out.println("Finish");
    }


    private static void print(Node travelRoute) {
        // TODO Auto-generated method stub
        System.out.println("Source: "+travelRoute.source+" DepartureTime: "+travelRoute.depFromSource+ " Destination: "+travelRoute.destination);
        String x="";
        for(int i=0;i<travelRoute.schedule.size();i++)
            x+=travelRoute.schedule.get(i)+",";
        System.out.println(x);
        System.out.println("*************");
    }

/**
 * Each source keeps an array that shows departure time of the moving objects departing from it. 
 * This array is sorted. 
 * @param departureTime
 * @param index
 */
    private static void sortAndAdd(double departureTime, int index) {
        // TODO Auto-generated method stub
        double[] x=new double[sourceOfFlight[index].size()];
        for(int i=0;i<x.length;i++)
            x[i]=(Double) sourceOfFlight[index].get(i);
        sourceOfFlight[index].clear();
        
        int i;
        for(i=0;i<x.length;i++)
            if(x[i]>=departureTime){
                for(int j=0;j<i;j++)
                    sourceOfFlight[index].add(x[j]);
                break;
            }
        sourceOfFlight[index].add(departureTime);
        for(int j=i;j<x.length;j++)
            sourceOfFlight[index].add(x[j]);
        
    }

    /**
     * Recursively generate a route based on timed-xy algorithm. It checks all the possible routes from source to the destination
     * which are created using timed-xy algorithm. 
     * @param arrivalTime
     * @param sourceX
     * @param sourceY
     * @param desX
     * @param desY
     * @param route
     * @return
     */
    private static List<Integer> generateRoute(double arrivalTime, int sourceX, int sourceY, int desX, int desY, List<Integer> route) {
        // TODO Auto-generated method stub
        if(containTime(sourceX,sourceY, arrivalTime))
            return null;
        route.add(sourceX*n+sourceY);
//        route.add(sourceX*m+sourceY);
        if(sourceX-desX==0 && sourceY-desY==0){
            return route;
        }
        
        int incX=IncreaseXY(sourceX, sourceY, desX, desY, 0);
        int incY=IncreaseXY(sourceX, sourceY, desX, desY, 1);
        if(incX==1){
            if(generateRoute(arrivalTime+MFD, sourceX+1, sourceY, desX, desY,route)==null)
                if(incY==1){
                    if(generateRoute(arrivalTime+MFD, sourceX, sourceY+1, desX, desY, route)==null){
                        if(route.size()>0)
                            route.remove(route.size()-1);
                        return null;
                    }
                        
                }
                else
                {
                    if(route.size()>0)
                        route.remove(route.size()-1);
                    return null;
                }
        }
        else if(incX==-1){
            if(generateRoute(arrivalTime+MFD, sourceX-1, sourceY, desX, desY,route)==null)
                if(incY==1){
                    if(generateRoute(arrivalTime+MFD, sourceX, sourceY+1, desX, desY, route)==null){
                        if(route.size()>0)
                            route.remove(route.size()-1);
                        return null;
                    }
                }
                else{
                    if(route.size()>0)
                        route.remove(route.size()-1);
                    return null;
                }
        }
        else if(incY==1)
        {
            if(generateRoute(arrivalTime+MFD, sourceX, sourceY+1, desX, desY, route)==null){
                if(route.size()>0)
                    route.remove(route.size()-1);
                return null;
                
            }
        }
        else{
            route.remove(route.size()-1);
            return null;
        }
       
        return route;
    }
    

    /**
     * Whether the travels in the node indexed with sourceX*n+sourceY have a conflict with the new incoming
     * moving object in time arrivalTime
     * @param sourceX
     * @param sourceY
     * @param arrivalTime
     * @return
     */
    private static boolean containTime(int sourceX, int sourceY, double arrivalTime) {
        // TODO Auto-generated method stub
        if(departureTimesInTracks[sourceX*n+sourceY]!=null && 
                departureTimesInTracks[sourceX*n+sourceY].contains(arrivalTime+MFD))
            return true;
        
        if(departureTimesInTracks[sourceX*n+sourceY]!=null && 
                departureTimesInTracks[sourceX*n+sourceY].contains(arrivalTime))   // To remove concurrency
            return true;
        
        List<Double> sorted=new ArrayList<Double>();
        if(departureTimesInTracks[sourceX*n+sourceY]!=null)
            for(int i=0;i<departureTimesInTracks[sourceX*n+sourceY].size();i++)
                if(departureTimesInTracks[sourceX*n+sourceY].get(i)>arrivalTime)
                    sorted.add(departureTimesInTracks[sourceX*n+sourceY].get(i));

        
        double min=10000;
        for(int i=0;i<sorted.size();i++)
            if(sorted.get(i)<=min)
                min=sorted.get(i);
        if((min!=10000 && min-MFD<arrivalTime && arrivalTime<min) || (min!=10000 && arrivalTime+MFD<min && min-MFD<= arrivalTime+MFD))
            return true;
        
        return false;
    }

    /**
     * The direction of the travel from (sourceX, sourceY) towards (destX, destY)
     * @param sourceX
     * @param sourceY
     * @param destX
     * @param destY
     * @param i
     * @return increaseX or increaseY based on the value of i: -1 for increaseX shows travel towards the north,
     *  0 don't move, 1 for increaseX shows travel towards south, 1 for increaseY shows travel towards east
     */
    private static int IncreaseXY(int sourceX, int sourceY, int destX, int destY, int i) {
        // TODO Auto-generated method stub
        int increaseX=0; // neither increase nor decrease
        int increaseY=0;
        if(sourceX==destX || (sourceX+1==destX && sourceY!=0 && destY==0)){
            increaseX=0;
        }
        
        if(sourceX<destX)
            increaseX=1;
        else if(sourceX>destX)
            increaseX=-1;
        
        if(sourceY==destY){
            increaseY=0;
        }
       
        if(sourceY<destY)
            increaseY=1;
        
        if(i==0)
            return increaseX;
        else
            return increaseY;
    }

    /**
     * Generate departure time based on the poisson distribution
     * @param sourceX
     * @param sourceY
     * @return
     */
    private static double generateDepartureTime(Integer sourceX, Integer sourceY) {
        // TODO Auto-generated method stub
        int index=0;
        if(sourceX==0 && sourceY==0)
            index=0;
        else if(sourceX==0 && sourceY!=0){
            index=sourceY+n;
        }
        else if(sourceY==0 && sourceX!=0){
            index=sourceX;
        }
        
        double nextTime=exp_gen();
        if(sourceOfFlight[index]!=null || nextTime==0)
            while(nextTime<=MFD || nextTime==0){
                nextTime=exp_gen();
            }
        
        if(sourceOfFlight[index]!=null)
            return ((Double)sourceOfFlight[index].get(sourceOfFlight[index].size()-1)+nextTime);
        else
            return nextTime;
    }

    /**
     * Generate source of the moving object randomly; for instance (1,0)
     * @return
     */
    private static String generateSource() {
        // TODO Auto-generated method stub
        int source=randInt(0, 2*n-1);
        if(source>n-1) //horizontal
            return 0+","+(source%(n));
        else         //vertical
            return source+","+0;
    }
    
    /**
     * Generate destination of the moving object randomly; for instance (8,8)
     * @return
     */
    private static String generateDestination(int sourceX, int sourceY) {
        // TODO Auto-generated method stub
        do {
            int destination=randInt(0, 2*n-1);
   
            if(destination<=n-1) //VERTICAL
                return (destination)+","+(n-1);
    
            else if (sourceX==0 && destination%(n)<=sourceY)
                    continue;
            return (n-1)+","+destination%(n);
    
        } while(true);
    }
    
    private static int randInt(int min, int max){
        Random rand = new Random(System.nanoTime());
        int randNum=rand.nextInt((max - min) + 1) + min;
        return randNum;

    }
    
    static double exp_gen()
    {
        Random r = new Random(System.nanoTime());
        double rand_value = r.nextDouble();
        while (rand_value == 0)
        {
            rand_value = r.nextDouble();
        }
        rand_value = -(Math.log(1 - rand_value) / landa);
        return (rand_value);
    }
    
    

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }
}
