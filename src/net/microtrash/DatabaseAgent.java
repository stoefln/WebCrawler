package net.microtrash;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

public class DatabaseAgent extends Agent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2L;

	private Hashtable<String,Webpage> parsedWebPages = new Hashtable<String,Webpage>();
	private Deque<String> unparsedLinks = new ArrayDeque<String>();
	private Set<String> workingOn = new HashSet<String>();
	private List<AID> crawlers = new ArrayList<AID>();
	private List<AID> freeCrawlers = new ArrayList<AID>();
	private String seedUrl = "";
	private String urlFilter = "";
	private ParseRequestPerformer parseRequestPerformer;
	private ParseResponseReceiver parseResponseReceiver;
	
	protected void log(String message) {
		System.out.println(message);
	}
	@Override
	protected void setup() {
		System.out.println("Hallo I'm the DatabaseAgent! My name is "+getAID().getName());

		Object[] args = getArguments(); 
		if (args != null && args.length != 0) {
			if(args.length > 0){
				seedUrl = (String) args[0];
				unparsedLinks.push(seedUrl);
			}
			if(args.length > 1){
				urlFilter = (String) args[1];
			}
			if(args.length > 2){
				int spawnCrawlAgents = Integer.parseInt((String) args[2]);
				ContainerController cc = getContainerController();
				for(int i=0;i < spawnCrawlAgents;i++){
					AgentController ac = null;
					try {
						String agentName = "Crawl" + (i + 1);
						ac = cc.createNewAgent(agentName, "net.microtrash.CrawlAgent", null);
						ac.start();
						log("Spawned Agent \""+agentName+"\"");
					} catch (StaleProxyException e) {
						e.printStackTrace();
					}
				}
			}
			log("Starting with seedUrl "+seedUrl+ " and filter: "+urlFilter);
		}else{
			// Make the agent terminate immediately
			log("No seedUrl specified. Please provide 1-3 params: seeUrl, urlFilter and crawlAgentAmount");
			doDelete();
			return;
		}
		
		// 1) look for agents which have registered as "crawlers" every 10 seconds 
		addBehaviour(new TickerBehaviour(this, 10000) { 
			
			private static final long serialVersionUID = 1L;

			protected void onTick() {
				//log("Update the list of crawler agents.");
				DFAgentDescription template = new DFAgentDescription(); 
				ServiceDescription sd = new ServiceDescription(); 
				sd.setType("crawling"); 
				template.addServices(sd);
				try {
					//log("All agents: ");
					DFAgentDescription[] result = DFService.search(myAgent, template); 
					for (int i = 0; i < result.length; ++i) {
						AID name = result[i].getName();
						if(!crawlers.contains(name)) {
							freeCrawlers.add(name);
							crawlers.add(name);
							log("new Crawler registered: " + result[i].getName());
						}
						//log("  "+result[i].getName());
					}
				}catch(FIPAException fe) {
					fe.printStackTrace();
				}
				
			}			
		});
		
		// 2) loop through all available crawlers and push a linkUrl to each of them (as long as there are links to other webpages which are not present in the DB by now) 
		parseRequestPerformer = new ParseRequestPerformer(this, 4000);
		addBehaviour(parseRequestPerformer);
		
		// 3) get responses, insert page into the db and add unparsed urls to the unparsedUrls Stack
		parseResponseReceiver = new ParseResponseReceiver();
		addBehaviour(parseResponseReceiver);
		
		super.setup();
	}
	
	@Override
	protected void takeDown() { 
		log("DatabaseAgent "+getAID().getName()+" sais good bye");
	}
	
	public void addWebpage(Webpage page) {
		parsedWebPages.put(page.getUrl().toLowerCase(),page);
		log("added webpage " + page.getUrl() + " to DB... total: "+parsedWebPages.size());
	}
	
	public boolean isWebpageParsed(String pageUrl){
		return parsedWebPages.containsKey(pageUrl.toLowerCase());
	}
	
	private class ParseRequestPerformer extends TickerBehaviour{
		
		public ParseRequestPerformer(Agent a, long period) {
			super(a, period);
		}


		private static final long serialVersionUID = 1L;

		
		@Override
		public void onTick() {
			//log("ParseRequestPerformer action()");
			try{
				log("Available Crawlers: " + freeCrawlers.size() + ", unparsed Links: " + unparsedLinks.size());
				while(freeCrawlers.size() > 0) {
					String link = unparsedLinks.removeFirst();
					if(!workingOn.contains(link)) {
						workingOn.add(link);
						AID aid = freeCrawlers.remove(0);
						log("send link for parsing to agent"+aid.getLocalName()+": "+link);
						ACLMessage message = new ACLMessage(ACLMessage.CFP);
						message.addReceiver(aid);
						message.setContent(link);
						message.setConversationId("parse-url");
						message.setReplyWith("message_"+aid+"_"+System.currentTimeMillis());
						myAgent.send(message);
					} else {
						log("Removing duplicate entry (" + link);
					}
				}
			}catch(NoSuchElementException e){
				log("Queue is empty.");
			} 
			
			if(unparsedLinks.size() == 0 && freeCrawlers.size() == crawlers.size()){
				log("Parsing process finished. All pages: ");
				log(parsedWebPages.toString());
				stop();
			}
			//log("ParseRequestPerformer block()");
			
			
		}

		
	}

	private class ParseResponseReceiver extends CyclicBehaviour{
		
		private static final long serialVersionUID = 2L;

		@Override
		public void action() {
			//log("ParseResponseReceiver action()");
			ACLMessage reply = myAgent.receive();
			
			if (reply != null) {
				AID aid = reply.getSender();
				if(crawlers.contains(aid)) {
					freeCrawlers.add(aid);	// crawler has done its job and is now queued again for the next one
				}
				if(reply.getPerformative() == ACLMessage.PROPOSE) {
					String serialisedPage = reply.getContent();
					Webpage page = null;
					try {
						page = (Webpage) Utility.fromString(serialisedPage);
					} catch (IOException e) {
						e.printStackTrace();
						return;
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
						return;
					}
					if (!isWebpageParsed(page.getUrl())) {
						addWebpage(page); 
						workingOn.remove(page.getUrl());
					}
					int added = 0;
					for(String url : page.getOutgoingLinks()){
						if(!unparsedLinks.contains(url) && !isWebpageParsed(url) && !workingOn.contains(url)) {
							if(urlFilter.equals("")){
								unparsedLinks.addLast(url);
								added++;
							}else if(url.indexOf(urlFilter) != -1){
								unparsedLinks.addLast(url);
								added++;
							}
						}
					}
					if(page.getSelectedContent() != null){
						log("> "+page.getSelectedContent());
					}
					log("added " + added + " URLs to queue. unparsedUrls total: "+unparsedLinks.size());
				} else {
					log("Crawler rejected because: " + reply.getContent());
				}
			} else {
				//log("ParseResponseReceiver block()");
				block();
			}

		}

		
	}
}

