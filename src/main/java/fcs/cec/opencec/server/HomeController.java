package fcs.cec.opencec.server;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.cloud.FirestoreClient;

@Controller
public class HomeController {
	private static final Logger LOGGER = LoggerFactory.getLogger(HomeController.class);

	@GetMapping(value = "/")
	public String welcome(Model model) throws InterruptedException, ExecutionException {
//		Firestore db = FirestoreClient.getFirestore();
//		// detele account 488857841634271
//		ApiFuture<WriteResult> writeResult = db.collection("Account").document("488857841634271").delete();
//		LOGGER.info("DELETE ACCOUNT DONE");
//		// end delete
//		ApiFuture<QuerySnapshot> future = db.collection("LessonMember").whereEqualTo("accountId", "488857841634271")
//				.get();
//		// future.get() blocks on response
//		List<QueryDocumentSnapshot> documents = future.get().getDocuments();
//		for (DocumentSnapshot document : documents) {
//			System.out.println(document.getId());
//			ApiFuture<WriteResult> writeResultLessonMember = db.collection("LessonMember").document(document.getId())
//					.delete();
//			LOGGER.info("DELETE LESSONMEMBER DONE");
//		}
//		ApiFuture<QuerySnapshot> future2 = db.collection("JourneyDay").whereEqualTo("accountId", "488857841634271")
//				.get();
//		// future.get() blocks on response
//		List<QueryDocumentSnapshot> document2s = future2.get().getDocuments();
//		for (DocumentSnapshot document : document2s) {
//			ApiFuture<WriteResult> writeResultLessonMember = db.collection("JourneyDay").document(document.getId())
//					.delete();
//			LOGGER.info("DELETE JourneyDay DONE");
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
