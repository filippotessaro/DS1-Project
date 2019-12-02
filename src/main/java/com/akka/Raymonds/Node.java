package com.akka.Raymonds;

import java.util.*;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import com.akka.Raymonds.Messages.Message;

import java.util.concurrent.ThreadLocalRandom;
import java.lang.*;



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
	public List<Message.Advise> advises;

	private float recoveryStart;


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
	private void init(Message.Initialize a) {
		this.id_holder = a.getId();

		for(int neighbor: this.neighbors.keySet()) {
			if(neighbor != id_holder) {
				this.neighbors.get(neighbor).tell(new Message.Initialize(my_id), getSelf());
			}
		}

		//Node wishes to enter in CS
		getSelf().tell(new Message.Enter_CS(), getSelf());

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
			if(this.id_holder != this.my_id && !this.request_q.isEmpty() && !this.asked) {
				this.neighbors.get(this.id_holder).tell(new Message.Request(this.my_id), getSelf());
				this.asked = true;
			}
		}
	}
	//#Handle Request message

	private void assign_privilege(){
		if(!this.recovery){
			if(this.id_holder == this.my_id && !this.using && !this.request_q.isEmpty()){

				// rewrite id_holder
				this.id_holder = request_q.remove();
				this.asked = false;

				if(id_holder == my_id){
					this.using = true;
					this.Do_CS();
				} else {
					this.neighbors.get(this.id_holder).tell(new Message.Privilege(this.my_id), getSelf());
				}
			}
		}
	}


	/*----- EVENTS MANAGEMENT SECTION ------*/
	public void Do_CS(){
		System.out.println("Node " + this.my_id + " is doing something in CS");
		try {
			int randomNum = ThreadLocalRandom.current().nextInt(0, 100);
			Thread.sleep(randomNum * 10);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			getSelf().tell(new Message.Exit_CS(), ActorRef.noSender());
			getSelf().tell(new Message.Enter_CS(), getSelf());
		}
	}

	private void wish_EnterCS(Message.Enter_CS msg){
		this.request_q.add(this.my_id);
		assign_privilege();
		make_request();
	}

	private void exit_CS(Message.Exit_CS msg) {
		System.out.println("Node " + this.my_id + " is exiting from the CS! \t Queue: [" + this.request_q.toString() + "]");
		this.using = false;
		this.assign_privilege();
		this.make_request();
	}

	private void on_RequestRcv(Message.Request req){
		int reqId = req.getFromId();
		this.request_q.add(reqId);
		this.assign_privilege();
		this.make_request();
	}

	private void on_PrivilegeRcv(Message.Privilege prv){
		if(this.recovery){
			System.out.println("Node " + this.my_id + " is in recovery and receives privilege from "
					+ prv.getFromId() + " \t Queue is : [" + this.request_q.toString() + "]");
		}
		this.id_holder = this.my_id;
		this.assign_privilege();
		this.make_request();
	}

	/*
	 * RECOVERY EVENTS
	 * */

	private void on_RestartRcv(Message.Restart res){
		int id_RestartNode = res.getFromId();
		neighbors.get(id_RestartNode).tell(
				new Message.Advise(this.my_id, this.id_holder, this.asked, (LinkedList)this.request_q), getSelf());
	}

	private void on_AdviseRcv(Message.Advise adv){

		// Add advise message to the list
		this.advises.add(adv);
		System.out.println("Advises Received from " + adv.fromId);
		if(this.advises.size() == this.neighbors.keySet().size()){
			RestoreNode();
		}
	}

	private void RestoreNode() {

		// enter only on this condition
		assert (this.advises.size() == this.neighbors.keySet().size() && this.recovery);
		assert (this.request_q.size() == 0);

		System.out.println("Restoring Failed Node " + my_id);

		boolean x_isHolder = true;

		//Determine holder
		for(Message.Advise advise: advises) {
			if(advise.holder_y != this.my_id){
				//case dissenting node
				x_isHolder = false;
				this.id_holder = advise.fromId;
			}
		}

		if(x_isHolder){
			System.out.println("Node " + this.my_id + " is privileged");
			this.id_holder = this.my_id;
			this.request_q.clear();
			this.request_q.add(this.my_id);
		} else {
			System.out.println("Node " + this.my_id + " is not privileged");
		}

		for(Message.Advise advise: advises) {
			// Reconstruct the Request_Qx
			if(advise.holder_y == this.my_id && advise.asked_y){
				this.request_q.add(advise.fromId);
			}
		}

		//Determining AskedX
		if(this.id_holder == this.my_id){
			this.asked = false;
		} else {
			for(Message.Advise advise: this.advises) {
				if(advise.fromId == this.id_holder &&
						advise.y_reqQueue.contains(this.my_id)) {
					this.asked = true;
				}
			}
		}

		//Reassigning usingX
		this.using = false;
		this.advises.clear();

		// End up recovery mode
		System.out.println("Node " + this.my_id + " terminates the recovery phase.");
		this.recovery = false;

		if(!x_isHolder){
			getSelf().tell(new Message.Enter_CS(), getSelf());
		} else {
			assign_privilege();
		}
	}

	private void on_NodeFailure(Message.NodeFailure failure){
		//Reset all parameters

		System.out.println("Holder of node " + this.my_id + " is " + this.id_holder);
		this.asked = false;
		this.request_q.clear();
		this.id_holder = -1;
		this.recoveryStart = System.currentTimeMillis();
		this.recovery = true;

		System.out.println("Node " + my_id + " is failing... :(");

		//Start Recovery procedure...
		StartRecovery();
	}

	private void StartRecovery(){
		//Delay for a sufficient long time
		System.out.println("Start delay");
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("End node " + this.my_id + "'s delay. \nStart sending Restart Messages to neighbours");

		//Send Restart Message to each neighbours
		for(int neighbor: neighbors.keySet()) {
			neighbors.get(neighbor).tell(new Message.Restart(my_id), getSelf());
		}
		//Now await the advise messages on the event calling
	}



	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(Message.Initialize.class, this::init)
				.match(Message.Request.class, this::on_RequestRcv)
				.match(Message.Exit_CS.class, this::exit_CS)
				.match(Message.Enter_CS.class, this::wish_EnterCS)
				.match(Message.Restart.class, this::on_RestartRcv)
				.match(Message.Advise.class, this::on_AdviseRcv)
				.match(Message.NodeFailure.class, this::on_NodeFailure)
				.match(Message.Building_tree.class, bt -> {
					this.neighbors.put(bt.id, bt.neighbor);
				})
				.match(Message.Privilege.class, this::on_PrivilegeRcv)
				.build();
	}
}