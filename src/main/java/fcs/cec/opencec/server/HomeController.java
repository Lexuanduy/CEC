package fcs.cec.opencec.server;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.cloud.FirestoreClient;

import fcs.cec.opencec.entity.MemberPost;

@Controller
public class HomeController {
	private static final Logger LOGGER = LoggerFactory.getLogger(HomeController.class);

	@GetMapping(value = "/")
	public String welcome(Model model) throws InterruptedException, ExecutionException {
//		Firestore db = FirestoreClient.getFirestore();
//		String memberId = "771517479977371";
//		DocumentReference docRefmp = db.collection("MemberPost").document("1784461175160264_2389589267980782");
//		ApiFuture<DocumentSnapshot> futuremp = docRefmp.get();
//		DocumentSnapshot documentmp = futuremp.get();
//		if (documentmp.exists()) {
//			System.out.println("Document data: " + documentmp.getData());
//			MemberPost mb = documentmp.toObject(MemberPost.class);
//			memberId = mb.getPosterId();
//		} else {
//			System.out.println("No such document!");
//		}
//		LOGGER.info("member id: " + memberId);
//
//		ApiFuture<QuerySnapshot> future = db.collection("Account").whereEqualTo("memberId", memberId).get();
//		// future.get() blocks on response
//		List<QueryDocumentSnapshot> documents = future.get().getDocuments();
//		for (DocumentSnapshot document : documents) {
//			LOGGER.info("account: " + document.getData());
//		}
//		ApiFuture<QuerySnapshot> futureLM = db.collection("LessonMember").whereEqualTo("memberId", memberId).get();
//		// future.get() blocks on response
//		List<QueryDocumentSnapshot> doc1s = futureLM.get().getDocuments();
//		for (DocumentSnapshot document : doc1s) {
//			LOGGER.info("lesson member: " + document.getData());
//		}
//		ApiFuture<QuerySnapshot> futureJD = db.collection("JourneyDay").whereEqualTo("memberId", memberId).get();
//		// future.get() blocks on response
//		List<QueryDocumentSnapshot> doc2s = futureJD.get().getDocuments();
//		for (DocumentSnapshot document : doc2s) {
//			LOGGER.info("journey day: " + document.getData());
//		}
		return "views/dashboard";
	}

	@GetMapping(value = "/support")
	public String support(Model model) throws InterruptedException, ExecutionException {
		return "support/support";
	}

	@GetMapping(value = "/volunteer")
	public String volunteer(Model model) throws InterruptedException, ExecutionException {
		return "volunteer/volunteer";
	}

	@GetMapping(value = "/private-policy")
	public String privacy() throws InterruptedException, ExecutionException {
		return "privacy/policy";
	}

	@GetMapping(value = "/verify-token")
	public String verifyToken() throws InterruptedException, ExecutionException {
		return "check-idToken/check-token";
	}
}
