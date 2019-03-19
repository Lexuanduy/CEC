package fcs.cec.opencec.server;

import java.io.IOException;
import java.util.ArrayList;
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

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;

import fcs.cec.opencec.entity.Lesson;

@Controller
public class LessonController {
	private static final Logger LOGGER = LoggerFactory.getLogger(LessonController.class);
//	static ArrayList<Lesson> lessonList = new ArrayList<Lesson>();
//
//	static {
//		Document doc = null;
//		String url = "https://script.googleusercontent.com/macros/echo?user_content_key=LEy1GAo9wjoLRZc9iC7Foj2_FlhoI-giInrErm2cV4u5x9ofZTKeL6S-8j3gm3s6KeWjkqEuwzhS-z8jFYAGLHj4X4Uv50wjm5_BxDlH2jW0nuo2oDemN9CCS2h10ox_1xSncGQajx_ryfhECjZEnCT0QRJ7P_-LtV3tAd8_b_dUnbO1rEvbeLLB2eAoIGhp1hENMaacOI9TktsviLkDHJlUq1JAmpDs&lib=MmSKrXssQcdpiSXxZX7nm1QZVzjmXS3D2";
//		try {
//			doc = Jsoup.connect(url).get();
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//		Element e = doc.select("lesson").first();
//		LOGGER.info("");
//		//for (Element element : e) {
////			lessonList.add(new Lesson(name, audioURL, videoURL, imageURL1, imageURL2));
//		//}
//
//	}

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
