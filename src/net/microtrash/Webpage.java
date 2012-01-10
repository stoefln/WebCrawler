package net.microtrash;

import java.util.Vector;

public class Webpage {

	private String url;
	private String title;
	public Vector<String> linkUrls = new Vector<String>();
	
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
	
}
