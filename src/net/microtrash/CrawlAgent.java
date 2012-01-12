package net.microtrash;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;


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

		@Override
		public void action() {
			log("ParseRequestReceiver action()");
			ACLMessage request = myAgent.receive(); 
			if (request != null) {
				log("got message!");
				String url = request.getContent();
			
				// TODO: move parts to own behaviour?
				// TODO: robots.txt?
				URL u;
				URLConnection urlConnection = null;
				try {
					u = new URL(url);
					urlConnection = u.openConnection();
					urlConnection.setAllowUserInteraction(false);
					InputStream stream = urlConnection.getInputStream();
					BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
					String type = urlConnection.getContentType();
					log("Content Type: " + type);
					if(!type.contains("text/html")) {
						throw new UnsupportedEncodingException("Not a html page");
					}
					String line;
					StringBuilder content = new StringBuilder();
					while((line = reader.readLine()) != null) {
						content.append(line);
					}
					reader.close();
					
					String aTag = "<a";
					String hrefParam = "href=";
					String lContent = content.toString().toLowerCase();
					content = null;
					char endChar;
					int i = 0;
					StringBuilder links = new StringBuilder();
					while((i = lContent.indexOf(aTag, i)) >= 0) {
						endChar = ' ';
						int start = lContent.indexOf(hrefParam, i);
						if (start < 0) {
							continue;
						}
						start += hrefParam.length();
						if(lContent.charAt(start) == '"') {
							start++;
							endChar = '"';
						}
						i = lContent.indexOf(endChar, start);
						String link = lContent.substring(start, i);
						try {
							if (link.startsWith("#")) {
								continue;
							}
							URL linkURL = new URL(u, link);
							link = linkURL.toString();
							log("found link: " + link);
							links.append(",");
							links.append(link);
						} catch (MalformedURLException e) {
							System.err.println("malformed URL: " + link);
							continue;
						}
					}
					links.deleteCharAt(0);

					// TODO: "," is not a good seperator, can be part of URLs 
					// (ref: http://stackoverflow.com/questions/1547899/which-characters-make-a-url-invalid)
					String urls = links.toString();
					links = null;
					log("returning message...");
					
					ACLMessage reply = request.createReply();
					 
					if (urls.length() > 0) {
						reply.setPerformative(ACLMessage.PROPOSE);
						reply.setContent(urls);
					} else {
						reportFailure(request, "No links found");
					} 
					myAgent.send(reply);
				} catch (MalformedURLException e) {
					reportFailure(request, "MalformedURLException");
				} catch (UnsupportedEncodingException e) {
					reportFailure(request, e.getMessage());
				} catch (IOException e) {
					reportFailure(request, "Connection Error");
				}
				
			} else {
				log("ParseResponseReceiver block()");
				block();
			}
			
		}
		
	}
}
