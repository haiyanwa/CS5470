package vcloud;

import java.util.Map;
import vcloud.Vhost;

public class Request {
	private int id;
	private Vtype type;
	private int amount;
	private int term; // length of the vhost life span
	private int groupId = -1; //for future request id
	private int reservedServerId = -1; //for future reserved space
	private boolean foundLoc = false;
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public Vtype getType() {
		return type;
	}
	public void setType(Vtype type) {
		this.type = type;
	}
	public int getAmount() {
		return amount;
	}
	public void setAmount(int amount) {
		this.amount = amount;
	}
	public int getTerm() {
		return term;
	}
	public void setTerm(int term) {
		this.term = term;
	}
	public int getGroupId() {
		return groupId;
	}
	public void setGroupId(int groupId) {
		this.groupId = groupId;
	}
	public int getReservedServerId() {
		return reservedServerId;
	}
	public void setReservedServerId(int reservedServerId) {
		this.reservedServerId = reservedServerId;
	}
	public boolean hasFoundLoc() {
		return foundLoc;
	}
	public void setFoundLoc(boolean foundLoc) {
		this.foundLoc = foundLoc;
	}
	
}
