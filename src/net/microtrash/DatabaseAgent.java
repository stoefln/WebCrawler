package net.microtrash;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

import java.util.EmptyStackException;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;

public class DatabaseAgent extends Agent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2L;

	private Hashtable<String,Webpage> parsedWebPages = new Hashtable<String,Webpage>();
	private Stack<String> unparsedLinks = new Stack<String>();
	private Vector<AID> crawlers = new Vector<AID>();
	private String seedUrl;
	
	protected void log(String message) {
		System.out.println(message);
	}
	@Override
	protected void setup() {
		System.out.println("Hallo I'm the DatabaseAgent! My name is "+getAID().getName());
		
		Object[] args = getArguments(); 
		if (args != null && args.length > 0) {
			seedUrl = (String) args[0];
			unparsedLinks.push(seedUrl);
			log("Starting with seedUrl "+seedUrl);
		}else{
			// Make the agent terminate immediately
			log("No seedUrl specified"); 
			doDelete();
			return;
		}
		
		// 1) look for agents which have registered as "crawlers" every 10 seconds 
		addBehaviour(new TickerBehaviour(this, 10000) { 
			
			private static final long serialVersionUID = 1L;

			protected void onTick() {
				log("Update the list of crawler agents.");
				DFAgentDescription template = new DFAgentDescription(); 
				ServiceDescription sd = new ServiceDescription(); 
				sd.setType("crawling"); 
				template.addServices(sd);
				crawlers = new Vector<AID>();
				try {
					log("All agents: ");
					DFAgentDescription[] result = DFService.search(myAgent, template); 
					for (int i = 0; i < result.length; ++i) {
						crawlers.add(result[i].getName());
						log("  "+result[i].getName());
					}
				}catch(FIPAException fe) {
					fe.printStackTrace();
				}
				
			}			
		});
		
		// 2) loop through all available crawlers and push a linkUrl to each of them (as long as there are links to other webpages which are not present in the DB by now) 
		addBehaviour(new ParseRequestPerformer());
		
		// 3) get responses, insert page into the db and add unparsed urls to the unparsedUrls Stack
		addBehaviour(new ParseResponseReceiver());
		
		super.setup();
	}
	
	@Override
	protected void takeDown() { 
		System.out.println("DatabaseAgent "+getAID().getName()+" sais good bye");
	}
	
	public void addWebpage(Webpage page) {
		parsedWebPages.put(page.getUrl(),page);
	}
	
	public boolean isWebpageParsed(String pageUrl){
		return parsedWebPages.contains(pageUrl);
	}
	
	private class ParseRequestPerformer extends CyclicBehaviour{
		
		private static final long serialVersionUID = 1L;

		
		@Override
		public void action() {
			log("ParseRequestPerformer action()");
			try{
				for(AID aid : crawlers ){
					String link = unparsedLinks.pop();
					log("send link for parsing: "+link);
					ACLMessage message = new ACLMessage(ACLMessage.CFP);
					message.addReceiver(aid);
					message.setContent(link);
					message.setConversationId("parse-url");
					message.setReplyWith("message_"+aid+"_"+System.currentTimeMillis());
					myAgent.send(message);
				}
			}catch(EmptyStackException e){} 
			log("ParseRequestPerformer block()");
			
			block();
		}

		
	}

	private class ParseResponseReceiver extends CyclicBehaviour{
		
		private static final long serialVersionUID = 2L;

		@Override
		public void action() {
			log("ParseResponseReceiver action()");
			ACLMessage reply = myAgent.receive(); 
			if (reply != null) {
				String urls = reply.getContent();
				String[] urlArr = urls.split(",");
				for(String url : urlArr){
					System.out.println(url);
				}
			} else {
				log("ParseResponseReceiver block()");
				block();
			}

		}

		
	}
}

