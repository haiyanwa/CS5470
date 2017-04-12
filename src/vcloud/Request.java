package vcloud;

import java.util.Map;
import vcloud.Vhost;

public class Request {
	private int id;
	private Vtype type;
	private int amount;
	
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
}
