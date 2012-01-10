package net.microtrash;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class ArchiveWebpageServer extends CyclicBehaviour { 
	
	public void action() {
		ACLMessage msg = myAgent.receive(); 
		if (msg != null) {
			String title = msg.getContent(); 
			ACLMessage reply = msg.createReply();
			Integer price = (Integer) catalogue.get(title); 
			if (price != null) {
				// The requested book is available for sale. Reply with the price
				reply.setPerformative(ACLMessage.PROPOSE); 
				reply.setContent(String.valueOf(price.intValue()));
			} else {
				// The requested book is NOT available for sale.
				reply.setPerformative(ACLMessage.REFUSE); reply.setContent("not-available");
			} 
			myAgent.send(reply);
		} 
	}
}