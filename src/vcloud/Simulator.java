package vcloud;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Comparator;

public class Simulator {
	
	private static Map<Vtype, Integer> vcpu;
	private static String LOG;
	private static String LogPath = "src/log/simulator.log";
	private static boolean debug = false;
	
	
	//compare which server has more space left
	public static Comparator<Server> ServerComparator = new Comparator<Server>() {
		public int compare(Server s1, Server s2) {
			//more cpu power left then return 1
			return s1.getCpuUnused()- s2.getCpuUnused();
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
			req.setTerm(Integer.parseInt(request[1]) + 1);
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
				output = output + v.getType() + " (id:" + v.getId() + " term: " + v.getTerm() + " " + v.getVhostStatus() + " )";
			}
			//System.out.println("\n");
			output = output + "\n";
		}
		/**for(Server s : servers){
			//System.out.println("server cpu level: " + s.getCpuUnused() );
			output = output + "server cpu level: " + s.getId() + " " + s.getCpuUnused() + "\n";
		}*/
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
			
			
			//loop through every one of the 10 requests
			for(int j=i; j<i+10; j++){
				Vhost vh = null;
				boolean found = false;
				
				Request request = requests.get(j);
				//System.out.println("Serverlist size " + servers.size());
				System.out.println("Request " + j + ": " + request.getType());
				
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
			servers = (ArrayList<Server>) cloud.getServers();
			for(Server s : servers){
				for(Vhost v: s.getVhosts()){
					if(v.getVhostStatus().equals(v.getVhostStatus().DOWN)){
						continue;
					}
					//decrease term
					int term_t = v.getTerm() - 1;
					if(term_t <= 0){
						//end of life, turn off
						v.setVhostStatus(v.getVhostStatus().DOWN);
						System.out.print(" removed vhost " + v.getId() + ": " + v.getType());
						//add back released cpu power
						//System.out.print(v.getServer().getId() + " before " + v.getServer().getCpuUnused());
						s.setCpuUnused(s.getCpuUnused() + vcpu.get(v.getType()));;
						//System.out.println(" after " + v.getServer().getCpuUnused());
					}else{
						//update the term
						v.setTerm(term_t);
					}
				}
			}
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
				
				//loop through every one of the 10 requests
				for(int j=i; j<i+10; j++){
					Vhost vh = null;
					boolean found = false;
					
					//sort the servers, in small -> large order, so fit the smallest available space first
					Collections.sort(servers, ServerComparator);
					
					//balanced fit : fit the server with more space left
					Collections.reverse(servers);
					//System.out.println("\n");
					
					Request request = requests.get(j);
					//System.out.println("Serverlist size " + servers.size());
					//System.out.println("Request " + j + ": " + request.getType());
					if(debug){
						LOG = LOG + "Request " + j + ": " + request.getType() + "\n";
					}
					//check each running server to find availability
					for(Server s : servers){
						System.out.println("Server_" + s.getId() + " cpu: " + s.getCpuUnused());
						if(debug){
							LOG = LOG + "Server_" + s.getId() + " cpu: " + s.getCpuUnused() + "\n";
						}
						
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
				//show server cpu level
				if(debug){
					showVmCPU(cloud);
				}
				//at the end of the time lot, remove those expired
				servers = (ArrayList<Server>) cloud.getServers();
				
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
							s.setCpuUnused(s.getCpuUnused() + vcpu.get(v.getType()));;
							//System.out.println(" after " + v.getServer().getCpuUnused());
							
						}else{
							//update the term
							v.setTerm(term_t);
						}
					}
				}
				System.out.print("\n");
				LOG = LOG + "\n";
				showCloud(cloud); 
				System.out.println("\n ----------------------------------------------------------");
				
		}
		
		return cloud;
	}
	
	//consider the next term incoming and outgoing vms in this term
	public static Cloud bestFitPrediction(Cloud cloud, List<Request> requests){
		
		//get list of servers in the cloud
		ArrayList<Server> servers = null;
		
		//process every 10 request as a time slot
		for(int i=0; i<requests.size(); i=i+10){
				System.out.println("Request group: " + i);
					
				servers = (ArrayList<Server>) cloud.getServers();
				System.out.println("Current server size: " + servers.size());
				
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
					
					//class to check which vms are going to terminate next term
					//getEndofTermVM()
					
					
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
				
				//check next 10 requests and consider them as prediction
				//keep space for them as reservation
				if((i + 10) < requests.size()){
					for(int p=i+10; p<i+20; p++){
						Request p_request = requests.get(p);
					}
				}
				
				
				
				
				
				System.out.println(" -------------------------------------------------------------");
				System.out.println("End of group: " + i/10 + " running server : " + servers.size() );
				
				//at the end of the time lot, remove those expired
				servers = (ArrayList<Server>) cloud.getServers();
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
							System.out.print(" removed vhost " + v.getId() + ": " + v.getType());
							//add back released cpu power
							System.out.print( " before " + v.getServer().getCpuUnused());
							s.setCpuUnused(s.getCpuUnused() + vcpu.get(v.getType()));;
							System.out.println(" after " + v.getServer().getCpuUnused());
							
						}else{
							//update the term
							v.setTerm(term_t);
						}
					}
				}
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
		
		//create cloud 1
		Cloud c = new Cloud(1);
		ArrayList<Server> serverList = new ArrayList<>();
		c.setServers(serverList);

		//initiate the first server
		Server s0 = new Server(0);
		s0.setCloud(c);
		serverList.add(s0);
		s0.setServerStatus(s0.getServerStatus().RUNNING);
		
		System.out.println("Cloud ID: " + c.getId());
		System.out.println("Running server: " + c.getServers().size());
		System.out.println("Server ID: " + s0.getId() + " Status: " + s0.getServerStatus());
		System.out.println("cpu level: " +  s0.getCpuUnused());
		
		Vhost vh1 = new Vhost();
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
		System.out.println("Server " + vh1.getServer().getId() + " cpu level: " + s0.getCpuUnused());
		//read in requests
		
		ArrayList<Request> requests = new ArrayList<>();
		requests = (ArrayList<Request>) readRequest("./src/vcloud/request.list");
		
		
		//c = firstFit(c, requests);
		c = bestFit(c, requests, true);
		try{
			
			FileWriter filewWriter = new FileWriter(LogPath);
			BufferedWriter buffer = new BufferedWriter(filewWriter);
			buffer.write(LOG);
			buffer.close();
			filewWriter.close();
		}catch(IOException e){
			e.getMessage();
		}
		
		System.out.println(c.getServers().size());
	}

}
