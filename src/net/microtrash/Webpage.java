package net.microtrash;

import java.io.Serializable;
import java.util.ArrayList;

public class Webpage implements Serializable {

	private static final long serialVersionUID = 2322L;
	private String url;
	private String title;
	private ArrayList<String> outgoingLinks = new ArrayList<String>();
	private String selectedContent;

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
	public boolean addOutgoingLink(String linkUrl) {
		if(!outgoingLinks.contains(linkUrl)){
			return outgoingLinks.add(linkUrl);
		}
		return false;
	}
	public ArrayList<String> getOutgoingLinks(){
		return this.outgoingLinks;
	}

	public String getSelectedContent() {
		return selectedContent;
	}

	public void setSelectedContent(String selectedContent) {
		this.selectedContent = selectedContent;
	}
}
