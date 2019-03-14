package fcs.cec.opencec.server;

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

import fcs.cec.opencec.entity.Member;

@Controller
public class ProfileController {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProfileController.class);

	@GetMapping(value = "m/{id}")
	public String profile(Model model, @PathVariable("id") String id) throws InterruptedException, ExecutionException {
		Firestore db = FirestoreOptions.getDefaultInstance().getService();
		LOGGER.info(id);
		DocumentReference docRef = db.collection("Member").document(id);
		// asynchronously retrieve the document
		ApiFuture<DocumentSnapshot> future = docRef.get();
		// future.get() blocks on response
		DocumentSnapshot document = future.get();
		if (document.exists()) {
			System.out.println("Document data: " + document.getData());
			Member member = document.toObject(Member.class);
			LOGGER.info(member.getName());
			model.addAttribute("member", member);
		} else {
			System.out.println("No such document!");
		}
		return "profiles/profile";
	}
}
