import java.util.*;
import java.io.*;

/* Nodes and edges are objects that implement the Comparable interface.  */
class Node implements Comparable<Node> {

	final int name;

	public Node(final int argName) {
		name = argName;
	}

	public int compareTo(final Node argNode) {
		return argNode == this ? 0 : -1;
	}
}

/* The edge class records the weight. */
class Edge implements Comparable<Edge> {

	final Node from, to; 
	final int weight;

	public Edge(final Node argFrom, final Node argTo, final int argWeight){
		from = argFrom;
		to = argTo;
		weight = argWeight;
	}

	public int compareTo(final Edge argEdge){
		return weight - argEdge.weight;
	}
}

/* This is the class that implements the Floyd-Warshall algorithm.  It
 * creates an array P to keep track of the actual path.  I have used a
 * different technique here than what I described in class.  Do not confuse
 * this with the P that I defined there. */

class FloydWarshall {
	public static final int INF = Integer.MAX_VALUE;

	static int[][] D;  // D[i][j] is the length of the shortest path from i to j. 
	static Node[][] P; // P[i][j] stores the first intermediate node on the
	// shortest path from i to j; if it is null the shortest
	// path from i to j is the edge linking i to j.

	public static void calcShortestPaths(Node[] nodes, Edge[] edges) {
		D = initializeWeight(nodes, edges);
		int[][] W = initializeWeight(nodes, edges);
		
		P = new Node[nodes.length][nodes.length];
		
		for (int k = 0; k < nodes.length; k++) {
			for (int i = 0; i < nodes.length; i++) {
				for (int j = 0; j < nodes.length; j++) {
					if (sum(D[i][k], D[k][j]) < D[i][j]) {
						D[i][j] = sum(D[i][k], D[k][j]);
						if (W[i][k] != INF) { //look at the original graph
							P[i][j] = nodes[k]; //if i has direct edge to k then it is the first intermediate node
						}
						else P[i][j] = P[i][k];
					}
				}
			}
		}

	}

	//ensures we do not go over infinity
	public static int sum(int a, int b) {
		if ((a == INF) || (b == INF)) return INF;
		else return a + b;
	}

	public static int getShortestDistance(Node source, Node target){
		return D[source.name][target.name];
	}

	/* This is really a wrapper program.  The real work is done by getIntermediatePath.  
       This produces a list of nodes that are on the path.  Do not change this code. */
	public static ArrayList<Node> getShortestPath(Node source, Node target){
		if(D[source.name][target.name] == Integer.MAX_VALUE){
			return new ArrayList<Node>();// there is no path
		}
		ArrayList<Node> path = getIntermediatePath(source, target);
		path.add(0, source);// Add the source at position 0
		path.add(target);// Add the target at the end.
		return path;
	}

	private static ArrayList<Node> getIntermediatePath(Node source, Node target){
		if(D == null){
			throw new IllegalArgumentException("Must call calcShortestPaths(...) before attempting to obtain a path.");
		}
		//create list of nodes on the intermediate path
		ArrayList<Node> p = new ArrayList<Node>();
		
		Node n;
		if (P[source.name][target.name] == null) { //return empty list because null is defined to mean direct edge
			return p;
		}
		else {
			//get at the first intermediate node
			n = P[source.name][target.name];
			
			//keep adding intermediary nodes until we get null, which means it's a direct edge then
			while (n != null) {
				p.add(n);
				n = P[n.name][target.name];
			}
			return p;
		}
			
		
	}

	private static int[][] initializeWeight(Node[] nodes, Edge[] edges){
		int[][] Weight = new int[nodes.length][nodes.length];

		for (int i = 0; i < nodes.length; i++) {
			for (int j = 0; j < nodes.length; j++) {
				if (i==j) 
					Weight[i][i] = 0; //distance to itself is 0
				else
					Weight[i][j] = INF; 

			}
		}


		//set known edges. the rest will stay at infinity
		for (int i = 0; i < edges.length; i++) {
			Weight[edges[i].from.name][edges[i].to.name] = edges[i].weight;
		}
		return Weight;

	}


	public static void main(String[] args){
		int numNodes;
		int numEdges;
		int from;
		int to;
		int weight;
		Node[] nodes;
		Edge[] edges;
		BufferedReader bi;
		StreamTokenizer st;
		ArrayList<Node> SP;
		try {
			if (args.length != 1) {
				System.err.println("Accepts 1 argument.");
				System.exit(1);
			}

			bi = new BufferedReader(new FileReader(args[0]));
			st = new StreamTokenizer(bi);

			st.nextToken();
			numNodes = (int) st.nval;
			System.out.format("There are %d nodes%n", numNodes);
			nodes = new Node[numNodes];
			for (int i = 0; i < numNodes; i++){ nodes[i] = new Node(i);};
			st.nextToken();
			numEdges = (int) st.nval;
			edges = new Edge[numEdges];
			for (int k = 0; k < numEdges; k++){
				st.nextToken();
				from = (int) st.nval;
				st.nextToken();
				to = (int) st.nval;
				st.nextToken();
				weight = (int) st.nval;
				edges[k] = new Edge(nodes[from],nodes[to],weight);
			}
			calcShortestPaths(nodes, edges);
			for (int i = 0; i < numNodes; i++){
				System.out.println("The routes from "+ i+ " are:");
				for (int j = 0; j < numNodes; j++){
					if (i == j) System.out.println("You are already at the destination " + i+".");
					else{
						SP = getShortestPath(nodes[i],nodes[j]);
						int l = SP.size();
						if (l == 0) System.out.println("There is no path from " + i + " to " + j);
						else{
							if (P[i][j] == null) System.out.println("The shortest path is the edge from "+i+" to "+j+".");
							else{
								System.out.println("From " + i +" to " + j +" the path is");
								for (int k = 0; k < l; k++){ 
									System.out.print(" "+ SP.get(k).name +" ");}
								System.out.println(" end. The length is "+D[i][j]);
							} 
						}
					}
				}
			}
		}
		catch (IOException ex) {
			System.err.println("I/O error");
			System.exit(1);
		}
	}
}