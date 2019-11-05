package com.lightbend.akka.sample;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import java.util.concurrent.ThreadLocalRandom;

import java.lang.*;

import static java.lang.Thread.sleep;

public class Node extends AbstractActor  {

	//#Building-tree message
	static public class Building_tree {
		public ActorRef neighbor;
		public int id;
		
		public Building_tree(int id, ActorRef neighbor) {
			this.neighbor = neighbor;
			this.id = id;
		}
	}
	//#Building-tree message
	
	//Initialization message
	public static class Initialize implements Serializable {
		private String holder;
		private int id;
		
		public Initialize(int id) {
			this.id = id;
			this.holder = null;
		}
	}
	//Initialization message

	public static class Privilege implements Serializable{
		private String token;

		Privilege(String tk){
			this.token = tk;
		}
	}
	
	//#Request message
	static public class Request implements Serializable {
		private int id;
		
		public Request(int id) {
			this.id = id;
		};
	}
	//#Request messages

	private String holder;
	private int my_id, id_holder;
	private boolean using, asked;
	private Queue<Integer> request_q = new LinkedList<Integer>();
	private Map<Integer, ActorRef> neighbors = new HashMap<Integer, ActorRef>();
	//private List<ActorRef> neighbors = new ArrayList<ActorRef>();
	//private Map<ActorRef, Integer> neighbors = new HashMap<ActorRef, Integer>();
	
	static public Props props(int id) {
		return Props.create(Node.class, () -> new Node(id));
	}
	
	public Node(int my_id) {
		this.my_id = my_id;
		this.using = false;
		this.asked = false;
		this.holder = null;
		this.id_holder = 0;
	}
	
	//#Handle initialization message
	private void init(Initialize a) {

		switch(a.id) {
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

		this.id_holder = a.id;

		for(int vicino: neighbors.keySet()) {
			if(vicino != id_holder) {
				neighbors.get(vicino).tell(new Initialize(my_id), getSelf());
			}
		}
		System.out.println("And the holder is....\n"+ holder + " " + my_id );

		boolean decided = true;
		int randomNum;
		while(decided) {
			randomNum = ThreadLocalRandom.current().nextInt(0, 4);
			if(randomNum == 2) {
				request_q.add(my_id);
				getSelf().tell(new Privilege(""), getSelf());
				getSelf().tell(new Request(my_id), getSelf());
				decided = false;
			}
		}
	}
	//#Handle initialization message

	//#Handle Request message
	private void make_request(Request new_r) {

		if(id_holder != my_id && !request_q.isEmpty() && asked == false) {
			neighbors.get(id_holder).tell(new Request(my_id), getSelf());
			asked = true;
		}
		if(id_holder == my_id) {
			getSelf().tell(new Privilege(""), getSelf());
		}
	}
	//#Handle Request message

	private void assign_privilege(Privilege pr){
		if(id_holder == my_id && !using && !request_q.isEmpty()){

			// rewrite id_holder
			id_holder = request_q.remove();
			asked = false;

			if(id_holder == my_id){
				using = true;
				//TODO enter CS
				enter_CS();

			} else {
				//TODO send privilege to holder
				// assign privilege may pass the privilege to another node
				// or initiate a local entry to the CS. If the privilege is passed
				// to another node, make req may request that the privilege be returned

				neighbors.get(id_holder).tell(new Privilege(""), getSelf());

				boolean decided = true;
				// random is useful in order to maintain a network stability
				int randomNum;
				while(decided) {
					// may request that the privilege be returned
					randomNum = ThreadLocalRandom.current().nextInt(0, 4);
					if(randomNum == 2) {
						getSelf().tell(new Request(my_id), getSelf());
						decided = false;
					}
				}
			}

		}
	}

	private void enter_CS(){
		try {
			int randomNum = ThreadLocalRandom.current().nextInt(0, 20);
			sleep(randomNum);
			exit_CS();

		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void exit_CS() {
		using = false;
		getSelf().tell(new Privilege(""), getSelf());
		//assign_privilege(new Privilege(""))
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(Initialize.class, this::init)
				.match(Request.class, this::make_request)
				.match(Building_tree.class, bt -> {
					this.neighbors.put(bt.id, bt.neighbor);
				})
				.match(Privilege.class, this::assign_privilege)
				.build();
	}

}
