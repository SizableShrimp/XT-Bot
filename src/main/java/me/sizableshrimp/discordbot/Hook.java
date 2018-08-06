package me.sizableshrimp.discordbot;

public class Hook {
	private String content;
	private String date;
	private String link;
	
	public Hook(String content, String date, String link) {
		this.content = content;
		this.date = date;
		this.link = link;
	}
	
	public String getContent() {
		return content;
	}
	
	public String getDate() {
		return date;
	}
	
	public String getLink() {
		return link;
	}
	
	public void setContent(String content) {
		this.content = content;
	}
	
	public void setDate(String date) {
		this.date = date;
	}
	
	public void setLink(String link) {
		this.link = link;
	}
}
