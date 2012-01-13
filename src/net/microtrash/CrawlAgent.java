package net.microtrash;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


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
		
		void reportFailure(ACLMessage request, String text) {
			log("reporting back: " + text);
			ACLMessage reply = request.createReply();
			reply.setPerformative(ACLMessage.REFUSE);
			reply.setContent(text);
			myAgent.send(reply);
		}

		private boolean isHtmlAddress(String url){
			
			if(!url.startsWith("http") && !url.startsWith("https")){
				return false;
			}
			String[] noHtmlExtension = {".png", ".jpg", ".jpeg", ".gif"};
			
			for(String ext : noHtmlExtension){
				if(url.endsWith(ext)){
					return false;
				}
			}
			return true;
		}
		
		@Override
		public void action() {
			log("ParseRequestReceiver action()");
			ACLMessage request = myAgent.receive(); 
			if (request != null) {
				log("got message!");
				String urlString = request.getContent();

				try {
					Document doc = Jsoup.connect(urlString).get();
					Elements newsHeadlines = doc.select("a");
					Webpage page = new Webpage(urlString);
					
					for(Element link : newsHeadlines){
						String url = link.attr("abs:href");
						if(!link.attr("href").startsWith("#") && isHtmlAddress(url)){
							page.addOutgoingLink(url);
						}
					}					
					log("found " + page.getOutgoingLinks().size() + " outgoing links.");

					ACLMessage reply = request.createReply();
					 
					if (page.getOutgoingLinks().size() > 0) {
						String serialisedPage = Utility.toString(page);
						reply.setPerformative(ACLMessage.PROPOSE);
						reply.setContent(serialisedPage);
					} else {
						reportFailure(request, "No links found");
					} 
					myAgent.send(reply);
				
				} catch (IOException e) {
					reportFailure(request, "Connection Error");
				} catch(IllegalArgumentException e){
					reportFailure(request, "Malformed URL: "+urlString);
				}
				
			} else {
				log("ParseResponseReceiver block()");
				block();
			}
			
		}
		
	}
}
