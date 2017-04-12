package vcloud;

import java.util.HashMap;
import java.util.Map;

//virtual host
public class Vhost {
	
	//belongs to which server
	private Server server;
	private Vtype type;
	//for how many time slots the server will run
	private int term;
	
	//status
	enum Status{
		DOWN,
		RUNNING
	}
	
	private Status vhostStatus;
	
	public Vhost(){
	}
	
	public Vhost(Vtype type){
		this.type = type;
	}

	public Server getServer() {
		return server;
	}

	public void setServer(Server server) {
		this.server = server;
	}

	public Status getVhostStatus() {
		return vhostStatus;
	}

	public void setVhostStatus(Status vhostStatus) {
		this.vhostStatus = vhostStatus;
	}

	public Vtype getType() {
		return type;
	}

	public void setType(Vtype type) {
		this.type = type;
	}

	public int getTerm() {
		return term;
	}

	public void setTerm(int term) {
		this.term = term;
	}
	
}
