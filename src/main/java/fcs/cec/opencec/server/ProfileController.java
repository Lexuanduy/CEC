package fcs.cec.opencec.server;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.json.JSONObject;
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
		}
		// get MemberPost by posterId
		ApiFuture<QuerySnapshot> queryPost = db.collection("MemberPost").whereEqualTo("posterId", id).get();
		List<MemberPost> posts = queryPost.get().toObjects(MemberPost.class);
		// get Video by posterId
		ApiFuture<QuerySnapshot> queryVideo = db.collection("Video").whereEqualTo("posterId", id).get();
		List<Video> videos = queryVideo.get().toObjects(Video.class);

		model.addAttribute("member", member);
		model.addAttribute("posts", posts);
		model.addAttribute("videos", videos);

		return "profiles/profile";
	}

	@GetMapping(value = "p/{id}")
	public String fileDetail(Model model, @PathVariable("id") String id)
			throws InterruptedException, ExecutionException {
		Firestore db = FirestoreOptions.getDefaultInstance().getService();
		// get video detail by id
		DocumentReference docRef = db.collection("Video").document(id);
		ApiFuture<DocumentSnapshot> future = docRef.get();
		Video video = null;
		String posterId = null;
		DocumentSnapshot document = future.get();
		if (document.exists()) {
			video = document.toObject(Video.class);
			posterId = video.getPosterId();
		} else {
			LOGGER.info("No such document video!");
		}
		// get member by posterId
		DocumentReference docRefMember = db.collection("Member").document(posterId);
		ApiFuture<DocumentSnapshot> futureMember = docRefMember.get();
		Member member = null;
		DocumentSnapshot documentMem = futureMember.get();
		if (documentMem.exists()) {
			member = documentMem.toObject(Member.class);
		} else {
			LOGGER.info("No such document member!");
		}
		// get memberpost
		ApiFuture<QuerySnapshot> futurePost = db.collection("MemberPost").whereEqualTo("posterId", posterId).get();
		List<QueryDocumentSnapshot> documents = futurePost.get().getDocuments();
		MemberPost memberPost = null;
		for (DocumentSnapshot documentPost : documents) {
		  memberPost = documentPost.toObject(MemberPost.class);
		}

		model.addAttribute("video", video);
		model.addAttribute("member", member);
		model.addAttribute("memberPost", memberPost);
		return "profiles/detail";
	}
}
