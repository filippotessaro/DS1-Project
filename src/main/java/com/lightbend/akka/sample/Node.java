package com.lightbend.akka.sample;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import com.lightbend.akka.sample.Messages.Message.*;
import java.util.concurrent.ThreadLocalRandom;
import java.lang.*;

import static java.lang.Thread.sleep;

public class Node extends AbstractActor  {
	private String holder;
	private int my_id, id_holder;
	private boolean using, asked;
	private Queue<Integer> request_q = new LinkedList<Integer>();
	private Map<Integer, ActorRef> neighbors = new HashMap<Integer, ActorRef>();
	//private List<ActorRef> neighbors = new ArrayList<ActorRef>();
	//private Map<ActorRef, Integer> neighbors = new HashMap<ActorRef, Integer>();
	private boolean recovery;

	public class LockClass{

	};
	
	static public Props props(int id) {
		return Props.create(Node.class, () -> new Node(id));
	}
	
	public Node(int my_id) {
		this.my_id = my_id;
		this.using = false;
		this.asked = false;
		this.holder = null;
		this.id_holder = 0;
		this.recovery = false;
	}
	
	//#Handle initialization message
	private void init(Initialize a) {

		switch(a.getId()) {
		  case 0:
			  holder = "self";
			  break;
		  case 1:
			  holder = "A";
			  break;
		  case 2:
			  holder = "B";
			  break;
		  case 3:
			  holder = "C";
			  break;
		  case 4:
			  holder = "D";
			  break;
		  case 5:
			  holder = "E";
			  break;
		  default:
			// code block :)
		}

		this.id_holder = a.getId();

		for(int neighbor: neighbors.keySet()) {
			if(neighbor != id_holder) {
				neighbors.get(neighbor).tell(new Initialize(my_id), getSelf());
			}
		}
		System.out.println("And the holder is....\n"+ holder + " " + my_id );

		//TODO check initialization
		//getSelf().tell(new Privilege(), getSelf());
		//getSelf().tell(new Request(my_id), getSelf());

		//Node wishes to enter in CS
		getSelf().tell(new Enter_CS(), getSelf());

	}
	//#Handle initialization message

	/*
	* USEFUL METHOD PART:
	* ASSIGN PRIVILEGE
	* MAKE REQUEST
	*/

	//#Handle Request message
	private void make_request() {
		if(!this.recovery){
			if(id_holder != my_id && !request_q.isEmpty() && asked == false) {
				neighbors.get(id_holder).tell(new Request(my_id), getSelf());
				asked = true;
			}
		}
	}
	//#Handle Request message

	private void assign_privilege(){
		if(!this.recovery){
			if(id_holder == my_id && !using && !request_q.isEmpty()){

				// rewrite id_holder
				id_holder = request_q.remove();
				asked = false;

				if(id_holder == my_id){
					using = true;
					//TODO enter in CS
					Do_CS();

				} else {
					//TODO send privilege MESSAGE to holder
					neighbors.get(id_holder).tell(new Privilege(), getSelf());
				}

			}
		}
	}




	/*----- EVENTS MANAGEMENT SECTION ------*/
	public void Do_CS(){
		System.out.println("Node: " + my_id + " is doing something in CS");

		try {
			int randomNum = ThreadLocalRandom.current().nextInt(0, 100);
			Thread.sleep(randomNum);

		} catch (InterruptedException e) {
			e.printStackTrace();
		}finally {
			// TODO implement callback to exitCS
			getSelf().tell(new Exit_CS(), ActorRef.noSender());
		}

	}

	private void wish_EnterCS(Enter_CS msg){
		request_q.add(my_id);
		assign_privilege();
		make_request();

		/*request_q.add(my_id);
		getSelf().tell(new Privilege(), getSelf());
		getSelf().tell(new Request(my_id), getSelf());*/

	}

	private void exit_CS(Exit_CS msg) {
		System.out.println("Node: " + my_id + " is exiting from the CS!");
		using = false;
		//getSelf().tell(new Privilege(), getSelf());
		//assign_privilege(new Privilege(""))
		assign_privilege();
		make_request();
	}

	private void on_RequestRcv(Request req){
		int reqId = req.getFromId();
		request_q.add(reqId);
		assign_privilege();
		make_request();
	}

	private void on_PrivilegeRcv(Privilege prv){
		id_holder = my_id;
		//holder = "self";
		assign_privilege();
		make_request();
	}

	/*
	* RECOVERY EVENTS
	* */

	private void on_RestartRcv(Restart res){

	}

	private void on_AdviseRcv(Advise adv){

	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(Initialize.class, this::init)
				.match(Request.class, this::on_RequestRcv)
				.match(Exit_CS.class, this::exit_CS)
				.match(Enter_CS.class, this::wish_EnterCS)
				.match(Restart.class, this::on_RestartRcv)
				.match(Advise.class, this::on_AdviseRcv)
				.match(Building_tree.class, bt -> {
					this.neighbors.put(bt.id, bt.neighbor);
				})
				.match(Privilege.class, this::on_PrivilegeRcv)
				.build();
	}


}
