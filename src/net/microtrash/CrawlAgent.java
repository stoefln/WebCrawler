package net.microtrash;

import java.util.EmptyStackException;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;


public class CrawlAgent extends Agent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	protected void log(String message) {
		System.out.println(message);
	}
	
	@Override
	protected void setup() {
		DFAgentDescription agentDescription = new DFAgentDescription(); 
		agentDescription.setName(getAID()); 
		ServiceDescription serviceDescription = new ServiceDescription(); 
		serviceDescription.setType("crawling"); 
		serviceDescription.setName("webpage crawler"); 
		agentDescription.addServices(serviceDescription);
		try { 
			DFService.register(this, agentDescription);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
		addBehaviour(new ParseRequestReceiver());
		
		super.setup();
	}

	protected void takeDown() { // Deregister from the yellow pages try {
		try {
			DFService.deregister(this); 
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}

		System.out.println("CrawlAgent "+getAID().getName()+" sais good bye");
	}

	private class ParseRequestReceiver extends CyclicBehaviour{
		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			log("ParseRequestReceiver action()");
			ACLMessage request = myAgent.receive(); 
			if (request != null) {
				log("got message!");
				String url = request.getContent();
			
				// TODO: send HTTP request, parse response
				String urls = "http://www.github.com,http://www.doodle.com";
				log("returning message...");
				
				// TODO: should we move following code to an extra Behavior?
				ACLMessage reply = request.createReply();
				 
				if (urls != null) { // if there is a valid response data object
					reply.setPerformative(ACLMessage.PROPOSE);
					reply.setContent(urls);
				} else {
					reply.setPerformative(ACLMessage.REFUSE); 
					reply.setContent("404 - not found!");
				} 
				myAgent.send(reply);
				
			} else {
				log("ParseResponseReceiver block()");
				block();
			}
			
		}
		
	}
}
