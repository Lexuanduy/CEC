import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.json.Json;
import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;

import ch.qos.logback.classic.Logger;
import fcs.cec.opencec.entity.Journey;
import fcs.cec.opencec.entity.Lesson;

public class Main {

	public static void main(String[] args) throws IOException, InterruptedException, ExecutionException, FirebaseAuthException {
		// story_body_container
//		https://www.facebook.com/hashtag/lesson8cec?source=feed_text&epa=HASHTAG
//		https://m.facebook.com/groups/cec.edu.vn/permalink/2259370051002705/
//		https://m.facebook.com/groups/cec.edu.vn/permalink/2264646963808347/
//		String url = "https://m.facebook.com/groups/cec.edu.vn/permalink/2262271487379228/";
//		Document doc = Jsoup.connect(url).get();
//		System.out.println(doc.html());
//		System.out.println(doc.select("#m_story_permalink_view .bb").attr("data-ft"));
//		String object = doc.select("#m_story_permalink_view .bb").attr("data-ft");
//		ObjectMapper mapper = new ObjectMapper();
//		Map<String,Object> map = mapper.readValue(object, Map.class);
//		System.out.println("top_level_post_id: " + map.get("top_level_post_id"));
//		System.out.println("content_owner_id_new: " + map.get("content_owner_id_new"));
//		System.out.println("lesson hashtag: " + doc.select(".bo .bt").text());
//		System.out.println("name member: " + doc.select("meta[property=\"og:title\"]").attr("content"));
//		System.out.println(doc.select("meta[property=\"og:title\"]").attr("content"));
//		
//		String title = doc.select("title").text();
//		System.out.println(title);
//		System.out.println(title.charAt(7));

//		Firestore db = FirestoreOptions.getDefaultInstance().getService();
//		Map<String, Object> data = new HashMap<>();
//		Map<String, Object> subdata = new HashMap<>();
//		subdata.put("Lesson 1", "1");
//		subdata.put("Lesson 2", "2");
//		data.put("numLesson", Arrays.asList("1", "2"));
//		data.put("uid", "VawkSF0bnmUmu7bMpC80sNn6Qft1");
//
//		db.collection("Learned").document("test2").set(data).get().getUpdateTime();
		
		// Update an existing document
//		DocumentReference docRef = db.collection("accounts").document("VawkSF0bnmUmu7bMpC80sNn6Qft1");
//		Map<String, Object> data = new HashMap<>();
//		data.put("Lesson 1", "1");
//		data.put("Lesson 2", "2");
//		// (async) Update one field
//		ApiFuture<WriteResult> future = docRef.update("numLesson", data);
		
	
		
		

		
//		FirebaseApp.initializeApp();
//		FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken("EAAS2fFjpbzABAD3Sy4mrb0VmexhUSwXjnk1IPOW0TDtxFYdvTjYlOVNkRNRjs58iGYSru6CfZBEe4uIqVc8VgH59zptkRQAj0kkIp16BX7cNsZCt55K9U0kcs0NFKyoUl1W4uF4BBF2JbBxZBY6QzQ9KwEXd6gr71hJ1RRsggZDZD");
//		String uid = decodedToken.getUid();
//		
//		System.out.println(uid);

		
//		Document doc = null;
//		String url = "https://script.googleusercontent.com/macros/echo?user_content_key=AH8p3WxEhxDFhaufV3L3moVwYAykzqrPyDsk6W5Nlh3M7uaqUwZuxSghoqQyW3kPOAnJS04oyJHl2F0LjZzr8dbUbiwZjhHem5_BxDlH2jW0nuo2oDemN9CCS2h10ox_1xSncGQajx_ryfhECjZEnCT0QRJ7P_-LtV3tAd8_b_dUnbO1rEvbeLLB2eAoIGhp1hENMaacOI9TktsviLkDHJlUq1JAmpDs&lib=MmSKrXssQcdpiSXxZX7nm1QZVzjmXS3D2";
//		try {
//			doc = Jsoup.connect(url).timeout(30000).get();
//		} catch (IOException e1) {
//			e1.printStackTrace();
//		}
//		Elements elements = doc.select("day");
//		Element ePdf = null;
//		Element videoMp4 = null;
//		String name = null;
//		String pdf = null;
//		String video = null;
//		Journey journey = null;
//		for (Element element : elements) {
//			name = element.attr("name");
//			ePdf = element.child(0);
//			pdf = ePdf.text();
//			videoMp4 = element.child(1);
//			video = videoMp4.text();
//			System.out.println("name: " + name);
//			System.out.println("pdf: " + pdf);
//			System.out.println("video: " + video);
//		}
	}

}
