import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.json.Json;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.WriteResult;

import fcs.cec.opencec.entity.Lesson;

public class Main {

	public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
		// story_body_container
//		https://www.facebook.com/hashtag/lesson8cec?source=feed_text&epa=HASHTAG
//		https://m.facebook.com/groups/cec.edu.vn/permalink/2259370051002705/
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

	}

}
