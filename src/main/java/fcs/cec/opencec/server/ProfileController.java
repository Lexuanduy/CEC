package fcs.cec.opencec.server;

import java.io.IOException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.cloud.FirestoreClient;

import fcs.cec.opencec.entity.Member;
import fcs.cec.opencec.entity.MemberPost;

@Controller
public class ProfileController {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProfileController.class);

	@GetMapping(value = "m/{id}")
	public String profile(Model model, @PathVariable("id") String id)
			throws InterruptedException, ExecutionException, JsonParseException, JsonMappingException, IOException {
		Firestore db = FirestoreClient.getFirestore();
		LOGGER.info("id member: " + id);
		// get Member by document member id
		DocumentReference docRef = db.collection("Member").document(id);
		ApiFuture<DocumentSnapshot> future = docRef.get();
		Member member = new Member();
		DocumentSnapshot document = future.get();
		if (document.exists()) {
			member = document.toObject(Member.class);
		} else {
			LOGGER.info("No such document member!");
			Document doc = null;
			String url = "https://graph.facebook.com/" + id
					+ "?access_token=1326545090735920|EaDaF1Rk_p41xfQaCqp--qHpNJg";
			try {
				doc = Jsoup.connect(url).ignoreContentType(true).timeout(30000).get();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			LOGGER.info("connected");
			LOGGER.info("doc: " + doc);
			if(doc == null) {
				return "error/404";
			}
			String object = doc.select("body").text();
			JSONObject jsonObj = new JSONObject(object);
			String name = (String) jsonObj.get("name");
			LOGGER.info(name);
			member.setAvatar("");
			member.setId(id);
			member.setName(name);
			ApiFuture<WriteResult> futureMem = db.collection("Member").document(id).set(member);
		}
		// get MemberPost by posterId
		List<HashMap<String, String>> listMap = new ArrayList<>();
		ApiFuture<QuerySnapshot> queryPost = db.collection("MemberPost").whereEqualTo("posterId", id).get();
		List<MemberPost> posts = queryPost.get().toObjects(MemberPost.class);

		String postIdCut;
		String urlVideo;
		JSONObject jsonObj;
		String dateCreate;
		for (MemberPost memberPost : posts) {
			HashMap<String, String> hashMap = new HashMap();
			hashMap.put("posterId", id);
			hashMap.put("posterName", member.getName());
			jsonObj = new JSONObject(memberPost.getAttachments());
			urlVideo = "https://www.facebook.com/missybon/videos/" + jsonObj.get("url");
			hashMap.put("permalink", urlVideo);
			postIdCut = memberPost.getId().substring(17);
			LOGGER.info("postIdCut: " + postIdCut);
			hashMap.put("id", postIdCut);
			hashMap.put("content", memberPost.getContent());
			hashMap.put("picture", memberPost.getPicture());
			dateCreate = convertTime(memberPost.getCreatedDate());
			hashMap.put("dateCreate", dateCreate);
			listMap.add(hashMap);
		}
		model.addAttribute("member", member);
		model.addAttribute("posts", listMap);
		return "profiles/profile";
	}

	@GetMapping(value = "p/{id}")
	public String fileDetail(Model model, @PathVariable("id") String id)
			throws InterruptedException, ExecutionException, JsonParseException, JsonMappingException, IOException {
		Firestore db = FirestoreClient.getFirestore();

		// get memberpost
		String docId = "1784461175160264_" + id;
		LOGGER.info("docId: " + docId);
		DocumentReference docRef = db.collection("MemberPost").document(docId);
		ApiFuture<DocumentSnapshot> future = docRef.get();
		MemberPost memberPost;
		DocumentSnapshot document = future.get();
		if (document.exists()) {
			LOGGER.info("member post exist!");
			memberPost = document.toObject(MemberPost.class);
			JSONObject jsonObj = new JSONObject(memberPost.getAttachments());
			String urlVideo = "https://www.facebook.com/missybon/videos/" + jsonObj.get("url");
			String dateCreate = convertTime(memberPost.getCreatedDate());
			memberPost.setPermalink(urlVideo);
			memberPost.setDateCreate(dateCreate);
		} else {
			LOGGER.info("No such document member post!");
			return "error/error-member-post";
		}

		// get member
		DocumentReference docRefMember = db.collection("Member").document(memberPost.getPosterId());
		ApiFuture<DocumentSnapshot> futureMember = docRefMember.get();
		Member member = new Member();
		DocumentSnapshot documentMember = futureMember.get();
		if (documentMember.exists()) {
			member = documentMember.toObject(Member.class);
		} else {
			return "error/error-member";
		}

		model.addAttribute("member", member);
		model.addAttribute("memberPost", memberPost);
		return "profiles/detail";
	}

	public String convertTime(long time) {
		Date date = new Date(time);
		Format format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		return format.format(date);
	}
}
