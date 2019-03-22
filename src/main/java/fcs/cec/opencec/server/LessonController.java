package fcs.cec.opencec.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.servlet.RequestDispatcher;
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

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;

import fcs.cec.opencec.entity.Account;
import fcs.cec.opencec.entity.Lesson;
import fcs.cec.opencec.entity.MemberPost;

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

		// get cookie
		String uid = null;
		Cookie[] cookies = req.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (cookie.getName().equals("uid")) {
					uid = cookie.getValue();
				}
			}
		}
		Firestore db = FirestoreOptions.getDefaultInstance().getService();

		// get lesson check lesson learned
		ApiFuture<QuerySnapshot> futurePost = db.collection("Account").whereEqualTo("uid", uid).get();
		List<QueryDocumentSnapshot> documents = futurePost.get().getDocuments();
		Account account = null;
		for (DocumentSnapshot documentPost : documents) {
			account = documentPost.toObject(Account.class);
		}
		int idLesson = Integer.parseInt(id);
		LOGGER.info("id lesson: " + idLesson);
		
		if(idLesson != 1) {
			int lessonLerned = account.getNumLesson().size();
			LOGGER.info("lessonLerned: " + lessonLerned);
			if ((lessonLerned > 1) && (lessonLerned < idLesson)) {
				LOGGER.info("fail next lesson");
				return "error/403";
			}
		}

		// get lesson by lesson number
		for (Lesson lesson : lessonList) {
			if (lesson.getName().equals(id)) {
				model.addAttribute("lesson", lesson);
			}
		}

		// add lesson learned, update account
		DocumentReference docRef = db.collection("Account").document(uid);
		Map<String, Object> data = new HashMap<>();
		for (int i = 1; i < idLesson + 1; i++) {
			String nameLesson = "Lesson " + String.valueOf(i);
			data.put(nameLesson, String.valueOf(i));
		}
		ApiFuture<WriteResult> future = docRef.update("numLesson", data);

		return "lesson/lesson";
	}

	@RequestMapping(value = "checkVideo", method = RequestMethod.POST)
	public void profile(Model model, ServletRequest request, ServletResponse response)
			throws IOException, InterruptedException, ExecutionException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse) response;

		String url = req.getParameter("url");
		String uid = req.getParameter("uid");
		LOGGER.info("uid user: " + uid);
		Document doc = Jsoup.connect(url).get();
		String title = doc.select("title").text();
		Character numLesson = title.charAt(7);
		LOGGER.info("num lesson: " + numLesson);
		Firestore db = FirestoreOptions.getDefaultInstance().getService();
		ApiFuture<QuerySnapshot> future = db.collection("Account").whereEqualTo("uid", uid).get();
		List<QueryDocumentSnapshot> documents = future.get().getDocuments();
		Account account = null;
		for (DocumentSnapshot documentPost : documents) {
			account = documentPost.toObject(Account.class);
		}
		int lessonLerned = account.getNumLesson().size();
		LOGGER.info("size: " + lessonLerned);
		int lessonCheckNow = Character.getNumericValue(numLesson);
		LOGGER.info("lesson check now: " + lessonCheckNow);
//		int lessonCheckNow = 4;
		if (lessonLerned == lessonCheckNow) {
			LOGGER.info("next lesson");
			DocumentReference docRef = db.collection("Account").document(uid);
			Map<String, Object> data = new HashMap<>();
			for (int i = 1; i < lessonLerned + 2; i++) {
				String nameLesson = "Lesson " + String.valueOf(i);
				data.put(nameLesson, String.valueOf(i));
			}
			ApiFuture<WriteResult> futureAccount = docRef.update("numLesson", data);
			int num = lessonLerned + 1;
			String urlreturn = "/lesson/" + num;
			res.getWriter().print(urlreturn);
		}
	}
}
