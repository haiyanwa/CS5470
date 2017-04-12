package vcloud;

import java.util.List;

//physical machine in the datacenter
public class Server {
	
	//total CPU units
	private static final int CPU = 4000;
	
	//how many cpu unit unused
	private int cpuUnused = CPU;
	
	//server id, identical
	private int id;
	
	//status
	enum StatusServer{
		DOWN,
		STANDBY,
		RUNNING
	}
		
	private StatusServer serverStatus;
	
	//cloud it belongs to
	private Cloud cloud;
	
	//vhosts in this server
	private List<Vhost> vhosts;
	
	public Server(int id){
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public StatusServer getServerStatus() {
		return serverStatus;
	}

	public void setServerStatus(StatusServer serverStatus) {
		this.serverStatus = serverStatus;
	}

	public List<Vhost> getVhosts() {
		return vhosts;
	}

	public void setVhosts(List<Vhost> vhosts) {
		this.vhosts = vhosts;
	}

	public Cloud getCloud() {
		return cloud;
	}

	public void setCloud(Cloud cloud) {
		this.cloud = cloud;
	}

	public int getCpuUnused() {
		return cpuUnused;
	}

	public void setCpuUnused(int cpuUnused) {
		this.cpuUnused = cpuUnused;
	}
	
}
