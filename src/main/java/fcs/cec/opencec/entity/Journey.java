package fcs.cec.opencec.entity;

public class Journey {
	public Journey(String name, String pdfUrl, String videoURL) {
		super();
		this.name = name;
		this.pdfUrl = pdfUrl;
		this.videoURL = videoURL;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getPdfUrl() {
		return pdfUrl;
	}
	public void setPdfUrl(String pdfUrl) {
		this.pdfUrl = pdfUrl;
	}
	public String getVideoURL() {
		return videoURL;
	}
	public void setVideoURL(String videoURL) {
		this.videoURL = videoURL;
	}
	private String name;
	private String pdfUrl;
	private String videoURL;
}
