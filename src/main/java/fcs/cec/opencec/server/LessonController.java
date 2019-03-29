package fcs.cec.opencec.server;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;

import fcs.cec.opencec.entity.Account;
import fcs.cec.opencec.entity.Lesson;
import fcs.cec.opencec.entity.LessonMember;

@Controller
public class LessonController {
	private static final Logger LOGGER = LoggerFactory.getLogger(LessonController.class);
	static ArrayList<Lesson> lessonList = new ArrayList<Lesson>();

	static {
		FirebaseApp.initializeApp();
		Document doc = null;
		String url = "https://script.googleusercontent.com/macros/echo?user_content_key=LEy1GAo9wjoLRZc9iC7Foj2_FlhoI-giInrErm2cV4u5x9ofZTKeL6S-8j3gm3s6KeWjkqEuwzhS-z8jFYAGLHj4X4Uv50wjm5_BxDlH2jW0nuo2oDemN9CCS2h10ox_1xSncGQajx_ryfhECjZEnCT0QRJ7P_-LtV3tAd8_b_dUnbO1rEvbeLLB2eAoIGhp1hENMaacOI9TktsviLkDHJlUq1JAmpDs&lib=MmSKrXssQcdpiSXxZX7nm1QZVzjmXS3D2";
		try {
			doc = Jsoup.connect(url).timeout(30000).get();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		Elements elements = doc.select("lesson");
		Element eImg1 = null;
		Element eImg2 = null;
		Element audioMp3 = null;
		Element videoMp4 = null;
		String name = null;
		String img1 = null;
		String img2 = null;
		String audio = null;
		String video = null;
		Lesson lesson = null;
		for (Element element : elements) {
			name = element.attr("name");
			eImg1 = element.child(0);
			img1 = eImg1.text();
			eImg2 = element.child(1);
			img2 = eImg2.text();
			audioMp3 = element.child(2);
			audio = audioMp3.text();
			videoMp4 = element.child(3);
			video = audioMp3.text();
			lesson = new Lesson(name, audio, video, img1, img2);
			lessonList.add(lesson);
		}
	}

	@RequestMapping(value = "lesson/{id}", method = RequestMethod.GET)
	public String lesson(Model model, @PathVariable("id") String id, @CookieValue("uid") String uid,
			@CookieValue("facebookId") String facebookId, HttpServletResponse response)
			throws InterruptedException, ExecutionException, FirebaseAuthException, IOException {
		LOGGER.info("uid: " + uid);
		LOGGER.info("facebookId: " + facebookId);
		int idLesson = Integer.parseInt(id);
		Firestore db = FirestoreOptions.getDefaultInstance().getService();
//		String uid = null;
//		FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
//		uid = decodedToken.getUid();
		if (idLesson > 1) {
//			FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
//			uid = decodedToken.getUid();
			// get lessonmember check lesson learned
			int lessonOld = idLesson - 1;
			ApiFuture<QuerySnapshot> futurePost = db.collection("LessonMember").whereEqualTo("uid", uid)
					.whereEqualTo("lesson", lessonOld).get();
			LessonMember lessonMember = futurePost.get().getDocuments().get(0).toObject(LessonMember.class);
			int status = lessonMember.getStatus();
			if (status == 0) {
				LOGGER.info("fail next lesson");
				return "error/404";
			}
		}
//		ApiFuture<DocumentReference> addedDocRef = db.collection("LessonMember").add(data);

		// get lesson by lesson number
		for (Lesson lesson : lessonList) {
			if (lesson.getName().equals(id)) {
				model.addAttribute("lesson", lesson);
			}
		}
		// create new next lesson
		Map<String, Object> data = new HashMap<>();
		idLesson = idLesson + 1;
		data.put("lesson", idLesson);
		data.put("memberId", facebookId);
		data.put("memberName", "");
		data.put("postId", "");
		data.put("status", 0);
		data.put("url", "");
		data.put("uid", uid);
		data.put("accountId", facebookId);
		data.put("createdAt", System.currentTimeMillis() / 1000);
		data.put("updatedAt", System.currentTimeMillis() / 1000);
		String docId = String.valueOf(idLesson) + facebookId;
		DocumentReference docRef = db.collection("LessonMember").document(docId);
		ApiFuture<DocumentSnapshot> future = docRef.get();
		DocumentSnapshot document = future.get();
		if (document.exists()) {
			LOGGER.info("document LessonMember eixst!");
		} else {
			ApiFuture<WriteResult> addedDocRef = db.collection("LessonMember").document(docId).set(data);
		}

		return "lesson/lesson";
	}

	@RequestMapping(value = "checkVideo", method = RequestMethod.POST)
	public void checkVideo(Model model, @RequestParam String url, @CookieValue("idToken") String idToken,
			@RequestParam String numLesson, HttpServletResponse response)
			throws IOException, InterruptedException, ExecutionException, ServletException, FirebaseAuthException {
		LOGGER.info("idToken: " + idToken);
		FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
		String uid = decodedToken.getUid();
		Firestore db = FirestoreOptions.getDefaultInstance().getService();
		Document doc = Jsoup.connect(url).get();
		String lessonHashtag = doc.select(".bo .bt").text();
		Character numLessonVideo = lessonHashtag.charAt(6);
		int lessonCheckNow = Character.getNumericValue(numLessonVideo);
		String object = doc.select("#m_story_permalink_view .bb").attr("data-ft");
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> map = mapper.readValue(object, Map.class);
		String postId = (String) map.get("top_level_post_id");
		String memberId = (String) map.get("content_owner_id_new");
		String memberName = doc.select("meta[property=\"og:title\"]").attr("content");
		LOGGER.info("lessonNumber :" + numLesson);
//		lessonCheckNow = 3;
		String docId = null;
//		LOGGER.info("uid: " + uid);
		if (Integer.parseInt(numLesson) == lessonCheckNow) {
			ApiFuture<QuerySnapshot> futureLesson = db.collection("LessonMember").whereEqualTo("uid", uid)
					.whereEqualTo("lesson", lessonCheckNow).get();
			List<QueryDocumentSnapshot> lessonDocuments = futureLesson.get().getDocuments();
			for (DocumentSnapshot document : lessonDocuments) {
				docId = document.getId();
			}
			LOGGER.info("docId: " + docId);
			if (docId == null) {
				response.setStatus(401);
				return;
			}
			DocumentReference docRefLesson = db.collection("LessonMember").document(docId);
			Map<String, Object> updates = new HashMap<>();
			updates.put("memberId", memberId);
			updates.put("memberName", memberName);
			updates.put("postId", postId);
			updates.put("status", 1);
			updates.put("url", url);
			updates.put("updatedAt", System.currentTimeMillis() / 1000);
			ApiFuture<WriteResult> futureLessonMember = docRefLesson.update(updates);
			// map member in account
			ApiFuture<QuerySnapshot> futureAccount = db.collection("Account").whereEqualTo("uid", uid).get();
			Account account = futureAccount.get().getDocuments().get(0).toObject(Account.class);
			String id = account.getId();
			DocumentReference docRef = db.collection("Account").document(id);
			ApiFuture<WriteResult> future = docRef.update("memberId", memberId);
			// end map
			lessonCheckNow = lessonCheckNow + 1;
			String uriReturn = "/lesson/" + String.valueOf(lessonCheckNow);
			response.getWriter().print(String.valueOf(uriReturn));
			response.setStatus(200);
		} else {
			response.setStatus(404);
		}
	}
}
