import java.util.*;
import java.io.*;

//This is the description of the objects that represent the nodes of the network.
class Vertex
{
    public static final int INFINITY = 1000; //Why did I not make this maxint?
    public int shortestDist;
    public int bestParent; //used for deciding the path
    private boolean known;
    private int dist; // distances are integers
    private Vertex path; //The path is a list of nodes
    public String name; //If the vertex is number 5 the name is v5; this is done by the main 
                        //program when the input is being read 
    public int pos;

    //constructors
    public Vertex(int p, String s)		 { known = false; dist = INFINITY; path = null; name = s; pos = p;}
    public Vertex(int d, int p, String s){ known = false; dist = d; 	   path = null; name = s; pos = p;}

    public boolean isKnown(){return known;}
    public int getDist(){ return dist;}
    public Vertex getPath(){ return path;}

    public void setKnown(boolean b){known = b;}
    public void setDist(int x){ dist = x;}
    public void setPath(Vertex v){ path = v;}

}

//The source vertex has to be in position 0 of the array.
class shortestPaths {

    static int numVertices;  //This is the number on the first line of the input file.
    static Vertex[] V; // This stores an array of vertices: vertex v5 is in position 5.
    static int[][] L; //This stores the array of link distances.
    
    // This is the set of vertices for which the shortest path is as yet unknown.
    static Set<Vertex> U = new LinkedHashSet<Vertex>();
    static Set<Vertex> visible = new LinkedHashSet<Vertex>(); //list of candidates for "known" set
    static Set<Vertex> known = new LinkedHashSet<Vertex>(); // U + known = all vertices
    
    //This adds all the vertices except v0 to U.  
    public static void initU(){
        for (int i = 1; i < numVertices; i++){U.add(V[i]);}
    }
    
    //This initializes the distances from the data read in from the input file.
    //If the distance is given by a direct link we know it, otherwise it is INFINITY.
    public static void initDists(int s0){
        V[s0].setKnown(true);
        for(int i = 0; i < numVertices; i++){V[i].setDist(L[s0][i]);}
    }

    // This finds the minimum among the vertices for which the shortest path is not yet known. 
    public static Vertex findMin() {  	
    	Iterator<Vertex> k = known.iterator(); 
    	
    	while (k.hasNext()) { //look at each vertex our known vertices can "see"
    		Vertex v = k.next();
    		initDists(v.pos);
    		for (int i = 0; i < numVertices; i++) {
    			if (V[i].getDist() != 0 && V[i].getDist() != 1000 && !known.contains(V[i])) 
    				{
    					if ( //if not visited yet OR if visited already but there is a shorter distance to the vertex
    						!visible.contains(V[i]) || ( visible.contains(V[i]) && (V[i].shortestDist > (v.shortestDist + V[i].getDist())) ) 
    					) { 
	    					V[i].shortestDist = v.shortestDist + V[i].getDist();
	    					V[i].bestParent = v.pos; //this will be used to decide the path
	    					visible.add(V[i]);
    					}
    					
    				}
    		}
    		
    	}
    	
    	//now we got all the vertices in the vicinity and their distances to v0
    	Iterator<Vertex> vis = visible.iterator();
    	if (!visible.isEmpty()) { //just for precaution
    		Vertex closest = vis.next(); 
    	
	    	Vertex temp;
	    	while (vis.hasNext()) { //find the best candidate to add to "known" set
	    		temp = vis.next();
	    		if (temp.shortestDist < closest.shortestDist) closest = temp;
	    	}
	    	
	    		if (closest.bestParent != 0) closest.setPath(V[closest.bestParent]); //set path
	    		else closest.setPath(null); //weird, i know, but needed to properly display paths
	    	
	    	visible.clear();

	    	return closest;
    	}
    	return null;
    }  


    //This is the main program, it finds the shortest paths.
    public static void findPaths() {
    	while(!U.isEmpty()) {
    		known.add(V[0]); //starting vertex is known
    		U.remove(V[0]); //just to make sure
    		V[0].shortestDist = 0;
    		
    		Vertex newMin = findMin();
    		if (newMin == null) return; //just in case
    		else {
    			known.add(newMin);
    			U.remove(newMin);
    		}
    	}
    }

    //    These are utility routines for displaying the paths.
    public static void showPath(Vertex v){
        if (v.getPath() != null)
            {
                showPath(v.getPath());
                System.out.print( " to " );
            }
        System.out.print(v.name);
    }

    public static void displayPath(Vertex v){
        Vertex tmp = v.getPath();
        if (tmp == null)
            {System.out.println("Direct from v0 to "+v.name);}
        else
            {
                System.out.print("From v0 via ");
                showPath(v);
                System.out.format("%n");
            }
    }
        
    // run this by calling java shortestPaths <inputfilename>
    public static void main(String[] args)
    {
        BufferedReader bi;
        StreamTokenizer st;
        String name;

        try {
			if (args.length != 1) {
				System.err.println("Accepts 1 argument.");
				System.exit(1);
			}

            bi = new BufferedReader(new FileReader(args[0]));
            st = new StreamTokenizer(bi);

            st.nextToken();
            numVertices = (int) st.nval;// first line gives number of vertices and nothing else
            System.out.format("There are %d vertices%n", numVertices);// For checking
            L = new int[numVertices][numVertices];
            V = new Vertex[numVertices];
            for (int i = 0; i< numVertices; i++){
                for (int j = 0; j < numVertices; j++){
                    st.nextToken();
                    L[i][j] = (int) st.nval;
                }
            }
            for (int i = 0; i < numVertices; i++){
                name = "v"+Integer.toString(i);
                V[i] = new Vertex(i,name);
            }
            /*
            System.out.println("The table of distances is:");        
            for (int i = 0; i < numVertices; i++){
                for (int j = 0; j < numVertices; j++){
                    System.out.format("L[%d][%d] = %d, ", i,j,L[i][j]);
                    System.out.format("%n");}} */
	        initDists(0);
	        initU();
	        findPaths();
	        System.out.println("The routes are:");
	        for (int i = 0; i < numVertices; i++){
	            displayPath(V[i]);
	        }
	        System.out.println("That's a really smart little green man!");
	        
        }
		catch (IOException ex) {
			System.err.println("I/O error");
			System.exit(1);
		}
	}
}