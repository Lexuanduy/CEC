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

import fcs.cec.opencec.entity.Account;
import fcs.cec.opencec.entity.MemberPost;

@Controller
public class HomeController {
	private static final Logger LOGGER = LoggerFactory.getLogger(HomeController.class);

	@GetMapping(value = "/")
	public String welcome(Model model) throws InterruptedException, ExecutionException {
		Firestore db = FirestoreClient.getFirestore();
//		String memberId = "2426737954263951";
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
//		DocumentReference docRefAcccount = db.collection("Account").document(memberId);
//		ApiFuture<DocumentSnapshot> future = docRefAcccount.get();
//		DocumentSnapshot document = future.get();
//		if (document.exists()) {
//			LOGGER.info("Document data account exist.");
//		} else {
//			LOGGER.info("No such document account!");
//		}
//		DocumentReference docRefLs1 = db.collection("LessonMember").document("12426737954263951");
//		ApiFuture<DocumentSnapshot> futureLs1 = docRefLs1.get();
//		DocumentSnapshot documentLs1 = futureLs1.get();
//		if (documentLs1.exists()) {
//			LOGGER.info("Document data ls1 exist.");
//		} else {
//			LOGGER.info("No such document ls1!");
//		}
//		DocumentReference docRefJd1 = db.collection("JourneyDay").document("3days12426737954263951");
//		ApiFuture<DocumentSnapshot> futureJd1 = docRefJd1.get();
//		DocumentSnapshot documentJd1 = futureJd1.get();
//		if (documentJd1.exists()) {
//			LOGGER.info("Document data j3ds1 exist.");
//		} else {
//			LOGGER.info("No such document j3ds1!");
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
