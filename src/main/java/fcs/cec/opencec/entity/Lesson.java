package fcs.cec.opencec.entity;

public class Lesson {
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getAudioURL() {
		return audioURL;
	}
	public void setAudioURL(String audioURL) {
		this.audioURL = audioURL;
	}
	public String getVideoURL() {
		return videoURL;
	}
	public void setVideoURL(String videoURL) {
		this.videoURL = videoURL;
	}
	public String getImageURL1() {
		return imageURL1;
	}
	public void setImageURL1(String imageURL1) {
		this.imageURL1 = imageURL1;
	}
	public String getImageURL2() {
		return imageURL2;
	}
	public void setImageURL2(String imageURL2) {
		this.imageURL2 = imageURL2;
	}
	private String name;
	private String audioURL;
	private String videoURL;
	private String imageURL1;
	private String imageURL2;
}
