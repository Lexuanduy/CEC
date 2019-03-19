package fcs.cec.opencec.server;

import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;

import fcs.cec.opencec.entity.Lesson;

@Controller
public class LessonController {
	@GetMapping(value = "lesson/{id}")
	public String profile(Model model, @PathVariable("id") String id) throws InterruptedException, ExecutionException {
		Firestore db = FirestoreOptions.getDefaultInstance().getService();
		DocumentReference docRef = db.collection("lessons").document(id);
		ApiFuture<DocumentSnapshot> future = docRef.get();
		DocumentSnapshot document = future.get();
		Lesson lesson = null;
		if (document.exists()) {
			lesson = document.toObject(Lesson.class);
		} else {
			System.out.println("No such document!");
		}
		model.addAttribute("lesson", lesson);
		return "lesson/lesson";
	}
}
