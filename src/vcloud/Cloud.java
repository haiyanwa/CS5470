package vcloud;

import java.util.List;

public class Cloud {
	
	//cloud id
	private int id;
	
	//Servers
	private List<Server> servers;
	
	public Cloud(int id){
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public List<Server> getServers() {
		return servers;
	}

	public void setServers(List<Server> servers) {
		this.servers = servers;
	}
	
}
