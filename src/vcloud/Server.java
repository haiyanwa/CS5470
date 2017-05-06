package vcloud;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

//physical machine in the datacenter
public class Server {
	
	//total CPU units
	private static final int CPU = 4000;
	
	//how many cpu unit unused
	private int cpuUnused = CPU;
	
	//server id, identical
	private int id;
	
	//future cpu power, map<groupid, cpu_openup>
	private static Map<Integer, Integer> futureCPU = new HashMap<>();
	
	private int reservedCPU = 0 ;
	
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
		//futureCPU.put(-1, 0);
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

	public static Map<Integer, Integer> getFutureCPU() {
		return futureCPU;
	}

	public static void setFutureCPU(Map<Integer, Integer> futureCPU) {
		Server.futureCPU = futureCPU;
	}

	public int getReservedCPU() {
		return reservedCPU;
	}

	public void setReservedCPU(int reservedCPU) {
		this.reservedCPU = reservedCPU;
	}
	
}
