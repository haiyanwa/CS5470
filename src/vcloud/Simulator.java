package vcloud;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Simulator {
	
	private static Map<Vtype, Integer> vcpu;
	
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
			req.setAmount(Integer.parseInt(request[1]));
			requests.add(req);
			num++;
		}
		return requests;
	}
	//show haw many physical servers running in a cloud and how many vh in each server
	public void showCloud(Cloud cloud){
		
	}
	
	public static Cloud firstFit(Cloud cloud, List<Request> requests){
		
		//get list of servers in the cloud
		ArrayList<Server> servers = null;
		
		//process every 10 request as a time slot
		for(int i=0; i<requests.size(); i=i+10){
			System.out.println("Request group: " + i);
			
			servers = (ArrayList<Server>) cloud.getServers();
			System.out.println("Current server size: " + servers.size());
			
			//loop through every one of the 10
			for(int j=i; j<i+10; j++){
				Vhost vh = null;
				Request request = requests.get(j);
				//System.out.println("Serverlist size " + servers.size());
				System.out.println("Request " + j + ": " + request.getType());
				
				//check each running server to find availability
				for(Server s : servers){
					System.out.println("Server_" + s.getId() + " cpu: " + s.getCpuUnused());
					if(vcpu.get(request.getType()) <= s.getCpuUnused()){
						//create vhost
						System.out.println(vcpu.get(request.getType()) + "<" + s.getCpuUnused());
						vh = new Vhost();
						vh.setServer(s);
						vh.setType(request.getType());
						vh.setVhostStatus(vh.getVhostStatus().RUNNING);
						s.setCpuUnused(s.getCpuUnused() - vcpu.get(request.getType()));
						//System.out.println("Server id:" + s.getId() + " left cpu " +  s.getCpuUnused());
						
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
					vh.setType(request.getType());
					vh.setVhostStatus(vh.getVhostStatus().RUNNING);
					vh.setServer(newS);
					servers.add(newS);
					cloud.setServers(null);
					cloud.setServers(servers);
				}
			}
			System.out.println("End of group: " + i/10 + " running server : " + servers.size() + " -------------------------");
		}
		
		
		System.out.println("out....");
		return cloud;
	}

	public static void main(String[] args) throws IOException  {
		
		vcpu = new HashMap<>();
		vcpu.put(Vtype.SMALL, 200);
		vcpu.put(Vtype.MEDIUM, 400);
		vcpu.put(Vtype.LARGE, 800);
		
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
		System.out.println("cpu level: " + s0.getCpuUnused());
		
		Vhost vh1 = new Vhost();
		vh1.setType(Vtype.SMALL);
		vh1.setVhostStatus(vh1.getVhostStatus().RUNNING);
		vh1.setServer(s0);
		
		System.out.println("Host Type: " + vh1.getType() + " cpu: " + vcpu.get(vh1.getType()));
		
		s0.setCpuUnused(s0.getCpuUnused() - vcpu.get(vh1.getType()));
		System.out.println("Server " + vh1.getServer().getId() + " cpu level: " + s0.getCpuUnused());
		//read in requests
		
		ArrayList<Request> requests = new ArrayList<>();
		requests = (ArrayList<Request>) readRequest("./src/vcloud/request.list");
		
		
		c = firstFit(c, requests);
		System.out.println(c.getServers().size());
	}

}
