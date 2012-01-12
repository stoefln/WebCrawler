package net.microtrash;

import java.io.Serializable;
import java.util.Vector;

public class Webpage implements Serializable {

	private static final long serialVersionUID = 2322L;
	private String url;
	private String title;
	private Vector<String> outgoingLinks = new Vector<String>();
	
	public Webpage(String url){
		this.url = url;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public void addOutgoingLink(String linkUrl) {
		if(!outgoingLinks.contains(linkUrl)){
			outgoingLinks.add(linkUrl);
		}
	}
	public Vector<String> getOutgoingLinks(){
		return this.outgoingLinks;
	}
}
