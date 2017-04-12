package vcloud;

import java.util.HashMap;
import java.util.Map;

public class VhostType {
	
	private static Map<Vtype, Integer> vcpu;
	
	public VhostType(){
		vcpu.put(Vtype.SMALL, 2000);
		vcpu.put(Vtype.MEDIUM, 4000);
		vcpu.put(Vtype.LARGE, 8000);
	}

	public static Map<Vtype, Integer> getVcpu() {
		return vcpu;
	}

	public static void setVcpu(Map<Vtype, Integer> vcpu) {
		VhostType.vcpu = vcpu;
	}
	
}
