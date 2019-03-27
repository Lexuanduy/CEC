package fcs.cec.opencec.server;

import java.io.IOException;
import java.util.ArrayList;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;

import fcs.cec.opencec.entity.Lesson;
import fcs.cec.opencec.entity.LessonMember;

@Controller
public class LessonController {
	private static final Logger LOGGER = LoggerFactory.getLogger(LessonController.class);
	static ArrayList<Lesson> lessonList = new ArrayList<Lesson>();

	static {
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

	@GetMapping(value = "lesson/{id}")
	public String profile(Model model, @PathVariable("id") String id, ServletRequest request, ServletResponse response)
			throws InterruptedException, ExecutionException {
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse) response;

		String uid = null;
		Cookie[] cookies = req.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (cookie.getName().equals("uid")) {
					uid = cookie.getValue();
				}
			}
		}
		int idLesson = Integer.parseInt(id);
		LOGGER.info("id lesson: " + idLesson);
		Firestore db = FirestoreOptions.getDefaultInstance().getService();

		if (idLesson > 1) {
			// get lessonmember check lesson learned
			int lessonOld = idLesson - 1;
			ApiFuture<QuerySnapshot> futurePost = db.collection("LessonMember").whereEqualTo("uid", uid)
					.whereEqualTo("lesson", lessonOld).get();
			List<QueryDocumentSnapshot> documents = futurePost.get().getDocuments();
			LessonMember lessonMember = null;
			for (DocumentSnapshot documentPost : documents) {
				lessonMember = documentPost.toObject(LessonMember.class);
			}
			int status = lessonMember.getStatus();
			if (status == 0) {
				LOGGER.info("fail next lesson");
				return "error/403";
			}
		}
		// set cookie
		res.addCookie(new Cookie("numLesson", id));
		LOGGER.info("id: " + id);
		// get cookie
		// get lesson by lesson number
		for (Lesson lesson : lessonList) {
			if (lesson.getName().equals(id)) {
				model.addAttribute("lesson", lesson);
			}
		}

		return "lesson/lesson";
	}

	@RequestMapping(value = "checkVideo", method = RequestMethod.POST)
	public void profile(Model model, ServletRequest request, ServletResponse response)
			throws IOException, InterruptedException, ExecutionException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse) response;
		Firestore db = FirestoreOptions.getDefaultInstance().getService();
		String lessonNumber = req.getParameter("lessonNumber");
		String url = req.getParameter("url");
		String uid = req.getParameter("uid");
		Document doc = Jsoup.connect(url).get();
		String lessonHashtag = doc.select(".bo .bt").text();
		LOGGER.info("lessonHashtag: " + lessonHashtag);
		Character numLessonVideo = lessonHashtag.charAt(6);
		int lessonCheckNow = Character.getNumericValue(numLessonVideo);
		LOGGER.info("num lesson video: " + lessonCheckNow);
		String object = doc.select("#m_story_permalink_view .bb").attr("data-ft");
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> map = mapper.readValue(object, Map.class);
		String postId = (String) map.get("top_level_post_id");
		LOGGER.info("postId: " + postId);
		String memberId = (String) map.get("content_owner_id_new");
		LOGGER.info("memberId: " + memberId);
		String memberName = doc.select("meta[property=\"og:title\"]").attr("content");
		LOGGER.info("memberName: " + memberName);
		String uri = "lesson/";
		LessonMember lessonMember = null;
		LOGGER.info("lessonNumber: " + lessonNumber);
//		lessonCheckNow = 1;
		if (Integer.parseInt(lessonNumber) == lessonCheckNow) {
			ApiFuture<QuerySnapshot> futureLesson = db.collection("LessonMember").whereEqualTo("uid", uid)
					.whereEqualTo("lesson", lessonCheckNow).get();
			// future.get() blocks on response
			List<QueryDocumentSnapshot> lessonDocuments = futureLesson.get().getDocuments();
			String docId;
			for (DocumentSnapshot document : lessonDocuments) {
				lessonMember = document.toObject(LessonMember.class);
				LOGGER.info("doc id lessonmember: " + document.getId());
				docId = document.getId();
				// Update an existing document
				DocumentReference docRef = db.collection("LessonMember").document(docId);
				// (async) Update one field
				Map<String, Object> updates = new HashMap<>();
				updates.put("memberId", memberId);
				updates.put("memberName", memberName);
				updates.put("postId", postId);
				updates.put("status", 1);
				updates.put("url", url);
				ApiFuture<WriteResult> futureLessonMember = docRef.update(updates);
			}

			// map member in account
//			DocumentReference docRef = db.collection("Account").document(uid);
//			ApiFuture<WriteResult> future = docRef.update("memberId", memberId);
//			WriteResult result = future.get();
//			System.out.println("Write result: " + result);
			// end map
			lessonCheckNow = lessonCheckNow + 1;
			String uriReturn = "/lesson/" + String.valueOf(lessonCheckNow);
			res.getWriter().print(String.valueOf(uriReturn));
			res.setStatus(200);
		} else {
			res.setStatus(404);
		}
	}
}
