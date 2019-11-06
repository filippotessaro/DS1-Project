package com.lightbend.akka.sample.Messages;

import akka.actor.ActorRef;

import java.io.Serializable;

public class Message {

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

        public String getHolder() {
            return holder;
        }

        public int getId() {
            return id;
        }

        private int id;

        public Initialize(int id) {
            this.id = id;
            this.holder = null;
        }
    }
    //Initialization message

    public static class Privilege implements Serializable{ }

    //#Request message
    static public class Request implements Serializable {
        int fromId;

        public Request(int i){
            this.fromId = i;
        }

        public int getFromId() {
            return fromId;
        }
    }
    //#Request messages

    /*Useful messages in order to maintain the Message-Event politics of Akka*/

    //Message to enter the CS
    static public class Enter_CS implements Serializable{ }

    //Message to exit from CS (at the end of it)
    static public class Exit_CS implements Serializable{ }


    /*
    * FAILURE MESSAGES
    * */

    //Restart Message in the moment that there is a node failure
    static public class Restart implements Serializable{ }

    //Advise message in reply to the Restart
    static public class Advise implements Serializable{ }
}
