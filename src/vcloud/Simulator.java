package vcloud;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.Comparator;

public class Simulator {
	
	private static Map<Vtype, Integer> vcpu;
	private static String LOG;
	private static String Graph_LOG;
	private static String LogPath = "src/log/simulator.log";
	private static boolean debug = true;
	
	
	//compare which server has more space left
	public static Comparator<Server> ServerComparator = new Comparator<Server>() {
		public int compare(Server s1, Server s2) {
			//more cpu power left then return 1
			return s1.getCpuUnused()- s2.getCpuUnused();
		}
	};
	public static Comparator<Request> RequestComparator = new Comparator<Request>() {
		public int compare(Request r1, Request r2) {
			//more cpu power left then return 1
			return vcpu.get(r1.getType()) - vcpu.get(r2.getType());
		}
	};
	
	public static List<Request> readRequest(String filepath) throws IOException{
		
		ArrayList<Request> requests = new ArrayList<>();
		File file = new File(filepath);
		Scanner read = new Scanner(file);
		int num = 0;
		while(read.hasNext()){
			String line = read.nextLine().trim();
			String[] request = line.split(",");
			//System.out.println(request[0] + " " + request[1]);
			
			//request[0] -> Vtype, request[1] -> vhost number
			int rt = Integer.parseInt(request[0]);
			Request req = new Request();
			req.setId(num);
			
			Map det = new HashMap();
			Vtype vt = null;
			
			switch(rt){
			case 0: 
				vt = Vtype.SMALL;
				break;
			case 1:
				vt = Vtype.MEDIUM;
				break;
			case 2:
				vt = Vtype.LARGE;
				break;
			}
			req.setType(vt);
			// term + 1 means won't count the term during the term the vhost is setup
			req.setTerm(Integer.parseInt(request[1]) );
			requests.add(req);
			num++;
		}
		return requests;
	}
	
	//show how many physical servers running in a cloud and how many vh in each server
	public static void showCloud(Cloud cloud){
		ArrayList<Server> servers = (ArrayList<Server>) cloud.getServers();
		String output = "";
		for(Server s : servers){
			//System.out.println("server: " + s.getId() );
			
			output = output + "server: " + s.getId() + "\n";
			for(Vhost v: s.getVhosts()){
				//System.out.print(v.getType() + " (id:" + v.getId() + " term: " + v.getTerm() + " " + v.getVhostStatus() + " )");
				output = output + v.getType() + " (id:" + v.getId() + " TERM: " + v.getTerm() + " " + v.getVhostStatus() + " )";
			}
			//System.out.println("\n");
			output = output + "\n";
		}
		if(debug){
			LOG = LOG + output;
			LOG = LOG + " -------------------------------------------------------------\n";
		}
		
		System.out.println(output);
		showVmCPU(cloud);
	}
	
	public static void showVmCPU(Cloud cloud){
		ArrayList<Server> servers = (ArrayList<Server>) cloud.getServers();
		String output = "";
		for(Server s : servers){
			//System.out.println("server cpu level: " + s.getCpuUnused() );
			output = output + "server: " + s.getId() + " cpu level " + s.getCpuUnused() + "\n";
		}
		System.out.println(output);
		LOG = LOG + output;
	}
	
	public static ArrayList<Vhost> getEndofTermVM(Cloud cloud){
		ArrayList<Server> servers = (ArrayList<Server>) cloud.getServers();
		ArrayList<Vhost> endOfTermVms = new ArrayList<>();
		for(Server s : servers){
			//System.out.println("server: " + s.getId() );
			
			for(Vhost v: s.getVhosts()){
				//System.out.print(v.getType() + " (id:" + v.getId() + " term: " + v.getTerm() + " " + v.getVhostStatus() + " )");
				if(v.getTerm() <=1){
					endOfTermVms.add(v);
				}
			}
			
		}
		System.out.println();
		return endOfTermVms;
	}
	
	public static Cloud firstFit(Cloud cloud, List<Request> requests){
		
		//get list of servers in the cloud
		ArrayList<Server> servers = null;
		
		//process every 10 request as a time slot
		for(int i=0; i<requests.size(); i=i+10){
			LOG = LOG + "Request group: " + i + "\n";
			System.out.println("Request group: " + i);
			
			servers = (ArrayList<Server>) cloud.getServers();
			LOG = LOG + "Current server size: " + servers.size();
			System.out.println("Current server size: " + servers.size());
			
			//from second round, remove expired vms
			if(i != 0){
				for(Server s : servers){
					if(debug){
						System.out.println("Server " + s.getId());
						LOG = LOG + " Server " + s.getId() + "\n";
					}
					
					for(Vhost v: s.getVhosts()){
						if(v.getVhostStatus().equals(v.getVhostStatus().DOWN)){
							continue;
						}
						//decrease term
						int term_t = v.getTerm() - 1;
						
						
						if(term_t <= 0){
							//end of life, turn off
							v.setVhostStatus(v.getVhostStatus().DOWN);
							if(debug){
								System.out.print(" removed vhost " + v.getId() + ": " + v.getType());
								LOG = LOG + " removed vhost " + v.getId() + ": " + v.getType() + "\n";
							}
							
							//add back released cpu power
							//System.out.print(" before " + v.getServer().getCpuUnused());
							s.setCpuUnused(s.getCpuUnused() + vcpu.get(v.getType()));
							//System.out.println(" after " + v.getServer().getCpuUnused());
							
						}else{
							//update the term
							v.setTerm(term_t);
						}
					}
				}
			}
			if(debug){
				LOG = LOG + "After removing expired VMs.\n";
				showVmCPU(cloud);
				LOG = LOG + "-----------------------------\n";
			}
			
			//loop through every one of the 10 requests
			for(int j=i; j<i+10; j++){
				Vhost vh = null;
				boolean found = false;
				
				Request request = requests.get(j);
				//System.out.println("Serverlist size " + servers.size());
				System.out.println("Request " + j + ": " + request.getType() + " " + request.getTerm());
				
				//check each running server to find availability
				for(Server s : servers){
					//System.out.println("Server_" + s.getId() + " cpu: " + s.getCpuUnused());
					//found available space in an existing server
					if(vcpu.get(request.getType()) <= s.getCpuUnused() && found == false){
						//create vhost
						//System.out.println(vcpu.get(request.getType()) + "<" + s.getCpuUnused());
						vh = new Vhost();
						vh.setServer(s);
						vh.setType(request.getType());
						vh.setVhostStatus(vh.getVhostStatus().RUNNING);
						vh.setTerm(request.getTerm());
						System.out.println("before: " + s.getCpuUnused());
						s.setCpuUnused(s.getCpuUnused() - vcpu.get(request.getType()));
						System.out.println("after: " + s.getCpuUnused());
						//update vhost list in Server
						ArrayList<Vhost> vhlist = null;
						if(s.getVhosts() == null){
							vhlist = new  ArrayList<>();
							vhlist.add(vh);
						}else{
							vhlist = (ArrayList<Vhost>) s.getVhosts();
							vhlist.add(vh);
						}
						s.setVhosts(vhlist);
						//System.out.println("Server id:" + s.getId() + " cpu-left " +  s.getCpuUnused() + " " + s.getVhosts().size());
						//found location already, get out of the loop, go next request
						found = true;
					}else{
						//check next server
						continue;
					}
					System.out.println("Server_" + s.getId() + " cpu: " + s.getCpuUnused());
				}
				if(vh == null){
					//no server available
					//create new server, add to cloud
					Server newS = new Server(servers.size());
					System.out.println("Create new server" + newS.getId());
					newS.setCloud(cloud);
					newS.setServerStatus(newS.getServerStatus().RUNNING);
					
					//bring up the new host
					vh = new Vhost();
					vh.setType(request.getType());
					vh.setTerm(request.getTerm());
					vh.setVhostStatus(vh.getVhostStatus().RUNNING);
					vh.setServer(newS);
					newS.setCpuUnused(newS.getCpuUnused() - vcpu.get(request.getType()));
					
					//update Vhost list in this Server obj
					ArrayList<Vhost> vhlist = new  ArrayList<>();
					vhlist.add(vh);
					newS.setVhosts(vhlist);
					
					servers.add(newS);
					cloud.setServers(null);
					cloud.setServers(servers);
				}
			}
			
			//need to turn off vhost which reaches to its life span
			
			System.out.println(" -------------------------------------------------------------");
			System.out.println("End of group: " + i/10 + " running server : " + servers.size() );
			LOG = LOG + " -------------------------------------------------------------\n";
			LOG = LOG + "End of group: " + i/10 + " running server : " + servers.size() + "\n";
			LOG = LOG + "\n";
			Graph_LOG = Graph_LOG + i/10 + "," + servers.size() + "\n";
			
			System.out.print("\n");
			showCloud(cloud); 
			System.out.println("\n ----------------------------------------------------------");
		}
		
		
		System.out.println("out....");
		return cloud;
	}
	
public static Cloud FFD(Cloud cloud, List<Request> requests){
		
		//get list of servers in the cloud
		ArrayList<Server> servers = null;
		
		//process every 10 request as a time slot
		for(int i=0; i<requests.size(); i=i+10){
			LOG = LOG + "Request group: " + i + "\n";
			System.out.println("Request group: " + i);
			
			servers = (ArrayList<Server>) cloud.getServers();
			LOG = LOG + "Current server size: " + servers.size();
			System.out.println("Current server size: " + servers.size());
			
			//from second round, remove expired vms
			if(i != 0){
				for(Server s : servers){
					if(debug){
						System.out.println("Server " + s.getId());
						LOG = LOG + " Server " + s.getId() + "\n";
					}
					
					for(Vhost v: s.getVhosts()){
						if(v.getVhostStatus().equals(v.getVhostStatus().DOWN)){
							continue;
						}
						//decrease term
						int term_t = v.getTerm() - 1;
						
						
						if(term_t <= 0){
							//end of life, turn off
							v.setVhostStatus(v.getVhostStatus().DOWN);
							if(debug){
								System.out.print(" removed vhost " + v.getId() + ": " + v.getType());
								LOG = LOG + " removed vhost " + v.getId() + ": " + v.getType() + "\n";
							}
							
							//add back released cpu power
							//System.out.print(" before " + v.getServer().getCpuUnused());
							s.setCpuUnused(s.getCpuUnused() + vcpu.get(v.getType()));
							//System.out.println(" after " + v.getServer().getCpuUnused());
							
						}else{
							//update the term
							v.setTerm(term_t);
						}
					}
				}
			}
			if(debug){
				LOG = LOG + "After removing expired VMs.\n";
				showVmCPU(cloud);
				LOG = LOG + "-----------------------------\n";
			}
			
			//sort 10 requests in descending order
			ArrayList<Request> requestsOrdered = new ArrayList<>();
			
			for(int j=i; j<i+10; j++){
				Request request = requests.get(j);
				requestsOrdered.add(request);
			}
			Collections.sort(requestsOrdered, RequestComparator);
			
			for(int n=0; n<10; n++){
				Vhost vh = null;
				boolean found = false;
				
				Request request = requestsOrdered.get(n);
				//System.out.println("Serverlist size " + servers.size());
				System.out.println("Request " + n + ": " + request.getType());
				
				for(Server s : servers){
					//System.out.println("Server_" + s.getId() + " cpu: " + s.getCpuUnused());
					//found available space in an existing server
					if(vcpu.get(request.getType()) <= s.getCpuUnused() && found == false){
						//create vhost
						//System.out.println(vcpu.get(request.getType()) + "<" + s.getCpuUnused());
						vh = new Vhost();
						vh.setServer(s);
						vh.setType(request.getType());
						vh.setVhostStatus(vh.getVhostStatus().RUNNING);
						vh.setTerm(request.getTerm());
						//System.out.println("before: " + s.getCpuUnused());
						s.setCpuUnused(s.getCpuUnused() - vcpu.get(request.getType()));
						//System.out.println("after: " + s.getCpuUnused());
						//update vhost list in Server
						ArrayList<Vhost> vhlist = null;
						if(s.getVhosts() == null){
							vhlist = new  ArrayList<>();
							vhlist.add(vh);
						}else{
							vhlist = (ArrayList<Vhost>) s.getVhosts();
							vhlist.add(vh);
						}
						s.setVhosts(vhlist);
						//System.out.println("Server id:" + s.getId() + " cpu-left " +  s.getCpuUnused() + " " + s.getVhosts().size());
						//found location already, get out of the loop, go next request
						found = true;
					}else{
						//check next server
						continue;
					}
					System.out.println("Server_" + s.getId() + " cpu: " + s.getCpuUnused());
				}
				
				if(vh == null){
					//no server available
					//create new server, add to cloud
					Server newS = new Server(servers.size());
					System.out.println("Create new server" + newS.getId());
					newS.setCloud(cloud);
					newS.setServerStatus(newS.getServerStatus().RUNNING);
					
					//bring up the new host
					vh = new Vhost();
					vh.setType(request.getType());
					vh.setTerm(request.getTerm());
					vh.setVhostStatus(vh.getVhostStatus().RUNNING);
					vh.setServer(newS);
					newS.setCpuUnused(newS.getCpuUnused() - vcpu.get(request.getType()));
					
					//update Vhost list in this Server obj
					ArrayList<Vhost> vhlist = new  ArrayList<>();
					vhlist.add(vh);
					newS.setVhosts(vhlist);
					
					servers.add(newS);
					cloud.setServers(null);
					cloud.setServers(servers);
				}
			}
			
			//need to turn off vhost which reaches to its life span
			
			System.out.println(" -------------------------------------------------------------");
			System.out.println("End of group: " + i/10 + " running server : " + servers.size() );
			LOG = LOG + " -------------------------------------------------------------\n";
			LOG = LOG + "End of group: " + i/10 + " running server : " + servers.size() + "\n";
			LOG = LOG + "\n";
			Graph_LOG = Graph_LOG + i/10 + "," + servers.size() + "\n";
			
			System.out.print("\n");
			showCloud(cloud); 
			System.out.println("\n ----------------------------------------------------------");
		}
		
		
		System.out.println("out....");
		return cloud;
	}
	
	//find the smallest one in the availables
	public static Cloud bestFit(Cloud cloud, List<Request> requests, boolean Balanced){
		
		//get list of servers in the cloud
		ArrayList<Server> servers = null;
		
		if(Balanced == true){
			LOG = LOG + "Method: Balanced fit\n";
		}else{
			LOG = LOG + "Method: Best fit\n";
		}
		//process every 10 request as a time slot
		for(int i=0; i<requests.size(); i=i+10){
				System.out.println("Request group: " + i);
				LOG = LOG + "------------------------------------------------------------------------------\n";
				LOG = LOG + "Request group: " + i + "\n";
				
				servers = (ArrayList<Server>) cloud.getServers();
				System.out.println("Current server size: " + servers.size());
				
				//from second round, remove expired vms
				if(i != 0){
					for(Server s : servers){
						if(debug){
							System.out.println("Server " + s.getId());
							LOG = LOG + " Server " + s.getId() + "\n";
						}
						
						for(Vhost v: s.getVhosts()){
							if(v.getVhostStatus().equals(v.getVhostStatus().DOWN)){
								continue;
							}
							//decrease term
							int term_t = v.getTerm() - 1;
							
							
							if(term_t <= 0){
								//end of life, turn off
								v.setVhostStatus(v.getVhostStatus().DOWN);
								if(debug){
									System.out.print(" removed vhost " + v.getId() + ": " + v.getType());
									LOG = LOG + " removed vhost " + v.getId() + ": " + v.getType() + "\n";
								}
								
								//add back released cpu power
								//System.out.print(" before " + v.getServer().getCpuUnused());
								s.setCpuUnused(s.getCpuUnused() + vcpu.get(v.getType()));
								//System.out.println(" after " + v.getServer().getCpuUnused());
								
							}else{
								//update the term
								v.setTerm(term_t);
							}
						}
					}
				}
				if(debug){
					LOG = LOG + "After removing expired VMs.\n";
					showVmCPU(cloud);
					LOG = LOG + "-----------------------------\n";
				}
				//loop through every one of the 10 requests
				for(int j=i; j<i+10; j++){
					Vhost vh = null;
					boolean found = false;
					
					//sort the servers, in small -> large order, so fit the smallest available space first
					Collections.sort(servers, ServerComparator);
					
					//balanced fit : fit the server with more space left
					if(Balanced == true){
						Collections.reverse(servers);
					}
					
					//System.out.println("\n");
					
					Request request = requests.get(j);
					//System.out.println("Serverlist size " + servers.size());
					//System.out.println("Request " + j + ": " + request.getType());
					if(debug){
						LOG = LOG + "Request " + j + ": " + request.getType() + "\n";
					}
					//check each running server to find availability
					for(Server s : servers){
						//System.out.println("Server_" + s.getId() + " cpu: " + s.getCpuUnused());
						//if(debug){
							//LOG = LOG + "Server_" + s.getId() + " cpu: " + s.getCpuUnused() + "\n";
						//}
						
						//found available space in an existing server
						if(vcpu.get(request.getType()) <= s.getCpuUnused() && found == false){
							//create vhost
							//System.out.println(vcpu.get(request.getType()) + "<" + s.getCpuUnused());
							vh = new Vhost();
							vh.setId(s.getVhosts().size() + 1);
							vh.setServer(s);
							vh.setType(request.getType());
							vh.setVhostStatus(vh.getVhostStatus().RUNNING);
							vh.setTerm(request.getTerm());
							//System.out.println("before: " + s.getCpuUnused());
							s.setCpuUnused(s.getCpuUnused() - vcpu.get(request.getType()));
							if(debug){
								System.out.println("after: " + s.getCpuUnused());
								LOG = LOG + "after: " + s.getCpuUnused() + "\n";
							}
							
							//update vhost list in Server
							ArrayList<Vhost> vhlist = null;
							if(s.getVhosts() == null){
								vhlist = new  ArrayList<>();
								vhlist.add(vh);
							}else{
								vhlist = (ArrayList<Vhost>) s.getVhosts();
								vhlist.add(vh);
							}
							s.setVhosts(vhlist);
							//System.out.println("Server id:" + s.getId() + " cpu-left " +  s.getCpuUnused() + " " + s.getVhosts().size());
							//found location already, get out of the loop, go next request
							found = true;
						}else{
							//check next server
							continue;
						}
					}
					if(vh == null){
						//no server available
						//create new server, add to cloud
						Server newS = new Server(servers.size());
						if(debug){
							System.out.println("Create new server" + newS.getId());
							LOG = LOG + "Create new server" + newS.getId() + "\n";
						}
						newS.setCloud(cloud);
						newS.setServerStatus(newS.getServerStatus().RUNNING);
						
						//bring up the new host
						vh = new Vhost();
						vh.setId(0);
						vh.setType(request.getType());
						vh.setTerm(request.getTerm());
						vh.setVhostStatus(vh.getVhostStatus().RUNNING);
						vh.setServer(newS);
						newS.setCpuUnused(newS.getCpuUnused() - vcpu.get(request.getType()));
						
						//update Vhost list in this Server obj
						ArrayList<Vhost> vhlist = new  ArrayList<>();
						vhlist.add(vh);
						
						newS.setVhosts(vhlist);
						//System.out.println("vlist " + newS.getVhosts().size());
						
						servers.add(newS);
						cloud.setServers(null);
						cloud.setServers(servers);
					}
				}
				System.out.println(" -------------------------------------------------------------");
				System.out.println("End of group: " + i/10 + " running server : " + servers.size() );
				LOG = LOG + " -------------------------------------------------------------\n";
				LOG = LOG + "End of group: " + i/10 + " running server : " + servers.size() + "\n";
				Graph_LOG = Graph_LOG + i/10 + "," + servers.size() + "\n";	
				//show server cpu level
				/**if(debug){
					showVmCPU(cloud);
				}*/
				//at the end of the time lot, remove those expired
				/**servers = (ArrayList<Server>) cloud.getServers();
				
				for(Server s : servers){
					System.out.println("Server " + s.getId());
					for(Vhost v: s.getVhosts()){
						if(v.getVhostStatus().equals(v.getVhostStatus().DOWN)){
							continue;
						}
						//decrease term
						int term_t = v.getTerm() - 1;
						
						
						if(term_t <= 0){
							//end of life, turn off
							v.setVhostStatus(v.getVhostStatus().DOWN);
							if(debug){
								System.out.print(" removed vhost " + v.getId() + ": " + v.getType());
								LOG = LOG + " removed vhost " + v.getId() + ": " + v.getType();
							}
							
							//add back released cpu power
							//System.out.print(" before " + v.getServer().getCpuUnused());
							s.setCpuUnused(s.getCpuUnused() + vcpu.get(v.getType()));
							//System.out.println(" after " + v.getServer().getCpuUnused());
							
						}else{
							//update the term
							v.setTerm(term_t);
						}
					}
				}*/
				System.out.print("\n");
				LOG = LOG + "\n";
				showCloud(cloud); 
				System.out.println("\n ----------------------------------------------------------");
				
		}
		
		return cloud;
	}

	//best fit decreasing
	public static Cloud BFD(Cloud cloud, List<Request> requests, boolean Balanced){
		
		//get list of servers in the cloud
		ArrayList<Server> servers = null;
		LOG = LOG + "Method: Best fit decreasing\n";
		
		//process every 10 request as a time slot
		for(int i=0; i<requests.size(); i=i+10){
				System.out.println("Request group: " + i);
				LOG = LOG + "------------------------------------------------------------------------------\n";
				LOG = LOG + "Request group: " + i + "\n";
					
				servers = (ArrayList<Server>) cloud.getServers();
				System.out.println("Current server size: " + servers.size());
				
				//from second round, remove expired vms
				if(i != 0){
					for(Server s : servers){
						if(debug){
							System.out.println("Server " + s.getId());
							LOG = LOG + " Server " + s.getId() + "\n";
						}
						
						for(Vhost v: s.getVhosts()){
							if(v.getVhostStatus().equals(v.getVhostStatus().DOWN)){
								continue;
							}
							//decrease term
							int term_t = v.getTerm() - 1;
							
							if(term_t <= 0){
								//end of life, turn off
								v.setVhostStatus(v.getVhostStatus().DOWN);
								if(debug){
									System.out.print(" removed vhost " + v.getId() + ": " + v.getType());
									LOG = LOG + " removed vhost " + v.getId() + ": " + v.getType() + "\n";
								}
								
								//add back released cpu power
								//System.out.print(" before " + v.getServer().getCpuUnused());
								s.setCpuUnused(s.getCpuUnused() + vcpu.get(v.getType()));
								//System.out.println(" after " + v.getServer().getCpuUnused());
								
							}else{
								//update the term
								v.setTerm(term_t);
							}
						}
					}
				}
				if(debug){
					LOG = LOG + "After removing expired VMs.\n";
					showVmCPU(cloud);
					LOG = LOG + "-----------------------------\n";
				}
				
				//sort 10 requests in descending order
				ArrayList<Request> requestsOrdered = new ArrayList<>();
				
				for(int j=i; j<i+10; j++){
					Request request = requests.get(j);
					requestsOrdered.add(request);
				}
				Collections.sort(requestsOrdered, RequestComparator);
				//for balanced fit
				if(Balanced == true){
					Collections.reverse(requestsOrdered);
				}
				
				//loop through every one of the 10 requests
				for(int n=0; n<10; n++){
					Vhost vh = null;
					boolean found = false;
					
					//sort the servers, in small -> large order, so fit the smallest available space first
					Collections.sort(servers, ServerComparator);
					
					//balanced fit : fit the server with more space left
					if(Balanced == true){
						Collections.reverse(servers);
					}
					
					//System.out.println("\n");
					
					Request request = requestsOrdered.get(n);
					
					//System.out.println("Serverlist size " + servers.size());
					//System.out.println("Request " + j + ": " + request.getType());
					if(debug){
						int index = n+i;
						LOG = LOG + "Request " + index + ": " + requestsOrdered.get(n).getType() + "\n";
					}
					//check each running server to find availability
					for(Server s : servers){
						//System.out.println("Server_" + s.getId() + " cpu: " + s.getCpuUnused());
						//if(debug){
							//LOG = LOG + "Server_" + s.getId() + " cpu: " + s.getCpuUnused() + "\n";
						//}
						
						//found available space in an existing server
						if(vcpu.get(request.getType()) <= s.getCpuUnused() && found == false){
							//create vhost
							//System.out.println(vcpu.get(request.getType()) + "<" + s.getCpuUnused());
							vh = new Vhost();
							vh.setId(s.getVhosts().size() + 1);
							vh.setServer(s);
							vh.setType(request.getType());
							vh.setVhostStatus(vh.getVhostStatus().RUNNING);
							vh.setTerm(request.getTerm());
							//System.out.println("before: " + s.getCpuUnused());
							s.setCpuUnused(s.getCpuUnused() - vcpu.get(request.getType()));
							if(debug){
								System.out.println("after: " + s.getCpuUnused());
								LOG = LOG + "after: " + s.getCpuUnused() + "\n";
							}
							
							//update vhost list in Server
							ArrayList<Vhost> vhlist = null;
							if(s.getVhosts() == null){
								vhlist = new  ArrayList<>();
								vhlist.add(vh);
							}else{
								vhlist = (ArrayList<Vhost>) s.getVhosts();
								vhlist.add(vh);
							}
							s.setVhosts(vhlist);
							//System.out.println("Server id:" + s.getId() + " cpu-left " +  s.getCpuUnused() + " " + s.getVhosts().size());
							//found location already, get out of the loop, go next request
							found = true;
						}else{
							//check next server
							continue;
						}
					}
					if(vh == null){
						//no server available
						//create new server, add to cloud
						Server newS = new Server(servers.size());
						if(debug){
							System.out.println("Create new server" + newS.getId());
							LOG = LOG + "Create new server" + newS.getId() + "\n";
						}
						newS.setCloud(cloud);
						newS.setServerStatus(newS.getServerStatus().RUNNING);
						
						//bring up the new host
						vh = new Vhost();
						vh.setId(0);
						vh.setType(request.getType());
						vh.setTerm(request.getTerm());
						vh.setVhostStatus(vh.getVhostStatus().RUNNING);
						vh.setServer(newS);
						newS.setCpuUnused(newS.getCpuUnused() - vcpu.get(request.getType()));
						
						//update Vhost list in this Server obj
						ArrayList<Vhost> vhlist = new  ArrayList<>();
						vhlist.add(vh);
						
						newS.setVhosts(vhlist);
						//System.out.println("vlist " + newS.getVhosts().size());
						
						servers.add(newS);
						cloud.setServers(null);
						cloud.setServers(servers);
					}
				}
				System.out.println(" -------------------------------------------------------------");
				System.out.println("End of group: " + i/10 + " running server : " + servers.size() );
				LOG = LOG + " -------------------------------------------------------------\n";
				LOG = LOG + "End of group: " + i/10 + " running server : " + servers.size() + "\n";
				Graph_LOG = Graph_LOG + i/10 + "," + servers.size() + "\n";
				
				//show server cpu level
				/**if(debug){
					showVmCPU(cloud);
				}*/
				//at the end of the time lot, remove those expired
				
				System.out.print("\n");
				LOG = LOG + "\n";
				showCloud(cloud); 
				System.out.println("\n ----------------------------------------------------------");
				
		}
		
		return cloud;
	}
	
	//this round:
	//get expiring space first, then fit predicted VMs to those spaces
	//left will be reserved in the current space
	//when there's perfect fit for the predicted type of vm, then reserve for the predicted vm
	//next round:
	//process 10 request and fit those request predicted into the reserved space, rest fits normally
	//since when reserve the space, the cpu usage has been counted, no more reduce of cpu amount when apply vm
	public static Cloud bestFitPrediction(Cloud cloud, List<Request> requests, boolean Balanced){
		
		//Map(requestId, server)
		Map<Integer, Server> predicted = new TreeMap<>();
		Set<Request> reserved = new HashSet<>();
		
		//get list of servers in the cloud
		ArrayList<Server> servers = null;
		
		if(Balanced == true){
			LOG = LOG + "Method: Balanced fit with Prediction\n";
		}else{
			LOG = LOG + "Method: Best fit with Prediction\n";
		}
		
		//process every 10 request as a time slot
		for(int i=0; i<requests.size(); i=i+10){
			System.out.println("Request group: " + i);
			LOG = LOG + "------------------------------------------------------------------------------\n";
			LOG = LOG + "Request group: " + i + "\n";
				
			servers = (ArrayList<Server>) cloud.getServers();
			System.out.println("Current server size: " + servers.size());
			
			//sort in increasing order
			Collections.sort(servers, ServerComparator);
			
			//for VMs expiring in next round
			Map<Vhost,Integer> expiringVMs = new HashMap<>();
			
			
			//---------------------------------------
			//from second round, remove expired vms
			//---------------------------------------
			if(i != 0){
				for(Server s : servers){
					if(debug){
						System.out.println("Server " + s.getId());
						LOG = LOG + " Server " + s.getId() + "\n";
					}
					for(Vhost v: s.getVhosts()){
						if(v.getVhostStatus().equals(v.getVhostStatus().DOWN)){
							continue;
						}
						//decrease term
						int term_t = v.getTerm() - 1;
						
						//turn off expired VMS
						if(term_t <= 0){
							//end of life, turn off
							v.setVhostStatus(v.getVhostStatus().DOWN);
							if(debug){
								System.out.print(" removed vhost " + v.getId() + ": " + v.getType() + " term: " + v.getTerm());
								LOG = LOG + " removed vhost " + v.getId() + ": " + v.getType() + " term: " + v.getTerm() + "\n";
							}
							//add released cpu power back 
							//System.out.print(" before " + v.getServer().getCpuUnused());
							LOG = LOG + " before " + v.getServer().getCpuUnused() + "\n";
							s.setCpuUnused(s.getCpuUnused() + vcpu.get(v.getType()));
							//System.out.println(" after " + v.getServer().getCpuUnused());
							LOG = LOG + "after " + + v.getServer().getCpuUnused() + "\n";
							//remove the vm from expiring list since it has expired already
							//expiringVMs.remove(v);
						
						//add VM to expiring list if they have only 1 term left
						}else if(term_t == 1){
							//vms expiring in the next round are added to expiringVMs list
							expiringVMs.put(v, 1);	
							LOG = LOG + "expiring vm " + v.getId() + " term: " + v.getTerm() + "\n";
							v.setTerm(term_t);
						
						}else{
							//update the term
							v.setTerm(term_t);
						}
					}
				}
			}
			if(debug){
				LOG = LOG + "After removing expired VMs.\n";
				showVmCPU(cloud);
				LOG = LOG + "-----------------------------\n";
			}
			
			//consider the next 10 requests as prediction and add to future request list
			ArrayList<Request> futureRequests = new ArrayList<>();
			//if it's not the last round
			if((i > 0 ) && (i + 10) < requests.size()){
				for(int p=i+10; p<i+20; p++){
					Request request = requests.get(p);
					futureRequests.add(request);
				}
			}
			//sort in increasing order
			Collections.sort(futureRequests, RequestComparator);
			//sort in decreasing order
			Collections.reverse(futureRequests);
			
			//-------------------------------------------------------------------------------------------------------
			//find future space for future request first, when there's no future space, arrange it in current space
			//a request match perfectly to a expiring VM -> use that space for this future request
			//otherwise add the expiring VM space to each server's futureCPU
			//---------------------------------------------------------------------
			//if it's not the last round
			//get next groupID
			Integer groupId = i+10;
			LOG = LOG + "i=" + i + " groupId = " + groupId;
			if(futureRequests.size() > 0){
				for(Request request : futureRequests){
					//request with non -1 groupId means this request is for the future
					request.setGroupId(groupId);
					LOG = LOG + "rid: " + request.getId() + " set gid: " + groupId + " " + request.getType() + "\n";
					
					for(Vhost vm: expiringVMs.keySet()){
						//when type matches, use this space for the future request
						if(vm.getType().equals(request.getType()) && expiringVMs.get(vm) != 0){
							//remember the reserved space
							Server reservedServer = vm.getServer();
							predicted.put(request.getId(), reservedServer);
							LOG = LOG + "Add predicted request: " + request.getId() + " for server " + reservedServer.getId() + "\n";
							//set expiring term to 0 so we won't consider it again
							expiringVMs.put(vm, 0);
							//set reserved server ID for the request
							request.setReservedServerId(vm.getServer().getId());
							//break the loop for the next request
							break;
						
						//add vm space to futureCPU of the server
						}
					}
				}
				// when no perfect match, add all the expiring vm space to each server's futureCPU
				//Map<Integer, Integer> futureCPU;
				for(Vhost vm: expiringVMs.keySet()){
					if(expiringVMs.get(vm) != 0){
						Map<Integer, Integer> futureCPU = vm.getServer().getFutureCPU();
						if(futureCPU.get(groupId) != null){
							LOG = LOG + "future " + futureCPU.get(groupId) + "\n";
							futureCPU.put(groupId, futureCPU.get(groupId) + vcpu.get(vm.getType()) );
							LOG = LOG + "future to " + futureCPU.get(groupId) + "\n";
						}else{
							futureCPU.put(groupId, vcpu.get(vm.getType()) );
							LOG = LOG + "set future " + futureCPU.get(groupId) + "\n";
						}
						vm.getServer().setFutureCPU(futureCPU);
						expiringVMs.put(vm, 0);
					}
				}
			}
			
			//request pool for current + future requests
			ArrayList<Request> requestPool = new ArrayList<>();
			
			//loop through futureRequests again and find out all the future requests without reserved space
			//and fit them into spaces opening up in the future
			for(Request request : futureRequests){
				boolean found = false;
				//request groupId != -1 means future request, ReservedServerId == -1 means no perfect match for the future spot
				if(request.getGroupId() != -1 && request.getReservedServerId() == -1){
					ServerLoop:
						for(Server s : servers){
							for(int gid : s.getFutureCPU().keySet()){
								if((gid==i+1) && s.getFutureCPU().get(gid) > vcpu.get(request.getType())){
									//really need this??
									request.setReservedServerId(s.getId());
									//decrease future cpu level
									int new_fcpu = s.getFutureCPU().get(gid) - vcpu.get(request.getType());
									Map<Integer, Integer> futureCPU = s.getFutureCPU();
									futureCPU.replace(gid, new_fcpu);
									LOG = LOG + "server " + s.getId() + " fcpu " + s.getFutureCPU().get(gid) + " ";
									//update the futureCPU of this server since the space is reserved
									s.setFutureCPU(futureCPU);
									found = true;
									//get out of the loop for next request
									//stop looping through servers
									break ServerLoop;
								}
							}
						}
				//current requests which are not predicted
				// they have to fit in current spaces
				}
				//no future space fits this request, then add to the pool
				if(found == false){
					requestPool.add(request);
				}
			}
			//also add the current 10 requests to requestPool
			for(int j=i; j<i+10; j++){
				Request request = requests.get(j);
				requestPool.add(request);
			}
			
			//allocation space for reserved request first
			for(Request request : requestPool){
				Vhost vh = null;
				//??????????????????????????????????????????????????????????
				//fits to the reserved space if there's any perfect match
				if((request.getGroupId() == i) && (request.getReservedServerId() != -1)){
					LOG = LOG + " try to use reserved server\n "; 
					Server reservedS = null;
					for(Server serv: servers){
						if(serv.getId() == request.getReservedServerId()){
							reservedS = serv;
							LOG = LOG + "use reserved server " + request.getReservedServerId() + " for " + request.getId() + "\n";
						}
					}
					vh = new Vhost();
					vh.setId(reservedS.getVhosts().size() + 1);
					vh.setServer(reservedS);
					vh.setType(request.getType());
					vh.setVhostStatus(vh.getVhostStatus().RUNNING);
					vh.setTerm(request.getTerm());
					reservedS.setCpuUnused(reservedS.getCpuUnused() - vcpu.get(request.getType()));
					if(debug){
						System.out.println("after: " + reservedS.getCpuUnused());
						LOG = LOG + "after: " + reservedS.getCpuUnused() + "\n";
					}
					//update vhost list in Server
					ArrayList<Vhost> vhlist = null;
					if(reservedS.getVhosts() == null){
						vhlist = new  ArrayList<>();
						vhlist.add(vh);
					}else{
						vhlist = (ArrayList<Vhost>) reservedS.getVhosts();
						vhlist.add(vh);
					}
					reservedS.setVhosts(vhlist);
					request.setFoundLoc(true);
				}
				//LOG = LOG + "reserved?? ";
				//??????????????????????????????????????????????????????????
			}
			
			
			//loop through the requestPool and allocate space
			//future request can take current spot if no more future space
			//current request can only use current space
			
			//sort in increasing order
			Collections.sort(requestPool, RequestComparator);
			//sort in decreasing order
			Collections.reverse(requestPool);
			
			for(Request request : requestPool){
				Vhost vh = null;
				boolean found = false;
				LOG = LOG + "request groupID " + request.getGroupId() + " reservedServer " + request.getReservedServerId() + " request ID " + request.getId()+ " " + request.getType() +"\n";
				LOG = LOG + "groupID " + request.getGroupId() + " i= "  +  i +  " " + request.getReservedServerId() + "\n";
				if(request.hasFoundLoc()){
					continue;
				}
				for(Server s : servers){
					//System.out.println("Server_" + s.getId() + " cpu: " + s.getCpuUnused());
					if(debug){
						LOG = LOG + "Server_" + s.getId() + " cpu: " + s.getCpuUnused() + "\n";
					}
					//found available space in an existing server
					if(vcpu.get(request.getType()) <= s.getCpuUnused() && found == false ){
						//create vhost
						//System.out.println(vcpu.get(request.getType()) + "<" + s.getCpuUnused());
						
						//future request in first round
						if(i==0 && request.getGroupId() != -1){
						//if(request.getGroupId() != -1){
							//reserver space, but we'll release this space later at the end of this term since it's only reservation
							s.setReservedCPU(s.getReservedCPU() + vcpu.get(request.getType()));
							s.setCpuUnused(s.getCpuUnused() - vcpu.get(request.getType()));
							LOG = LOG + "requestId " + request.getId() + " reservedFor " + s.getReservedCPU() + "\n";
							found = true;
							
						//future request after first round
						}
						else if(i > 0 && request.getGroupId() > i){
							//reserver space, but we'll release this space later at the end of this term since it's only reservation
							s.setReservedCPU(s.getReservedCPU() + vcpu.get(request.getType()));
							s.setCpuUnused(s.getCpuUnused() - vcpu.get(request.getType()));
							LOG = LOG + "requestId " + request.getId() + " reserved " + s.getReservedCPU() + "\n";
							found = true;
						//current request
						}
						else{
							vh = new Vhost();
							vh.setId(s.getVhosts().size() + 1);
							vh.setServer(s);
							vh.setType(request.getType());
							vh.setVhostStatus(vh.getVhostStatus().RUNNING);
							vh.setTerm(request.getTerm());
							//System.out.println("before: " + s.getCpuUnused());
							s.setCpuUnused(s.getCpuUnused() - vcpu.get(request.getType()));
							if(debug){
								System.out.println("after: " + s.getCpuUnused());
								LOG = LOG + "after: " + s.getCpuUnused() + "\n";
							}
							//update vhost list in Server
							ArrayList<Vhost> vhlist = null;
							if(s.getVhosts() == null){
								vhlist = new  ArrayList<>();
								vhlist.add(vh);
							}else{
								vhlist = (ArrayList<Vhost>) s.getVhosts();
								vhlist.add(vh);
							}
							s.setVhosts(vhlist);
							//System.out.println("Server id:" + s.getId() + " cpu-left " +  s.getCpuUnused() + " " + s.getVhosts().size());
							//found location already, get out of the loop, go next request
							found = true;
						}
						
					}else{
						//check next server
						continue;
					}
				}
				if(vh == null && found == false){
					//no server available
					//create new server, add to cloud
					
					Server newS = new Server(servers.size());
					if(debug){
						System.out.println("Create new server" + newS.getId());
						LOG = LOG + "Create new server" + newS.getId() + "\n";
					}
					newS.setCloud(cloud);
					newS.setServerStatus(newS.getServerStatus().RUNNING);
					
					//for future request
					/**if(request.getGroupId() != -1){
						newS.setReservedCPU(newS.getReservedCPU() + vcpu.get(request.getType()));
						newS.setCpuUnused(newS.getCpuUnused() - vcpu.get(request.getType()));
						found = true;
					}*/
					if(i==0 && request.getGroupId() != -1){
						newS.setReservedCPU(newS.getReservedCPU() + vcpu.get(request.getType()));
						newS.setCpuUnused(newS.getCpuUnused() - vcpu.get(request.getType()));
						found = true;
						LOG = LOG + "requestId " + request.getId() + " reservedFor " + newS.getReservedCPU() + "\n";
						//update Vhost list in this Server obj
						ArrayList<Vhost> vhlist = new  ArrayList<>();
						newS.setVhosts(vhlist);
					}
					else if(i > 0 && request.getGroupId() != i && request.getGroupId() != -1){
						newS.setReservedCPU(newS.getReservedCPU() + vcpu.get(request.getType()));
						newS.setCpuUnused(newS.getCpuUnused() - vcpu.get(request.getType()));
						found = true;
						LOG = LOG + "requestId " + request.getId() + " reservedFor " + newS.getReservedCPU() + "\n";
						//update Vhost list in this Server obj
						ArrayList<Vhost> vhlist = new  ArrayList<>();
						newS.setVhosts(vhlist);
					}else{
						//bring up the new host
						vh = new Vhost();
						vh.setId(0);
						vh.setType(request.getType());
						vh.setTerm(request.getTerm());
						vh.setVhostStatus(vh.getVhostStatus().RUNNING);
						vh.setServer(newS);
						newS.setCpuUnused(newS.getCpuUnused() - vcpu.get(request.getType()));
						
						//update Vhost list in this Server obj
						ArrayList<Vhost> vhlist = new  ArrayList<>();
						vhlist.add(vh);
						newS.setVhosts(vhlist);
					}
					
					
					//System.out.println("vlist " + newS.getVhosts().size());
					servers.add(newS);
					cloud.setServers(null);
					cloud.setServers(servers);
				}
			}
			System.out.println(" -------------------------------------------------------------");
			System.out.println("End of group: " + i/10 + " running server : " + servers.size() );
			LOG = LOG + " -------------------------------------------------------------\n";
			LOG = LOG + "End of group: " + i/10 + " running server : " + servers.size() + "\n";
			Graph_LOG = Graph_LOG + i/10 + "," + servers.size() + "\n";	
			
			System.out.print("\n");
			LOG = LOG + "\n";
			showCloud(cloud); 
			System.out.println("\n ----------------------------------------------------------");
			
			//release reserved cpu since they are not requested in this round
			for(Server s : servers){
				if(s.getReservedCPU() > 0){
					s.setCpuUnused(s.getCpuUnused() + s.getReservedCPU());
					s.setReservedCPU(0);
				}
				LOG = LOG + "after released for reservation " + s.getId() + " " + s.getCpuUnused() + "\n";
			}
		}	
		
		return cloud;
	}
	
	
	//consider the next term incoming and outgoing vms in this term
	public static Cloud bestFitPrediction1(Cloud cloud, List<Request> requests){
		
		//Map(requestId, server)
		Map<Integer, Server> predicted = new TreeMap<>();
		Set<Request> reserved = new HashSet<>();
		
		//get list of servers in the cloud
		ArrayList<Server> servers = null;
		
		//process every 10 request as a time slot
		for(int i=0; i<requests.size(); i=i+10){
				System.out.println("Request group: " + i);
				LOG = LOG + "-------------------------------------------\n";
				LOG = LOG + "Request group: " + i + "\n";
					
				servers = (ArrayList<Server>) cloud.getServers();
				System.out.println("Current server size: " + servers.size());
				
				//for VMs expiring in next round
				ArrayList<Vhost> expiringVMs = new ArrayList<>();
				
				//from second round, remove expired vms
				if(i != 0){
					for(Server s : servers){
						if(debug){
							System.out.println("Server " + s.getId());
							LOG = LOG + "Removing expired....\n Server " + s.getId() + "\n";
						}
						
						for(Vhost v: s.getVhosts()){
							if(v.getVhostStatus().equals(v.getVhostStatus().DOWN)){
								continue;
							}
							//decrease term
							int term_t = v.getTerm() - 1;
							
							if(term_t <= 0){
								//end of life, turn off
								v.setVhostStatus(v.getVhostStatus().DOWN);
								if(debug){
									System.out.print(" removed vhost " + v.getId() + ": " + v.getType());
									LOG = LOG + " removed vhost " + v.getId() + ": " + v.getType() + "\n";
								}
								
								//add back released cpu power
								//System.out.print(" before " + v.getServer().getCpuUnused());
								s.setCpuUnused(s.getCpuUnused() + vcpu.get(v.getType()));
								//System.out.println(" after " + v.getServer().getCpuUnused());
								expiringVMs.remove(v);
								
							}else if(term_t == 1){
									expiringVMs.add(v);	
									LOG = LOG + "expiring vm " + v.getId() + " term: " + v.getTerm() + "\n";
							}else{
								//update the term
								v.setTerm(term_t);
								LOG = LOG + v.getId() + " set term to: " + v.getTerm();
							}
						}
					}
				}
				if(debug){
					LOG = LOG + "After removing expired VMs.\n";
					showVmCPU(cloud);
					LOG = LOG + "-----------------------------\n";
				}
				
				//next 10 as prediction
				if((i + 10) < requests.size()){
					for(int p=i+10; p<i+20; p++){
						Request request = requests.get(p);
						for(Vhost vm: expiringVMs){
							//when expired type same as future request
							if(vm.getType().equals(request.getType())){
								//reserve
								Server reservedServer = vm.getServer();
								predicted.put(request.getId(), reservedServer);
								LOG = LOG + "Add predicted request: " + request.getId() + " for server " + reservedServer.getId() + "\n";
							}
						}
					}
				}
				
				//loop through every one of the 10 requests
				for(int j=i; j<i+10; j++){
					Vhost vh = null;
					boolean found = false;
					
					//sort the servers
					Collections.sort(servers, ServerComparator);
					
					//Collections.reverse(servers);
					System.out.println("\n");
					
					Request request = requests.get(j);
					//System.out.println("Serverlist size " + servers.size());
					System.out.println("Request " + j + ": " + request.getType());
					LOG = LOG + "Request " + j + ": " + request.getType() +  " " + request.getTerm() + "\n";
					
					if(predicted.containsKey(request.getId())){
						Server reservedServer = predicted.get(request.getId());
						LOG = LOG + "Found predicted request: " + request.getId() + " for server " + reservedServer.getId() + "\n";
						vh = new Vhost();
						vh.setId(reservedServer.getVhosts().size() + 1);
						vh.setServer(reservedServer);
						vh.setType(request.getType());
						vh.setVhostStatus(vh.getVhostStatus().RUNNING);
						vh.setTerm(request.getTerm());
						reservedServer.setCpuUnused(reservedServer.getCpuUnused() - vcpu.get(request.getType()));
						ArrayList<Vhost> vhlist = null;
						if(reservedServer.getVhosts() == null){
							vhlist = new  ArrayList<>();
							vhlist.add(vh);
						}else{
							vhlist = (ArrayList<Vhost>) reservedServer.getVhosts();
							vhlist.add(vh);
						}
						reservedServer.setVhosts(vhlist);
						//remove from predicted list
						predicted.remove(request.getId());
					}else{
						//check each running server to find availability
						for(Server s : servers){
							System.out.println("Server_" + s.getId() + " cpu: " + s.getCpuUnused());
							//found available space in an existing server
							if(vcpu.get(request.getType()) <= s.getCpuUnused() && found == false){
								//create vhost
								//System.out.println(vcpu.get(request.getType()) + "<" + s.getCpuUnused());
								vh = new Vhost();
								vh.setId(s.getVhosts().size() + 1);
								vh.setServer(s);
								vh.setType(request.getType());
								vh.setVhostStatus(vh.getVhostStatus().RUNNING);
								vh.setTerm(request.getTerm());
								System.out.println("before: " + s.getCpuUnused());
								s.setCpuUnused(s.getCpuUnused() - vcpu.get(request.getType()));
								System.out.println("after: " + s.getCpuUnused());
								//update vhost list in Server
								ArrayList<Vhost> vhlist = null;
								//add vhost to the server's vhost list
								if(s.getVhosts() == null){
									vhlist = new  ArrayList<>();
									vhlist.add(vh);
								}else{
									vhlist = (ArrayList<Vhost>) s.getVhosts();
									vhlist.add(vh);
								}
								s.setVhosts(vhlist);
								//System.out.println("Server id:" + s.getId() + " cpu-left " +  s.getCpuUnused() + " " + s.getVhosts().size());
								//found location already, get out of the loop, go next request
								found = true;
							}else{
								//check next server
								continue;
							}
						}
					}
					
					if(vh == null){
						//no server available
						//create new server, add to cloud
						Server newS = new Server(servers.size());
						System.out.println("Create new server" + newS.getId());
						newS.setCloud(cloud);
						newS.setServerStatus(newS.getServerStatus().RUNNING);
						
						//bring up the new host
						vh = new Vhost();
						vh.setId(0);
						vh.setType(request.getType());
						vh.setVhostStatus(vh.getVhostStatus().RUNNING);
						vh.setServer(newS);
						newS.setCpuUnused(newS.getCpuUnused() - vcpu.get(request.getType()));
						
						//update Vhost list in this Server obj
						ArrayList<Vhost> vhlist = new  ArrayList<>();
						vhlist.add(vh);
						
						newS.setVhosts(vhlist);
						//System.out.println("vlist " + newS.getVhosts().size());
						
						servers.add(newS);
						cloud.setServers(null);
						cloud.setServers(servers);
					}
				}
				
				System.out.println(" -------------------------------------------------------------");
				System.out.println("End of group: " + i/10 + " running server : " + servers.size() );
				
				
				System.out.print("\n");
				showCloud(cloud); 
				System.out.println("\n ----------------------------------------------------------");
				
		}
		
		return cloud;
	}

	public static void main(String[] args) throws IOException  {
		
		vcpu = new HashMap<>();
		vcpu.put(Vtype.SMALL, 300);
		vcpu.put(Vtype.MEDIUM, 700);
		vcpu.put(Vtype.LARGE, 900);
		
		debug = true;
		
		//create cloud 1
		Cloud c = new Cloud(1);
		ArrayList<Server> serverList = new ArrayList<>();
		c.setServers(serverList);

		//initiate the first server
		Server s0 = new Server(0);
		s0.setCloud(c);
		serverList.add(s0);
		s0.setServerStatus(s0.getServerStatus().RUNNING);
		ArrayList<Vhost> vs = new ArrayList<>();
		s0.setVhosts(vs);
		
		System.out.println("Cloud ID: " + c.getId());
		System.out.println("Running server: " + c.getServers().size());
		System.out.println("Server ID: " + s0.getId() + " Status: " + s0.getServerStatus());
		System.out.println("cpu level: " +  s0.getCpuUnused());
		
		/**Vhost vh1 = new Vhost();
		vh1.setId(0);
		vh1.setType(Vtype.SMALL);
		vh1.setVhostStatus(vh1.getVhostStatus().RUNNING);
		vh1.setServer(s0);
		ArrayList<Vhost> vs = null;
		if(s0.getVhosts() == null){
			vs = new ArrayList<>();
		}else{
			vs = (ArrayList<Vhost>) s0.getVhosts();
		}
		vs.add(vh1);
		s0.setVhosts(vs);
		s0.setCpuUnused(s0.getCpuUnused() - vcpu.get(vh1.getType()));
		
		System.out.println("Host " + vh1.getId() + " Type: " + vh1.getType() + " cpu: " + vcpu.get(vh1.getType()));
		System.out.println("Server has vhosts: " + s0.getVhosts().size());
		
		s0.setCpuUnused(s0.getCpuUnused() - vcpu.get(vh1.getType()));
		System.out.println("Server " + vh1.getServer().getId() + " cpu level: " + s0.getCpuUnused());*/
		//read in requests
		
		ArrayList<Request> requests = new ArrayList<>();
		requests = (ArrayList<Request>) readRequest("./src/vcloud/request.list");
		
		//c = firstFit(c, requests);
		//c = bestFit(c, requests, false);
		c = bestFitPrediction(c, requests, false);
		//c = BFD(c, requests, false);
		//c = FFD(c, requests);
		//c = BFDPrediction(c, requests, true);
		try{
			
			FileWriter filewWriter = new FileWriter(LogPath);
			BufferedWriter buffer = new BufferedWriter(filewWriter);
			//buffer.write(Graph_LOG);
			buffer.write(LOG);
			buffer.close();
			filewWriter.close();
		}catch(IOException e){
			e.getMessage();
		}
		
		System.out.println(c.getServers().size());
	}

}
