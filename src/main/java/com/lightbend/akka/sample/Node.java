package com.lightbend.akka.sample;

import java.sql.Array;
import java.util.*;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import com.lightbend.akka.sample.Messages.Message.*;
import java.util.concurrent.ThreadLocalRandom;
import java.lang.*;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;


import static java.lang.Thread.activeCount;
import static java.lang.Thread.sleep;

public class Node extends AbstractActor  {
	private int my_id;

	public int getId_holder() {
		return id_holder;
	}

	private int id_holder;
	private boolean using, asked;
	private Queue<Integer> request_q = new LinkedList<Integer>();
	private Map<Integer, ActorRef> neighbors = new HashMap<Integer, ActorRef>();

	// params for recovery
	private boolean recovery;
	public List<Advise> advises;

	
	static public Props props(int id) {

		return Props.create(Node.class, () -> new Node(id));
	}
	
	public Node(int my_id) {
		this.my_id = my_id;
		this.using = false;
		this.asked = false;
		this.id_holder = 0;
		this.recovery = false;

		this.advises = new ArrayList();
	}
	
	//#Handle initialization message
	private void init(Initialize a) {
		this.id_holder = a.getId();

		for(int neighbor: neighbors.keySet()) {
			if(neighbor != id_holder) {
				neighbors.get(neighbor).tell(new Initialize(my_id), getSelf());
			}
		}

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
		neighbors.get(id_RestartNode).tell(
				new Advise(my_id, this.id_holder, this.asked, (LinkedList)request_q), getSelf());

	}

	private void on_AdviseRcv(Advise adv){

		if(adv != null){
			// Add advise message to the list
			advises.add(adv);
		} else {
			System.out.println("There is a problem on Advise reception");
		}

		if(advises.size() == neighbors.keySet().size()){
			System.out.println("Advises Received from " + adv.fromId);
			RestoreNode();
		}

	}

	private void RestoreNode() {
		//Node restoring part

		// enter only on this condition
		assert (advises.size() == neighbors.keySet().size() && this.recovery);

		System.out.println("Restoring Failed Node: " + my_id);

		boolean x_isHolder = false;


		//Determine holder & reconstruct queue in one cycle
		for(Advise advise: advises) {
			if(advise.holder_y == this.my_id){
				x_isHolder = true;
			} else{
				//case dissenting node
				x_isHolder = false;
				this.id_holder = advise.holder_y;
			}

			// Reconstruct the Request_Qx
			if(advise.holder_y == my_id && advise.asked_y){
				this.request_q.add(advise.fromId);
			}

		}
		if(x_isHolder){
			System.out.println("Node " + my_id + " is privileged");
			this.id_holder = my_id;
		}else{
			System.out.println("Node " + my_id + " is not privileged");
		}

		//Determining AskedX
		if(id_holder == my_id){
			this.asked = false;
		}else{
			for(Advise advise: advises) {
				if(advise.fromId == id_holder &&
						advise.y_reqQueue.contains(my_id)) {
					this.asked = true;
				}
			}
		}


		//Reassigning usingX
		this.using = false;

		advises.clear();

		// End up recovery mode
		System.out.println("Node " + this.my_id + " terminates the recovery mode. ");
		this.recovery = false;
	}

	private void on_NodeFailure(NodeFailure failure){
		//Reset all parameters
		this.asked = false;
		this.request_q.clear();
		this.id_holder = -1;

		System.out.println("Node " + my_id + " is failing... :(");

		//Start Recovery procedure...
		StartRecovery();
	}

	private void StartRecovery(){
		//Delay for a sufficient long time
		System.out.println("Sleep ...");
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("End Sleep ... \nStart sending Restart Messages to neighbours");

		//Send Restart Message to each neighbours
		for(int neighbor: neighbors.keySet()) {
			neighbors.get(neighbor).tell(new Restart(my_id), getSelf());
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
