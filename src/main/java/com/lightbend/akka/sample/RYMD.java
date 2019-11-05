package com.lightbend.akka.sample;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import com.lightbend.akka.sample.Message.Message.Building_tree;
import com.lightbend.akka.sample.Message.Message.Initialize;

import java.util.List;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

public class RYMD {
	 public static void main(String[] args) {
		    final ActorSystem system = ActorSystem.create("RaymondAlgorithm");
		    try {
		      //#create-actors
		      final ActorRef Node_A = 
		        system.actorOf(Node.props(1),"A");
		      final ActorRef Node_B = 
				        system.actorOf(Node.props(2),"B");
		      final ActorRef Node_C = 
				        system.actorOf(Node.props(3),"C");
		      final ActorRef Node_D = 
				        system.actorOf(Node.props(4),"D");
		      final ActorRef Node_E = 
				        system.actorOf(Node.props(5),"E");
		      //#create-actors
	        
		      //#create tree-structure 
		      Node_A.tell(new Building_tree(2,Node_B), ActorRef.noSender());
		      //Node_A.tell(new Building_tree(3,Node_C), ActorRef.noSender());
		    
		      //Node_C.tell(new Building_tree(1,Node_A), ActorRef.noSender());
		      //Node_C.tell(new Building_tree(5,Node_E), ActorRef.noSender());
		      //Node_C.tell(new Building_tree(4,Node_D), ActorRef.noSender());
		    
		      Node_B.tell(new Building_tree(1,Node_A), ActorRef.noSender());
		    
		      //Node_E.tell(new Building_tree(3,Node_C), ActorRef.noSender());
		      
		      //Node_D.tell(new Building_tree(3,Node_C), ActorRef.noSender());
		      //#create tree-structure
		    
		      //#inject token in arbitrary node and flood it
		      Node_A.tell(new Initialize(0), ActorRef.noSender());
		      //#inject token in arbitrary node and flood it
			 
		      System.out.println(">>> Press ENTER to exit <<<");
		      System.in.read();
		    } catch (IOException ioe) {
		    } finally {
		      system.terminate();
	    }
	}
}
