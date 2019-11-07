package com.lightbend.akka.sample;

import java.sql.Array;
import java.util.*;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import com.lightbend.akka.sample.Messages.Message.*;
import java.util.concurrent.ThreadLocalRandom;
import java.lang.*;

import static java.lang.Thread.activeCount;
import static java.lang.Thread.sleep;

public class Node extends AbstractActor  {
	private String holder;
	private int my_id, id_holder;
	private boolean using, asked;
	private Queue<Integer> request_q = new LinkedList<Integer>();
	private Map<Integer, ActorRef> neighbors = new HashMap<Integer, ActorRef>();

	// params for recovery
	private boolean recovery;
	public List<Advise> advises;

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

		this.advises = null;
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
			if(id_holder != my_id && !request_q.isEmpty() && !asked) {
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
					Do_CS();
				} else {
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
		} finally {
			getSelf().tell(new Exit_CS(), ActorRef.noSender());

			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				System.out.println("Exception: " + e.toString() );
			}
			getSelf().tell(new Enter_CS(), getSelf());
		}
	}

	private void wish_EnterCS(Enter_CS msg){
		request_q.add(my_id);
		assign_privilege();
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		make_request();

		/*request_q.add(my_id);
		getSelf().tell(new Privilege(), getSelf());
		getSelf().tell(new Request(my_id), getSelf());*/

	}

	private void exit_CS(Exit_CS msg) {
		System.out.println("Node: " + my_id + " is exiting from the CS!");
		using = false;
		assign_privilege();
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		make_request();
	}

	private void on_RequestRcv(Request req){
		int reqId = req.getFromId();
		request_q.add(reqId);
		assign_privilege();
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		make_request();
	}

	private void on_PrivilegeRcv(Privilege prv){
		id_holder = my_id;
		//holder = "self";
		assign_privilege();
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		make_request();
	}

	/*
	* RECOVERY EVENTS
	* */

	private void on_RestartRcv(Restart res){
		int id_RestartNode = res.getFromId();

		int hold = 0;
		boolean inQueueX = false, asked = false;

		if(this.id_holder == id_RestartNode && !this.asked){
			//x may be privileged node, Y is not an element of requestq
			hold = id_RestartNode;
			inQueueX = false;
		} else if(this.id_holder == id_RestartNode && this.asked){
			//x may be privileged, y is an element of requestq
			hold = id_RestartNode;
			inQueueX = true;
		} else if(this.id_holder != id_RestartNode && !this.request_q.contains(id_RestartNode)){
			//x not be privileged, asked must be false
			hold = this.my_id;
			asked = false;
		} else if(this.id_holder != id_RestartNode && this.request_q.contains(id_RestartNode)){
			//x not be the privileged node, it has req the privilege and asked=true
			hold = this.my_id;
			asked = true;
		}

		neighbors.get(id_RestartNode).tell(new Advise(my_id, hold, asked, inQueueX), getSelf());

	}

	private void on_AdviseRcv(Advise adv){

		//TODO Store advises in data structure
		if(adv != null){
			// Add advise message to the list
			advises.add(adv);
		} else {
			System.out.println("There is a problem on Advise reception");
		}

		if(advises.size() == neighbors.size()){
			System.out.println("Advises Rece");
			RestoreNode();
		}

	}

	private void RestoreNode() {
		//TODO End the recovery mode
		//Node restoring part

	}

	private void on_NodeFailure(NodeFailure failure){
		//TODO set all node parameters to default ex: requestq. neighbours ...
		//Reset all parameters
		this.asked = false;
		this.request_q = null;
		this.holder = null;

		//Start Recovery procedure...
		StartRecovery();
	}

	private void StartRecovery(){
		//Delay for a sufficient long time
		try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		//Send Restart Message to each neighbours
		for(int neighbor: neighbors.keySet()) {
			if(neighbor != id_holder) {
				neighbors.get(neighbor).tell(new Restart(my_id), getSelf());
			}
		}
		//Now await the advise messages on the event calling
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
				.match(NodeFailure.class, this::on_NodeFailure)
				.match(Building_tree.class, bt -> {
					this.neighbors.put(bt.id, bt.neighbor);
				})
				.match(Privilege.class, this::on_PrivilegeRcv)
				.build();
	}


}
