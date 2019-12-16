package com.akka.Raymonds;

import com.akka.Raymonds.Graph_Generator.Graph;
import com.akka.Raymonds.Graph_Generator.GraphGen;
import com.akka.Raymonds.Graph_Generator.StdOut;
import com.akka.Raymonds.Messages.Message;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import java.io.IOException;
import java.util.Random;
import java.util.Scanner;


public class RYMD {

	final static ActorSystem system = ActorSystem.create("RaymondAlgorithm");

	public static ActorRef OnceUponATime(int vertex) {
		final ActorRef nodeRef =
				system.actorOf(Node.props(vertex, system));
		return nodeRef;
	}

	 public static void main(String[] args) {
		    try {
				int V = Integer.parseInt(args[0]);
				ActorRef[] arr = new ActorRef[V];

				StdOut.println("The random tree is:");
				//Create the random tree
				Graph G = GraphGen.tree(V);
				StdOut.println(G);

				//#Create actors
				for (int i = 0; i < V; i++) {
					arr[i] = OnceUponATime(i);
				}
				for (int j = 0; j < arr.length; j++) {
					Iterable<Integer> neighbors = G.adj(j);
					for (int vertex: neighbors) {
						arr[j].tell(new Message.Building_tree(vertex,arr[vertex]), ActorRef.noSender());
					}
				}
				//#Create actors

				System.out.println(">>> Press f to fail a node <<<");
				System.out.println(">>> Press t to terminate <<<");


				//#inject token in a random node and start flooding
				Random rand = new Random();
				int value = rand.nextInt(V -1 );
				System.out.println("The starting holder is: " + value);
				arr[value].tell(new Message.Initialize(value), ActorRef.noSender());
				//#inject token in arbitrary node and start flooding

				Scanner s = new Scanner(System.in);
				char command = s.next().charAt(0);
				while(true) {
					if (command == 'f') {
						Random random = new Random();
						int val = random.nextInt(V);
						arr[val].tell(new Message.NodeFailure(), ActorRef.noSender());
					} else if (command == 't') {
						system.terminate();
						return;
					}
					System.in.read();
				}

		    } catch (IOException ioe) {
		    } finally {
				system.terminate();
	    }
	}
}
