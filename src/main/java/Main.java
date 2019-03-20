import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class Main {

	public static void main(String[] args) throws IOException {
		//story_body_container
		Document doc = Jsoup.connect("https://m.facebook.com/groups/cec.edu.vn/permalink/2259370051002705/").get();
		
		System.out.println(doc.select("meta[property=\"og:title\"]").attr("content"));

	}

}
