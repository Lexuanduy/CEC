package fcs.cec.opencec.entity;

public class MemberPost {
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getPosterId() {
		return posterId;
	}
	public void setPosterId(String posterId) {
		this.posterId = posterId;
	}
	public String getAttachments() {
		return attachments;
	}
	public void setAttachments(String attachments) {
		this.attachments = attachments;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public Long getCreatedDate() {
		return createdDate;
	}
	public void setCreatedDate(Long createdDate) {
		this.createdDate = createdDate;
	}
	public Long getLastUpdate() {
		return lastUpdate;
	}
	public void setLastUpdate(Long lastUpdate) {
		this.lastUpdate = lastUpdate;
	}
	public String getPermalink() {
		return permalink;
	}
	public void setPermalink(String permalink) {
		this.permalink = permalink;
	}
	public String getPicture() {
		return picture;
	}
	public void setPicture(String picture) {
		this.picture = picture;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	private String id;
	private String posterId;
	private String attachments;
	private String content;
	private Long createdDate;
	private Long lastUpdate;
	private String permalink;
	private String picture;
	private String type;
}
