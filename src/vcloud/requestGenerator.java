package vcloud;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Random;

//write out to request.list with format
//vtype : number
public class requestGenerator {

	public static void main(String[] args) {
		
		try{
			PrintWriter writer = new PrintWriter("./src/vcloud/request.list", "UTF-8");
			for(int i=0;i<100;i++){
				Random rand = new Random();
				int t = rand.nextInt(3);
				
				Random rand1 = new Random();
				int n = rand1.nextInt(10)+1;
				writer.println(t + "," + n);
			}
			writer.close();
			
		}catch (IOException e) {
			System.out.println("file not exists");
		}
		
		
		
		
	}

}
