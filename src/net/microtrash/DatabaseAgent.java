package net.microtrash;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;

import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.util.Hashtable;
import java.util.Vector;

public class DatabaseAgent extends Agent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2L;

	private Hashtable<String,Webpage> db = new Hashtable<String,Webpage>();
	private Vector<AID> crawlers = new Vector<AID>();
	private String seedUrl;
	
	@Override
	protected void setup() {
		System.out.println("Hallo I'm the DatabaseAgent! My name is "+getAID().getName());
		
		Object[] args = getArguments(); 
		if (args != null && args.length > 0) {
			seedUrl = (String) args[0]; 
			System.out.println("Starting with seedUrl "+seedUrl);
		}else{
			// Make the agent terminate immediately
			System.out.println("No seedUrl specified"); 
			doDelete();
			return;
		}
		
		// 1) look for agents which have registered as "crawlers" every 10 seconds 
		addBehaviour(new TickerBehaviour(this, 10000) { 
			
			private static final long serialVersionUID = 1L;

			protected void onTick() {
		
				// Update the list of seller agents
				DFAgentDescription template = new DFAgentDescription(); 
				ServiceDescription sd = new ServiceDescription(); 
				sd.setType("crawling"); 
				template.addServices(sd);
				try { 
					DFAgentDescription[] result = DFService.search(myAgent, template); 

					for (int i = 0; i < result.length; ++i) {
						registerCrawlAgent(result[i].getName());
					}
				}catch(FIPAException fe) {
					fe.printStackTrace();
				}
				
			}			
		});
		
		// 2) loop through all available crawlers and push a linkUrl to each of them (as long as there are links to other webpages which are not present in the DB by now) 
		addBehaviour(new ParseRequestPerformer());
		super.setup();
	}
	
	@Override
	protected void takeDown() { 
		System.out.println("DatabaseAgent "+getAID().getName()+" sais good bye");
	}
	
	public void addWebpage(Webpage page) {
		db.put(page.getUrl(),page);
	}
	
	public boolean isWebpageParsed(String pageUrl){
		return db.contains(pageUrl);
	}
	
	protected void registerCrawlAgent(AID aid){
		if(crawlers.indexOf(aid) != -1){
			crawlers.add(aid);
		}
	}
	
}
