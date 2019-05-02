package fcs.cec.opencec.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

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

import fcs.cec.opencec.entity.Member;
import fcs.cec.opencec.entity.MemberPost;

@Controller
public class ProfileController {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProfileController.class);

	@GetMapping(value = "m/{id}")
	public String profile(Model model, @PathVariable("id") String id)
			throws InterruptedException, ExecutionException, JsonParseException, JsonMappingException, IOException {
		Firestore db = FirestoreOptions.getDefaultInstance().getService();
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
			String object = doc.select("body").text();
			ObjectMapper mapper = new ObjectMapper();
			Map<String, Object> map = mapper.readValue(object, Map.class);
			String name = (String) map.get("name");
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
		for (MemberPost memberPost : posts) {
			HashMap<String, String> hashMap = new HashMap();
			hashMap.put("posterId", id);
			hashMap.put("posterName", member.getName());
			hashMap.put("permalink", memberPost.getPermalink());
			hashMap.put("id", memberPost.getId());
			hashMap.put("content", memberPost.getContent());
			hashMap.put("picture", memberPost.getPicture());
			listMap.add(hashMap);
		}
		model.addAttribute("member", member);
		model.addAttribute("posts", listMap);
		return "profiles/profile";
	}

	@GetMapping(value = "p/{id}")
	public String fileDetail(Model model, @PathVariable("id") String id)
			throws InterruptedException, ExecutionException, JsonParseException, JsonMappingException, IOException {
		Firestore db = FirestoreOptions.getDefaultInstance().getService();

		// get memberpost
		String docId = "1784461175160264_" + id;
		DocumentReference docRef = db.collection("MemberPost").document(docId);
		ApiFuture<DocumentSnapshot> future = docRef.get();
		MemberPost memberPost = new MemberPost();
		DocumentSnapshot document = future.get();
		if (document.exists()) {
			memberPost = document.toObject(MemberPost.class);
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
}
