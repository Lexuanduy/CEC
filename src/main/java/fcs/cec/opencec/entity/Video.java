package fcs.cec.opencec.entity;

public class Video {
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
	public String getPosterName() {
		return posterName;
	}
	public void setPosterName(String posterName) {
		this.posterName = posterName;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
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
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public Long getCreatedDate() {
		return createdDate;
	}
	public void setCreatedDate(Long createdDate) {
		this.createdDate = createdDate;
	}
	private String id;
	private String posterId;
	private String posterName;
	private String content;
	private String permalink;
	private String picture;
	private String url;
	private String status;
	private Long createdDate;
}
