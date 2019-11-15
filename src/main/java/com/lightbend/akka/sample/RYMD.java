package com.lightbend.akka.sample;

import akka.actor.Actor;
import com.lightbend.akka.sample.Graph_Generator.Graph;
import com.lightbend.akka.sample.Graph_Generator.StdOut;
import com.lightbend.akka.sample.Messages.Message;
import com.lightbend.akka.sample.Messages.Message.Building_tree;
import com.lightbend.akka.sample.Messages.Message.Initialize;
import com.lightbend.akka.sample.Messages.Message.NodeFailure;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;


import static com.lightbend.akka.sample.Graph_Generator.GraphGen.tree;


public class RYMD {

	final static ActorSystem system = ActorSystem.create("RaymondAlgorithm");

	public static ActorRef OnceUponATime(int vertex) {
		final ActorRef nodeRef =
				system.actorOf(Node.props(vertex));
		return nodeRef;
	}

	 public static void main(String[] args) {
		    try {
				int V = Integer.parseInt(args[0]);
				int E = Integer.parseInt(args[1]);
				int V1 = V/2;
				int V2 = V - V1;
				ActorRef[] arr = new ActorRef[V];

				//StdOut.println(tree(V));
				//StdOut.println();


				StdOut.println("The random tree is:");
				//Create the random tree
				Graph G = tree(V);
				StdOut.println(G);

				//Create actors
				for (int i = 0; i < V; i++) {
					arr[i] = OnceUponATime(i);
				}
				for (int j = 0; j < arr.length; j++) {
					Iterable<Integer> neighbors = G.adj(j);
					for (int vertex: neighbors) {
						arr[j].tell(new Building_tree(vertex,arr[vertex]), ActorRef.noSender());
					}
				}
				//Create actors

				//#inject token in a random node and flood it
				Random rand = new Random();
				int value = rand.nextInt(V);
				System.out.println("The starting holder is: " + value);
				arr[value].tell(new Initialize(value), ActorRef.noSender());
				//#inject token in arbitrary node and flood it

				/*
				Scanner s = new Scanner(System.in);
				char command = s.next().charAt(0);
			
				if (command == 'f') {
					Random rand_om = new Random();
					int valu_e = rand_om.nextInt(V);
					arr[valu_e].tell(new Message.NodeFailure(), ActorRef.noSender());
				}

				System.out.println(">>> Press f to fail a node <<<");
*/
                System.out.println(">>> Press ENTER to exit <<<");
                System.in.read();
		    } catch (IOException ioe) {
		    } finally {
				system.terminate();
	    }
	}
}
