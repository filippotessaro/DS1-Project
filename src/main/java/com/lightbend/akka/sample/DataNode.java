package com.lightbend.akka.sample;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class DataNode implements Iterable<DataNode> {
	
	String holder, name;
	boolean using, asked;
	Queue<String> request_q = new LinkedList<String>();
	List<Node> neighbors;
	
	public DataNode() {
		request_q.clear();
		using = false;
		asked = false;
		holder = null;
		neighbors.clear();
	}
	
	/*this is a comment
	static public class Privilege {// Need implementation}
	static public class Request {// Need implementation}
	
	
	public void assign_privilege() {
		if((holder = getSelf()) && (using == false) && !(request_q.isEmpty())) {
			holder = request_q.poll();
			asked = false;
			if(holder = self) {
				using = true;
				System.out.println("I'm in the critical section!");
			} else {
				//Send PRIVILAGE to HOLDER
			}
		}
	}
	
	public void make_request() {
		if((holder != self) && !(request_q.isEmpty() && (asked == false))) {
			//Send REQUEST to HOLDER
			asked = true;
		}
	}
*/	

	@Override
	public Iterator<DataNode> iterator() {
		// TODO Auto-generated method stub
		return null;
	}
}
