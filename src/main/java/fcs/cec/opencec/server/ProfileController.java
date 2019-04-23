package fcs.cec.opencec.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;

import fcs.cec.opencec.entity.Member;
import fcs.cec.opencec.entity.MemberPost;
import fcs.cec.opencec.entity.Video;

@Controller
public class ProfileController {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProfileController.class);

	@GetMapping(value = "m/{id}")
	public String profile(Model model, @PathVariable("id") String id) throws InterruptedException, ExecutionException {
		Firestore db = FirestoreOptions.getDefaultInstance().getService();
		// get Member by document member id
		DocumentReference docRef = db.collection("Member").document(id);
		ApiFuture<DocumentSnapshot> future = docRef.get();
		Member member = null;
		DocumentSnapshot document = future.get();
		if (document.exists()) {
			member = document.toObject(Member.class);
		} else {
			LOGGER.info("No such document member!");
			return "";
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
			throws InterruptedException, ExecutionException {
		Firestore db = FirestoreOptions.getDefaultInstance().getService();

		// get memberpost
		DocumentReference docRef = db.collection("MemberPost").document(id);
		ApiFuture<DocumentSnapshot> future = docRef.get();
		MemberPost memberPost = null;
		DocumentSnapshot document = future.get();
		if (document.exists()) {
			memberPost = document.toObject(MemberPost.class);
		} else {
			LOGGER.info("No such document member post!");
			return "";
		}

		// get member
		DocumentReference docRefMember = db.collection("Member").document(memberPost.getPosterId());
		ApiFuture<DocumentSnapshot> futureMember = docRefMember.get();
		Member member = null;
		DocumentSnapshot documentMember = futureMember.get();
		if (documentMember.exists()) {
			member = documentMember.toObject(Member.class);
		} else {
			LOGGER.info("No such document member!");
			return "";
		}

		model.addAttribute("member", member);
		model.addAttribute("memberPost", memberPost);
		return "profiles/detail";
	}
}
