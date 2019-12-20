package fcs.cec.opencec.server;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.ComputeEngineCredentials;
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
import com.google.firebase.cloud.FirestoreClient;

import fcs.cec.opencec.entity.Account;
import fcs.cec.opencec.entity.JourneyDay;
import fcs.cec.opencec.entity.Lesson;
import fcs.cec.opencec.entity.LessonMember;

@Controller
public class LessonController {
	private static final Logger LOGGER = LoggerFactory.getLogger(LessonController.class);
	static ArrayList<Lesson> lessonList = new ArrayList<Lesson>();

	static {
//		GoogleCredentials credentials = null;
//		try {
//			credentials = GoogleCredentials.fromStream(
//					new FileInputStream("/var/lib/tomcat8/opencec-firebase-adminsdk-ccqab-9f50c0997b.json"));
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		FirebaseOptions options = new FirebaseOptions.Builder().setCredentials(credentials).setProjectId("opencec")
//				.build();

		Resource r = new DefaultResourceLoader().getResource("/key.json");
		GoogleCredentials cred = null;
		try {
			cred = GoogleCredentials.fromStream(r.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
		FirestoreOptions options = FirestoreOptions.newBuilder()
				.setCredentials(cred)
				.build();
		FirebaseApp.initializeApp(options.toString());

//		FirebaseApp.initializeApp();

		Document doc = null;
		String url = "https://script.googleusercontent.com/macros/echo?user_content_key=dmTT0L5ltyjs6C0mzfB8Kf1FkNPCAqiExMVyEY7hPKS9QIrht-BvnAzuAZYIZvlrnSaC13gWKjpsPFnfMb3lT9L5wfimIUbgm5_BxDlH2jW0nuo2oDemN9CCS2h10ox_1xSncGQajx_ryfhECjZEnCT0QRJ7P_-LtV3tAd8_b_dUnbO1rEvbeLLB2eAoIGhp1hENMaacOI9TktsviLkDHJlUq1JAmpDs&lib=MmSKrXssQcdpiSXxZX7nm1QZVzjmXS3D2";
		try {
			doc = Jsoup.connect(url).timeout(30000).get();
		} catch (IOException e1) {
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
			LOGGER.info("lesson: " + name);
			eImg1 = element.child(0);
			img1 = eImg1.text();
			eImg2 = element.child(1);
			img2 = eImg2.text();
			audioMp3 = element.child(2);
			audio = audioMp3.text();
			videoMp4 = element.child(3);
			video = videoMp4.text();
			lesson = new Lesson(name, audio, video, img1, img2);
			lessonList.add(lesson);
		}
	}

	@RequestMapping(value = "lesson/{id}", method = RequestMethod.GET)
	public String openLesson(HttpServletRequest request, Model model,
			@CookieValue(value = "idToken", required = false) String idToken, @PathVariable("id") String id,
			@RequestParam(value = "v", required = false) String v,
			@RequestParam(value = "me", required = false) String me)
			throws NoSuchAlgorithmException, InterruptedException, ExecutionException {
		LOGGER.info("lesson: " + id);
		LOGGER.info("v: " + v);
		LOGGER.info("me: " + me);
		Firestore db = FirestoreClient.getFirestore();
		FirebaseToken decodedToken = null;

		int idLesson = Integer.parseInt(id);
		LOGGER.info("idLesson: " + idLesson);
		if (idLesson < 1) {
			LOGGER.info("lesson < 1");
			return "error/error-lesson";
		}
		if (idLesson == 1) {
			LOGGER.info("CHECK ID TOKEN: " + idToken);
			if (idToken != null) {
				try {
					decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
					Cookie[] cookies = request.getCookies();
					String facebookId = null;
					String photoURL = null;
					String uid = null;
					String displayName = null;
					String email = null;
					if (cookies != null) {
						for (Cookie cookie : cookies) {
							if (cookie.getName().equals("facebookId")) {
								facebookId = cookie.getValue();
							}
							if (cookie.getName().equals("photoURL")) {
								photoURL = cookie.getValue();
							}
							if (cookie.getName().equals("uid")) {
								uid = cookie.getValue();
							}
							if (cookie.getName().equals("displayName")) {
								displayName = cookie.getValue();
							}
							if (cookie.getName().equals("email")) {
								email = cookie.getValue();
							}
						}
					}
					LOGGER.info("get cookie: " + facebookId + " - " + photoURL + " - " + uid + " - " + displayName
							+ " - " + email);
					if (facebookId != null) {
						DocumentReference docRefAcccount = db.collection("Account").document(facebookId);
						ApiFuture<DocumentSnapshot> future = docRefAcccount.get();
						DocumentSnapshot document = future.get();
						if (document.exists()) {
							LOGGER.info("Document data account exist.");
						} else {
							LOGGER.info("No such document account!");
							if (facebookId != null) {
								Account account = new Account();
								account.setId(facebookId);
								account.setDisplayName(displayName);
								account.setEmail(email);
								account.setMemberId(facebookId);
								account.setRole("user");
								account.setUid(uid);
								ApiFuture<WriteResult> futureAddAccount = db.collection("Account").document(facebookId)
										.set(account);
							}
						}

						LOGGER.info("check lesson1.");
						String docLs1Id = "1" + facebookId;
						LOGGER.info("doc ls1: " + docLs1Id);
						DocumentReference docRefLs1 = db.collection("LessonMember").document(docLs1Id);
						ApiFuture<DocumentSnapshot> futureLs1 = docRefLs1.get();
						DocumentSnapshot documentLs1 = futureLs1.get();
						if (documentLs1.exists()) {
							LOGGER.info("Document data lesson 1 exist.");
						} else {
							LOGGER.info("No such document account lesson1!");
							LessonMember ls1 = new LessonMember();
							ls1.setLesson(1);
							ls1.setMemberId(facebookId);
							ls1.setMemberName(displayName);
							ls1.setPostId("");
							ls1.setStatus(0);
							ls1.setUrl("");
							ls1.setAccountId(facebookId);
							ls1.setUid(uid);
							ls1.setCreatedAt(System.currentTimeMillis());
							ls1.setCreatedAt(System.currentTimeMillis());
							ApiFuture<WriteResult> futureAddLs = db.collection("LessonMember").document(docLs1Id)
									.set(ls1);
						}

						LOGGER.info("check journey 3days1");
						String docJourneyDayFirst = "3days1" + facebookId;
						LOGGER.info("doc jd first id: " + docJourneyDayFirst);
						DocumentReference docRefJd1 = db.collection("JourneyDay").document(docJourneyDayFirst);
						ApiFuture<DocumentSnapshot> futureJd1 = docRefJd1.get();
						DocumentSnapshot documentJd1 = futureJd1.get();
						if (documentJd1.exists()) {
							LOGGER.info("Document data journey 3days1 exist.");
						} else {
							LOGGER.info("No such document data journey 3days1 exist.");
							JourneyDay jd = new JourneyDay();
							jd.setDay(1);
							jd.setJourney("3days");
							jd.setMemberId(facebookId);
							jd.setMemberName(displayName);
							jd.setPostId("");
							jd.setStatus(0);
							jd.setUrl("");
							jd.setAccountId(facebookId);
							jd.setUid(uid);
							jd.setCreatedAt(System.currentTimeMillis());
							jd.setUpdatedAt(System.currentTimeMillis());
							ApiFuture<WriteResult> futureAddJd = db.collection("JourneyDay")
									.document(docJourneyDayFirst).set(jd);
						}
					}
					// end check
				} catch (FirebaseAuthException e) {
					// TODO Auto-generated catch block
					LOGGER.info("catch decodedToken");
					List<HashMap<String, String>> listMap = new ArrayList<>();
					HashMap<String, String> hashMap = new HashMap();
					String urlLesson = "/lesson/" + String.valueOf(idLesson);
					hashMap.put("urlLesson", urlLesson);
					listMap.add(hashMap);
					model.addAttribute("lesson", listMap);
					return "check-idToken/check-token";
				}
			}
		}

		if (v != null) {
			String str = id + me;
			LOGGER.info("str: " + str);

			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] hashInBytes = md.digest(str.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder();
			for (byte b : hashInBytes) {
				sb.append(String.format("%02x", b));
			}
			LOGGER.info("sb: " + sb);
			if (v.equalsIgnoreCase(sb.toString())) {

			} else {
				LOGGER.info("error lesson.");
				return "error/error-lesson";
			}

			for (Lesson lesson : lessonList) {
				if (lesson.getName().equals(id)) {
					model.addAttribute("lesson", lesson);
				}
			}
		} else {
			if (idLesson > 1 && idLesson < 24) {
				int lessonOld = idLesson - 1;
				LOGGER.info("idToken: " + idToken);
				if (idToken == null) {
					LOGGER.info("check idToken null.");
					List<HashMap<String, String>> listMap = new ArrayList<>();
					HashMap<String, String> hashMap = new HashMap();
					String urlLesson = "/lesson/" + String.valueOf(idLesson);
					hashMap.put("urlLesson", urlLesson);
					listMap.add(hashMap);
					model.addAttribute("lesson", listMap);
					return "check-idToken/check-token";
				}

				try {
					decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
				} catch (FirebaseAuthException e) {
					// TODO Auto-generated catch block
					List<HashMap<String, String>> listMap = new ArrayList<>();
					HashMap<String, String> hashMap = new HashMap();
					String urlLesson = "/lesson/" + String.valueOf(idLesson);
					hashMap.put("urlLesson", urlLesson);
					listMap.add(hashMap);
					model.addAttribute("lesson", listMap);
					return "check-idToken/check-token";
				}
				String uid = decodedToken.getUid();
				// get doc id account
				ApiFuture<QuerySnapshot> futureAccount = db.collection("Account").whereEqualTo("uid", uid).get();
				List<QueryDocumentSnapshot> accountDocuments = futureAccount.get().getDocuments();
				String facebookId = null;
				for (DocumentSnapshot document : accountDocuments) {
					facebookId = document.getId();
				}

				String docLessonMember = lessonOld + facebookId;
				DocumentReference docRefLessonMember = db.collection("LessonMember").document(docLessonMember);
				ApiFuture<DocumentSnapshot> futureLessonMember = docRefLessonMember.get();
				DocumentSnapshot documentLessonMember = futureLessonMember.get();
				if (documentLessonMember.exists()) {
					LOGGER.info("check status old lesson!");
					LessonMember lessonMember = documentLessonMember.toObject(LessonMember.class);
					if (lessonMember.getStatus() == 0) {
						LOGGER.info("fail next lesson");
						return "error/error-lesson-old";
					}
				} else {
					LOGGER.info("check status old lesson but document not exist!");
					return "error/error-lesson-old";
				}
				if (idLesson == 2) {
					LOGGER.info("lesson 2.");
					Map<String, Object> data = new HashMap<>();
					idLesson = 2;
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
					DocumentReference docRefLesson = db.collection("LessonMember").document(docId);
					ApiFuture<DocumentSnapshot> futureLesson = docRefLesson.get();
					DocumentSnapshot documentLesson = futureLesson.get();
					if (documentLesson.exists()) {
						LOGGER.info("document LessonMember exist!");
					} else {
						LOGGER.info("create doccument lesson2!");
						ApiFuture<WriteResult> addedDocRef = db.collection("LessonMember").document(docId).set(data);
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
				DocumentReference docRefLesson = db.collection("LessonMember").document(docId);
				ApiFuture<DocumentSnapshot> futureLesson = docRefLesson.get();
				DocumentSnapshot documentLesson = futureLesson.get();
				if (documentLesson.exists()) {
					LOGGER.info("document LessonMember exist!");
				} else {
					ApiFuture<WriteResult> addedDocRef = db.collection("LessonMember").document(docId).set(data);
				}
			}
			if (idLesson >= 25) {
				LOGGER.info("lesson > 24");
				return "error/error-lesson";
			}

			// get lesson by lesson number
			for (Lesson lesson : lessonList) {
				if (lesson.getName().equals(id)) {
					model.addAttribute("lesson", lesson);
				}
			}
		}

		return "lesson/lesson";
	}

//	public String lesson(Model model, @PathVariable("id") String id,
//			@CookieValue(value = "idToken", required = false) String idToken, HttpServletResponse response)
//			throws InterruptedException, ExecutionException, IOException {
//		int idLesson = Integer.parseInt(id);
//		LOGGER.info("idLesson: " + idLesson);
//		
//		
////		Firestore db = FirestoreOptions.getDefaultInstance().getService();
//		Firestore db = FirestoreClient.getFirestore();
//
//		if (idLesson < 1) {
//			LOGGER.info("lesson < 1");
//			return "error/error-lesson";
//		}
//		if (idLesson > 1 && idLesson < 24) {
//			int lessonOld = idLesson - 1;
//			LOGGER.info("idToken: " + idToken);
//			if (idToken == null) {
//				LOGGER.info("check idToken null.");
//				List<HashMap<String, String>> listMap = new ArrayList<>();
//				HashMap<String, String> hashMap = new HashMap();
//				String urlLesson = "/lesson/" + String.valueOf(idLesson);
//				hashMap.put("urlLesson", urlLesson);
//				listMap.add(hashMap);
//				model.addAttribute("lesson", listMap);
//				return "check-idToken/check-token";
//			}
//
//			FirebaseToken decodedToken = null;
//			try {
//				decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
//			} catch (FirebaseAuthException e) {
//				// TODO Auto-generated catch block
//				List<HashMap<String, String>> listMap = new ArrayList<>();
//				HashMap<String, String> hashMap = new HashMap();
//				String urlLesson = "/lesson/" + String.valueOf(idLesson);
//				hashMap.put("urlLesson", urlLesson);
//				listMap.add(hashMap);
//				model.addAttribute("lesson", listMap);
//				return "check-idToken/check-token";
//			}
//			String uid = decodedToken.getUid();
//			// get doc id account
//			ApiFuture<QuerySnapshot> futureAccount = db.collection("Account").whereEqualTo("uid", uid).get();
//			List<QueryDocumentSnapshot> accountDocuments = futureAccount.get().getDocuments();
//			String facebookId = null;
//			for (DocumentSnapshot document : accountDocuments) {
//				facebookId = document.getId();
//			}
//
//			String docLessonMember = lessonOld + facebookId;
//			DocumentReference docRefLessonMember = db.collection("LessonMember").document(docLessonMember);
//			ApiFuture<DocumentSnapshot> futureLessonMember = docRefLessonMember.get();
//			DocumentSnapshot documentLessonMember = futureLessonMember.get();
//			if (documentLessonMember.exists()) {
//				LOGGER.info("check status old lesson!");
//				LessonMember lessonMember = documentLessonMember.toObject(LessonMember.class);
//				if (lessonMember.getStatus() == 0) {
//					LOGGER.info("fail next lesson");
//					return "error/error-lesson-old";
//				}
//			} else {
//				LOGGER.info("check status old lesson but document not exist!");
//				return "error/error-lesson-old";
//			}
//			if (idLesson == 2) {
//				LOGGER.info("lesson 2.");
//				Map<String, Object> data = new HashMap<>();
//				idLesson = 2;
//				data.put("lesson", idLesson);
//				data.put("memberId", facebookId);
//				data.put("memberName", "");
//				data.put("postId", "");
//				data.put("status", 0);
//				data.put("url", "");
//				data.put("uid", uid);
//				data.put("accountId", facebookId);
//				data.put("createdAt", System.currentTimeMillis() / 1000);
//				data.put("updatedAt", System.currentTimeMillis() / 1000);
//				String docId = String.valueOf(idLesson) + facebookId;
//				DocumentReference docRefLesson = db.collection("LessonMember").document(docId);
//				ApiFuture<DocumentSnapshot> futureLesson = docRefLesson.get();
//				DocumentSnapshot documentLesson = futureLesson.get();
//				if (documentLesson.exists()) {
//					LOGGER.info("document LessonMember exist!");
//				} else {
//					LOGGER.info("create doccument lesson2!");
//					ApiFuture<WriteResult> addedDocRef = db.collection("LessonMember").document(docId).set(data);
//				}
//			}
//			// create new next lesson
//			Map<String, Object> data = new HashMap<>();
//			idLesson = idLesson + 1;
//			data.put("lesson", idLesson);
//			data.put("memberId", facebookId);
//			data.put("memberName", "");
//			data.put("postId", "");
//			data.put("status", 0);
//			data.put("url", "");
//			data.put("uid", uid);
//			data.put("accountId", facebookId);
//			data.put("createdAt", System.currentTimeMillis() / 1000);
//			data.put("updatedAt", System.currentTimeMillis() / 1000);
//			String docId = String.valueOf(idLesson) + facebookId;
//			DocumentReference docRefLesson = db.collection("LessonMember").document(docId);
//			ApiFuture<DocumentSnapshot> futureLesson = docRefLesson.get();
//			DocumentSnapshot documentLesson = futureLesson.get();
//			if (documentLesson.exists()) {
//				LOGGER.info("document LessonMember exist!");
//			} else {
//				ApiFuture<WriteResult> addedDocRef = db.collection("LessonMember").document(docId).set(data);
//			}
//		}
//		if (idLesson >= 25) {
//			LOGGER.info("lesson > 24");
//			return "error/error-lesson";
//		}
//
//		// get lesson by lesson number
//		for (Lesson lesson : lessonList) {
//			if (lesson.getName().equals(id)) {
//				model.addAttribute("lesson", lesson); 
//			}
//		}
//		return "lesson/lesson";
//	}

//	@RequestMapping(value = "lesson/{id}", method = RequestMethod.GET)
//	public String lesson(Model model, @PathVariable("id") String id,
//			@CookieValue(value = "idToken", required = false) String idToken)
//			throws InterruptedException, ExecutionException, IOException {
//		int idLesson = Integer.parseInt(id);
//		LOGGER.info("idLesson: " + idLesson);
////		Firestore db = FirestoreOptions.getDefaultInstance().getService();
//		Firestore db = FirestoreClient.getFirestore();
//
//		if (idLesson < 1) {
//			LOGGER.info("lesson < 1");
//			return "error/error-lesson";
//		}
//		if (idLesson > 1 && idLesson < 24) {
//			int lessonOld = idLesson - 1;
//			LOGGER.info("idToken: " + idToken);
//			if (idToken == null) {
//				LOGGER.info("check idToken null.");
//				List<HashMap<String, String>> listMap = new ArrayList<>();
//				HashMap<String, String> hashMap = new HashMap();
//				String urlLesson = "/lesson/" + String.valueOf(idLesson);
//				hashMap.put("urlLesson", urlLesson);
//				listMap.add(hashMap);
//				model.addAttribute("lesson", listMap);
//				return "check-idToken/check-token";
//			}
//
//			FirebaseToken decodedToken = null;
//			try {
//				decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
//			} catch (FirebaseAuthException e) {
//				// TODO Auto-generated catch block
//				List<HashMap<String, String>> listMap = new ArrayList<>();
//				HashMap<String, String> hashMap = new HashMap();
//				String urlLesson = "/lesson/" + String.valueOf(idLesson);
//				hashMap.put("urlLesson", urlLesson);
//				listMap.add(hashMap);
//				model.addAttribute("lesson", listMap);
//				return "check-idToken/check-token";
//			}
//			String uid = decodedToken.getUid();
//			// get doc id account
//			ApiFuture<QuerySnapshot> futureAccount = db.collection("Account").whereEqualTo("uid", uid).get();
//			List<QueryDocumentSnapshot> accountDocuments = futureAccount.get().getDocuments();
//			String facebookId = null;
//			for (DocumentSnapshot document : accountDocuments) {
//				facebookId = document.getId();
//			}
//
//			String docLessonMember = lessonOld + facebookId;
//			DocumentReference docRefLessonMember = db.collection("LessonMember").document(docLessonMember);
//			ApiFuture<DocumentSnapshot> futureLessonMember = docRefLessonMember.get();
//			DocumentSnapshot documentLessonMember = futureLessonMember.get();
//			if (documentLessonMember.exists()) {
//				LOGGER.info("check status old lesson!");
//				LessonMember lessonMember = documentLessonMember.toObject(LessonMember.class);
//				if (lessonMember.getStatus() == 0) {
//					LOGGER.info("fail next lesson");
//					return "error/error-lesson-old";
//				}
//			} else {
//				LOGGER.info("check status old lesson but document not exist!");
//				return "error/error-lesson-old";
//			}
//			if (idLesson == 2) {
//				LOGGER.info("lesson 2.");
//				Map<String, Object> data = new HashMap<>();
//				idLesson = 2;
//				data.put("lesson", idLesson);
//				data.put("memberId", facebookId);
//				data.put("memberName", "");
//				data.put("postId", "");
//				data.put("status", 0);
//				data.put("url", "");
//				data.put("uid", uid);
//				data.put("accountId", facebookId);
//				data.put("createdAt", System.currentTimeMillis() / 1000);
//				data.put("updatedAt", System.currentTimeMillis() / 1000);
//				String docId = String.valueOf(idLesson) + facebookId;
//				DocumentReference docRefLesson = db.collection("LessonMember").document(docId);
//				ApiFuture<DocumentSnapshot> futureLesson = docRefLesson.get();
//				DocumentSnapshot documentLesson = futureLesson.get();
//				if (documentLesson.exists()) {
//					LOGGER.info("document LessonMember exist!");
//				} else {
//					LOGGER.info("create doccument lesson2!");
//					ApiFuture<WriteResult> addedDocRef = db.collection("LessonMember").document(docId).set(data);
//				}
//			}
//			// create new next lesson
//			Map<String, Object> data = new HashMap<>();
//			idLesson = idLesson + 1;
//			data.put("lesson", idLesson);
//			data.put("memberId", facebookId);
//			data.put("memberName", "");
//			data.put("postId", "");
//			data.put("status", 0);
//			data.put("url", "");
//			data.put("uid", uid);
//			data.put("accountId", facebookId);
//			data.put("createdAt", System.currentTimeMillis() / 1000);
//			data.put("updatedAt", System.currentTimeMillis() / 1000);
//			String docId = String.valueOf(idLesson) + facebookId;
//			DocumentReference docRefLesson = db.collection("LessonMember").document(docId);
//			ApiFuture<DocumentSnapshot> futureLesson = docRefLesson.get();
//			DocumentSnapshot documentLesson = futureLesson.get();
//			if (documentLesson.exists()) {
//				LOGGER.info("document LessonMember exist!");
//			} else {
//				ApiFuture<WriteResult> addedDocRef = db.collection("LessonMember").document(docId).set(data);
//			}
//		}
//		if (idLesson >= 25) {
//			LOGGER.info("lesson > 24");
//			return "error/error-lesson";
//		}
//		// get lesson by lesson number
//		for (Lesson lesson : lessonList) {
//			if (lesson.getName().equals(id)) {
//				model.addAttribute("lesson", lesson);
//			}
//		}
//
//		return "lesson/lesson";
//	}

//	@RequestMapping(value = "checkVideo", method = RequestMethod.POST)
//	public void checkVideo(Model model, @RequestParam String url,
//			@CookieValue(value = "uid", required = true) String uid, @RequestParam String numLesson,
//			HttpServletResponse response){
//		
//		
//	}

	@RequestMapping(value = "checkVideo", method = RequestMethod.POST)
	public void checkVideo(Model model, @RequestParam String url,
			@CookieValue(value = "idToken", required = true) String idToken, @RequestParam String numLesson,
			HttpServletResponse response)
			throws IOException, InterruptedException, ExecutionException, ServletException, FirebaseAuthException {
		LOGGER.info("url video check lesson: " + url);
		LOGGER.info("idToken: " + idToken);
		if (idToken == null) {
			LOGGER.info("check idToken null.");
			response.setStatus(401);
			return;
		}

		FirebaseToken decodedToken = null;
		try {
			decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
		} catch (FirebaseAuthException e) {
			// TODO Auto-generated catch block
			LOGGER.info("catch, verify idToken!");
			response.setStatus(401);
			return;
		}
		String uid = decodedToken.getUid();
//		Firestore db = FirestoreOptions.getDefaultInstance().getService();
		Firestore db = FirestoreClient.getFirestore();
		Document doc = Jsoup.connect(url).get();
//		String lessonHashtag = doc.select(".bo .bt").text();
		String lessonHashtag = doc.select("meta[property=\"og:description\"]").attr("content");
		System.out.println("lessonHashtag: " + lessonHashtag);
		int lessonCheckNow = 0;
		// check lesson by DK
		if (lessonHashtag.toLowerCase().contains("les")) {
			Pattern p = Pattern.compile("(\\d+)");
			Matcher m = p.matcher(lessonHashtag.toLowerCase());
			Boolean check = m.find();
			LOGGER.info("checkVideo Check m find: " + check);
			if (check) {
				lessonCheckNow = Integer.parseInt(m.group());
			}
//			if (m.find()) {
//				lessonCheckNow = Integer.parseInt(m.group());
//			}
		}
		LOGGER.info("Find lesson number: " + lessonCheckNow);
		if (lessonCheckNow == 0) {
            response.setStatus(404);
            return;
        }
		int begin = doc.html().indexOf("content_owner_id_new&quot;:&quot;")
				+ "content_owner_id_new&quot;:&quot;".length();
		int end = doc.html().indexOf("&quot;", begin);
		String memberId = doc.html().substring(begin, end);
		LOGGER.info("memberId: " + memberId);
		String memberName = doc.select("meta[property=\"og:title\"]").attr("content");
		String urlContent = doc.select("meta[property=\"og:url\"]").attr("content");
		int last = urlContent.lastIndexOf("=");
		String postId = urlContent.substring(last + 1);
		LOGGER.info("postId: " + postId);
		LOGGER.info("memberName: " + memberName);

		// get account by uid
		ApiFuture<QuerySnapshot> futureAcc = db.collection("Account").whereEqualTo("uid", uid).get();
		List<QueryDocumentSnapshot> accDocuments = futureAcc.get().getDocuments();
		String facebookId = null;
		for (DocumentSnapshot document : accDocuments) {
			facebookId = document.getId();
		}
		LOGGER.info("checkVideo facebookId: " + facebookId);
		if (facebookId == null) {
			LOGGER.info("facebookId null");
			return;
		}
		// check name account
		String docAccount = facebookId;
		DocumentReference docRefAccount = db.collection("Account").document(docAccount);
		ApiFuture<DocumentSnapshot> futureAccount = docRefAccount.get();
		DocumentSnapshot document = futureAccount.get();
		Account account = document.toObject(Account.class);
		LOGGER.info("display name: " + account.getDisplayName());
//		if (!account.getUid().equals(uid)) {
//			LOGGER.info("An cap bai viet cua nguoi khac.");
//			response.setStatus(405);
//			return;
//		} else {
		LOGGER.info("lessonNumber :" + numLesson);
		LOGGER.info("parse lessonNumber :" + numLesson);
		if (Integer.parseInt(numLesson) == lessonCheckNow) {
			String docLessonMember = numLesson + facebookId;
			DocumentReference docRefLessonMember = db.collection("LessonMember").document(docLessonMember);

			// check url lesson old
			ApiFuture<QuerySnapshot> futureLesson = db.collection("LessonMember").whereEqualTo("accountId", facebookId)
					.whereEqualTo("url", url).get();
			List<QueryDocumentSnapshot> documents = futureLesson.get().getDocuments();
			if (!documents.isEmpty()) {
				LOGGER.info("url lesson exits, break.");
				response.setStatus(400);
				return;
			}
			// end check url

			Map<String, Object> updatesLesson = new HashMap<>();
			updatesLesson.put("memberId", memberId);
			updatesLesson.put("memberName", memberName);
			updatesLesson.put("postId", postId);
			updatesLesson.put("status", 1);
			updatesLesson.put("url", url);
			updatesLesson.put("updatedAt", System.currentTimeMillis() / 1000);
			ApiFuture<WriteResult> futureLessonMember = docRefLessonMember.update(updatesLesson);
			// update Account

			if (document.exists()) {
				// Update an existing document
				LOGGER.info("update memberId in account");
				ApiFuture<WriteResult> future = docRefAccount.update("memberId", memberId);
			} else {
				System.out.println("No such document account!");
			}
			lessonCheckNow = lessonCheckNow + 1;
			String uriReturn = "/lesson/" + String.valueOf(lessonCheckNow);
			response.getWriter().print(String.valueOf(uriReturn));
			response.setStatus(200);
			return;
		} else {
			response.setStatus(404);
			return;
		}
//		}
	}

	@RequestMapping(value = "checkVow", method = RequestMethod.POST)
	public void checkVow(Model model, @CookieValue(value = "idToken", required = true) String idToken, @RequestParam String lesson, @RequestParam String contentVow,
						   HttpServletResponse response) throws ExecutionException, InterruptedException, IOException {
//	public void checkVow(Model model, @RequestParam String uid , @RequestParam String lesson, @RequestParam String contentVow,
//						 HttpServletResponse response) throws ExecutionException, InterruptedException, IOException {
	    LOGGER.info("start checkVow lesson: " + lesson);
		LOGGER.info("start checkVow contentVow: " + contentVow);
		if(!contentVow.equals("Tôi xin thề tôi đã làm bài này rồi. Nếu sai tôi là chó")) {
			LOGGER.info("error contentVow.");
			return;
		}
		if (Integer.parseInt(lesson) < 0 || Integer.parseInt(lesson) > 23) {
			LOGGER.info("error lesson number.");
			return;
		}
		LOGGER.info("idToken: " + idToken);
		if (idToken == null) {
			LOGGER.info("check idToken null.");
			response.setStatus(401);
			return;
		}
		FirebaseToken decodedToken = null;
		try {
			decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
		} catch (FirebaseAuthException e) {
			// TODO Auto-generated catch block
			LOGGER.info("catch, verify idToken!");
			response.setStatus(401);
			return;
		}
		String uid = decodedToken.getUid();
		LOGGER.info("checkVow uid: " + uid);
        // get account by uid
        Firestore db = FirestoreClient.getFirestore();
        ApiFuture<QuerySnapshot> futureAcc = db.collection("Account").whereEqualTo("uid", uid).get();
        List<QueryDocumentSnapshot> accDocuments = futureAcc.get().getDocuments();
        LOGGER.info("size: " + accDocuments.size());
        String facebookId = null;
        for (DocumentSnapshot document : accDocuments) {
            facebookId = document.getId();
        }
        LOGGER.info("checkVow facebookId: " + facebookId);
        if (facebookId == null) {
            LOGGER.info("facebookId null");
            response.setStatus(404);
            return;
        }
        String docAccount = facebookId;
        DocumentReference docRefAccount = db.collection("Account").document(docAccount);
        ApiFuture<DocumentSnapshot> futureAccount = docRefAccount.get();
        DocumentSnapshot document = futureAccount.get();
        Account account = document.toObject(Account.class);

        String docIdLesson = lesson + facebookId;
        LOGGER.info("docIdLesson: " + docIdLesson);

        DocumentReference docRefLessonMember = db.collection("LessonMember").document(docIdLesson);
        ApiFuture<DocumentSnapshot> future = docRefLessonMember.get();
        DocumentSnapshot documentLm = future.get();
        if (document.exists()) {
            System.out.println("Document data exist.");
            Map<String, Object> updatesLesson = new HashMap<>();
            updatesLesson.put("memberId", account.getMemberId());
            updatesLesson.put("memberName", account.getDisplayName());
            updatesLesson.put("postId", "");
            updatesLesson.put("status", 1);
            updatesLesson.put("url", "");
			updatesLesson.put("uid", uid);
            updatesLesson.put("updatedAt", System.currentTimeMillis() / 1000);
            ApiFuture<WriteResult> futureLessonMember = docRefLessonMember.update(updatesLesson);
            LOGGER.info("update lesson done");
        } else {
            System.out.println("No such document, add new set status 1.");
            Map<String, Object> newLesson = new HashMap<>();
			newLesson.put("accountId", account.getId());
			newLesson.put("lesson", Integer.parseInt(lesson));
            newLesson.put("memberId", account.getMemberId());
            newLesson.put("memberName", account.getDisplayName());
            newLesson.put("postId", "");
            newLesson.put("status", 1);
            newLesson.put("url", "");
			newLesson.put("uid", uid);
			newLesson.put("createdAt", System.currentTimeMillis() / 1000);
            newLesson.put("updatedAt", System.currentTimeMillis() / 1000);
//            ApiFuture<WriteResult> futureLessonMember = docRefLessonMember.set(newLesson);
			ApiFuture<WriteResult> addedDocRef = db.collection("LessonMember").document(docIdLesson).set(newLesson);
            LOGGER.info("set new lesson status 1 done");
        }


        // create new next lesson

        int idLesson = Integer.parseInt(lesson);
		LOGGER.info("idLesson: " + idLesson);
		idLesson = idLesson + 1;
//		Map<String, Object> data = new HashMap<>();
//        data.put("lesson", idLesson);
//        data.put("memberId", facebookId);
//        data.put("memberName", account.getDisplayName());
//        data.put("postId", "");
//        data.put("status", 0);
//        data.put("url", "");
//        data.put("uid", uid);
//        data.put("accountId", facebookId);
//        data.put("createdAt", System.currentTimeMillis() / 1000);
//        data.put("updatedAt", System.currentTimeMillis() / 1000);
//        String docId = String.valueOf(idLesson) + facebookId;
//        DocumentReference docRefLesson = db.collection("LessonMember").document(docId);
//        ApiFuture<DocumentSnapshot> futureLesson = docRefLesson.get();
//        DocumentSnapshot documentLesson = futureLesson.get();
//        if (documentLesson.exists()) {
//            LOGGER.info("document LessonMember exist!");
//        } else {
//            ApiFuture<WriteResult> addedDocRef = db.collection("LessonMember").document(docId).set(data);
//        }

        response.getWriter().print(idLesson);
        response.setStatus(200);
        return;
	}

	@RequestMapping(value = "/events/altp", method = RequestMethod.GET)
	public String evtAltp(Model model, HttpServletRequest request)
			throws FirebaseAuthException, InterruptedException, ExecutionException, UnsupportedEncodingException {

		Cookie[] cookies = request.getCookies();
		String idToken = null;
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				LOGGER.info("cookie: " + cookie);
				if (cookie.getName().equals("idToken")) {
					// do something
//					idToken = cookie.getValue();
					idToken = URLDecoder.decode(cookie.getValue(), "ASCII");
				}
			}
		}

//		LOGGER.info("idToken first: " + idToken);
//		String name = "cookieIdToken"; 
//		Cookie cookie = new Cookie(name, URLEncoder.encode(idToken, "ASCII"));
//		idToken = URLDecoder.decode(cookie.getValue(), "ASCII");

		LOGGER.info("idToken last: " + idToken);
		List<HashMap<String, String>> listLessonActive = new ArrayList<>();
		List<HashMap<String, String>> listLessonLock = new ArrayList<>();
		if (idToken == null) {
//			LOGGER.info("check idToken null.");
//			return "check-idToken/check-token-events";
			HashMap<String, String> hashMapActive = new HashMap();
			String nameLessonActive = "Lesson " + String.valueOf(1);
			hashMapActive.put("nameLesson", nameLessonActive);
			hashMapActive.put("keyLesson", String.valueOf(1));
			LOGGER.info("nameLesson: " + nameLessonActive);
			LOGGER.info("keyLesson: " + String.valueOf(1));
			listLessonActive.add(hashMapActive);

			for (Lesson lesson : lessonList) {
				if (Integer.parseInt(lesson.getName()) > 1) {
					HashMap<String, String> hashMapLock = new HashMap();
					hashMapLock.put("keyLesson", lesson.getName());
					LOGGER.info("keyLesson: " + lesson.getName());
					String nameLessonLock = "Lesson " + lesson.getName();
					hashMapLock.put("nameLesson", nameLessonLock);
					LOGGER.info("nameLesson: " + nameLessonLock);
					listLessonLock.add(hashMapLock);
				}
			}
		} else {
//			FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
			FirebaseToken decodedToken = null;
			try {
				decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
			} catch (FirebaseAuthException e) {
				// TODO Auto-generated catch block
				LOGGER.info("catch, verify idToken!");
				return "check-idToken/check-token";
			}

			String uid = decodedToken.getUid();
//			Firestore db = FirestoreOptions.getDefaultInstance().getService();
			Firestore db = FirestoreClient.getFirestore();
			// get account id
			ApiFuture<QuerySnapshot> future = db.collection("Account").whereEqualTo("uid", uid).get();
			List<QueryDocumentSnapshot> documents = future.get().getDocuments();
			String accountId = null;
			for (DocumentSnapshot document : documents) {
				accountId = document.getId();
			}
			LOGGER.info("account Id: " + accountId);
			// get list lesson learned
			ApiFuture<QuerySnapshot> futureLesson = db.collection("LessonMember").whereEqualTo("accountId", accountId)
					.whereEqualTo("status", 1).get();
			List<QueryDocumentSnapshot> lessonDocuments = futureLesson.get().getDocuments();
			int numLessons = lessonDocuments.size();
			LOGGER.info("numLessons: " + numLessons);

//			listLessonActive = new ArrayList<>();
			LessonMember lessonMember = new LessonMember();
			// get account by uid
			ApiFuture<QuerySnapshot> futureAcc = db.collection("Account").whereEqualTo("uid", uid).get();
			List<QueryDocumentSnapshot> accDocuments = futureAcc.get().getDocuments();
			String facebookId = null;
			for (DocumentSnapshot docAcc : accDocuments) {
				facebookId = docAcc.getId();
			}
			if (numLessons == 0) {
				HashMap<String, String> hashMap = new HashMap();
				String nameLesson = "Lesson " + String.valueOf(1);
				hashMap.put("nameLesson", nameLesson);
				hashMap.put("keyLesson", String.valueOf(1));
				LOGGER.info("nameLesson: " + nameLesson);
				LOGGER.info("keyLesson: " + String.valueOf(1));
				listLessonActive.add(hashMap);
			} else {
				for (DocumentSnapshot document : lessonDocuments) {
					lessonMember = document.toObject(LessonMember.class);
					HashMap<String, String> hashMap = new HashMap();
					String nameLesson = "Lesson " + String.valueOf(lessonMember.getLesson());
					hashMap.put("nameLesson", nameLesson);
					hashMap.put("keyLesson", String.valueOf(lessonMember.getLesson()));
					LOGGER.info("nameLesson: " + nameLesson);
					LOGGER.info("keyLesson: " + String.valueOf(lessonMember.getLesson()));
					listLessonActive.add(hashMap);
				}
				LOGGER.info("LESSOONNNN: " + lessonMember.getLesson());
				if (lessonMember.getLesson() < 24) {
					String docNextLesson = String.valueOf(numLessons + 1) + facebookId;
					DocumentReference docRef = db.collection("LessonMember").document(docNextLesson);
					ApiFuture<DocumentSnapshot> futureCheck = docRef.get();
					DocumentSnapshot document = futureCheck.get();
					if (document.exists()) {
						LOGGER.info("doc Lesson Check exist!");
					} else {

						Map<String, Object> data = new HashMap<>();
						data.put("lesson", numLessons + 1);
						data.put("memberId", facebookId);
						data.put("memberName", "");
						data.put("postId", "");
						data.put("status", 0);
						data.put("url", "");
						data.put("uid", uid);
						data.put("accountId", facebookId);
						data.put("createdAt", System.currentTimeMillis() / 1000);
						data.put("updatedAt", System.currentTimeMillis() / 1000);
						ApiFuture<WriteResult> addedDocRef = db.collection("LessonMember").document(docNextLesson)
								.set(data);
					}

					HashMap<String, String> hashMapNext = new HashMap();
					LOGGER.info("LESSOONNNN NEXTTTT: " + String.valueOf(numLessons + 1));
					String nameLessonNext = "Lesson " + String.valueOf(numLessons + 1);
					hashMapNext.put("nameLesson", nameLessonNext);
					hashMapNext.put("keyLesson", String.valueOf(numLessons + 1));
					LOGGER.info("nameLesson: " + nameLessonNext);
					LOGGER.info("keyLesson: " + String.valueOf(numLessons + 1));
					listLessonActive.add(hashMapNext);
				}
			}

			// get list lesson
//			listLessonLock = new ArrayList<>();
			numLessons = numLessons + 1;
			for (Lesson lesson : lessonList) {
				if (Integer.parseInt(lesson.getName()) > numLessons) {
					HashMap<String, String> hashMap = new HashMap();
					hashMap.put("keyLesson", lesson.getName());
					LOGGER.info("keyLesson: " + lesson.getName());
					String nameLesson = "Lesson " + lesson.getName();
					hashMap.put("nameLesson", nameLesson);
					LOGGER.info("nameLesson: " + nameLesson);
					listLessonLock.add(hashMap);
				}
			}
		}

		model.addAttribute("activeLessons", listLessonActive);
		model.addAttribute("lockLessons", listLessonLock);
		return "lesson/event-altp";
	}

	@RequestMapping(value = "/openLockLesson", method = RequestMethod.POST)
	public void openLockLesson(Model model, @RequestParam String url,
			@CookieValue(value = "idToken", required = true) String idToken, @RequestParam String numLesson,
			HttpServletResponse response)
			throws IOException, InterruptedException, ExecutionException, ServletException, FirebaseAuthException {
		LOGGER.info("url video open lesson: " + url);
//		FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
		LOGGER.info("idToken: " + idToken);
		if (idToken == null) {
			LOGGER.info("check idToken null.");
			response.setStatus(401);
			return;
		}

		FirebaseToken decodedToken = null;
		try {
			decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
		} catch (FirebaseAuthException e) {
			// TODO Auto-generated catch block
			LOGGER.info("catch, verify idToken!");
			response.setStatus(401);
			return;
		}
		String uid = decodedToken.getUid();
//		Firestore db = FirestoreOptions.getDefaultInstance().getService();
		Firestore db = FirestoreClient.getFirestore();
		// get account by uid
		ApiFuture<QuerySnapshot> futureAcc = db.collection("Account").whereEqualTo("uid", uid).get();
		List<QueryDocumentSnapshot> accDocuments = futureAcc.get().getDocuments();
		String facebookId = null;
		for (DocumentSnapshot document : accDocuments) {
			facebookId = document.getId();
		}
		LOGGER.info("openLockLesson facebookId: " + facebookId);
		if (facebookId == null) {
			LOGGER.info("facebookId null");
			return;
		}
		// check exist last lesson
		String docLast = numLesson + facebookId;
		LOGGER.info("docLast: " + docLast);
		DocumentReference docRefLast = db.collection("LessonMember").document(docLast);
		ApiFuture<DocumentSnapshot> futureLast = docRefLast.get();
		DocumentSnapshot documentLast = futureLast.get();
		if (!documentLast.exists()) {
			LOGGER.info("last day not exist.");
			response.setStatus(406);
			return;
		}
		// end check

		Document doc = Jsoup.connect(url).get();
//		String lessonHashtag = doc.select(".bo .bt").text();
		String lessonHashtag = doc.select("meta[property=\"og:description\"]").attr("content");
		LOGGER.info("lessonHashtag: " + lessonHashtag);
		int lessonCheckNow = 0;
		// check lesson by DK
		if (lessonHashtag.toLowerCase().contains("les")) {
			Pattern p = Pattern.compile("(\\d+)");
			Matcher m = p.matcher(lessonHashtag.toLowerCase());
			Boolean check = m.find();
			System.out.println("checkVideo Check m find: " + check);
			if (check) {
				lessonCheckNow = Integer.parseInt(m.group());
			}
//			if (m.find()) {
//				lessonCheckNow = Integer.parseInt(m.group());
//			}
		}
		LOGGER.info("Find lesson number: " + lessonCheckNow);
		int begin = doc.html().indexOf("content_owner_id_new&quot;:&quot;")
				+ "content_owner_id_new&quot;:&quot;".length();
		int end = doc.html().indexOf("&quot;", begin);
		String memberId = doc.html().substring(begin, end);
		LOGGER.info("memberId: " + memberId);
		String memberName = doc.select("meta[property=\"og:title\"]").attr("content");
		String urlContent = doc.select("meta[property=\"og:url\"]").attr("content");
		int last = urlContent.lastIndexOf("=");
		String postId = urlContent.substring(last + 1);
		LOGGER.info("postId: " + postId);
		LOGGER.info("memberName: " + memberName);

		// check name account
		String docAccount = facebookId;
		DocumentReference docRefAccount = db.collection("Account").document(docAccount);
		ApiFuture<DocumentSnapshot> futureAccount = docRefAccount.get();
		DocumentSnapshot document = futureAccount.get();
		Account account = document.toObject(Account.class);
//		if (!account.getDisplayName().equals(memberName)) {
//			LOGGER.info("An cap bai viet cua nguoi khac.");
//			response.setStatus(405);
//			return;
//		} else {
		LOGGER.info("lessonNumber :" + numLesson);
		if (Integer.parseInt(numLesson) == lessonCheckNow) {
			String docLessonMember = numLesson + facebookId;
			DocumentReference docRefLessonMember = db.collection("LessonMember").document(docLessonMember);
			// check document Lesson moment
			ApiFuture<DocumentSnapshot> futureLessMem = docRefLessonMember.get();
			DocumentSnapshot documentLessMem = futureLessMem.get();
			if (!documentLessMem.exists()) {
				Map<String, Object> data = new HashMap<>();
				data.put("lesson", lessonCheckNow);
				data.put("memberId", facebookId);
				data.put("memberName", "");
				data.put("postId", "");
				data.put("status", 0);
				data.put("url", "");
				data.put("uid", uid);
				data.put("accountId", facebookId);
				data.put("createdAt", System.currentTimeMillis() / 1000);
				data.put("updatedAt", System.currentTimeMillis() / 1000);
				ApiFuture<WriteResult> addedDocRef = db.collection("LessonMember").document(docLessonMember).set(data);
			}

			// check url lesson old
			ApiFuture<QuerySnapshot> futureLesson = db.collection("LessonMember").whereEqualTo("accountId", facebookId)
					.whereEqualTo("url", url).get();
			List<QueryDocumentSnapshot> documents = futureLesson.get().getDocuments();
			if (!documents.isEmpty()) {
				LOGGER.info("url lesson exits, break.");
				response.setStatus(400);
				return;
			}
			// end check url

			Map<String, Object> updatesLesson = new HashMap<>();
			updatesLesson.put("memberId", memberId);
			updatesLesson.put("memberName", memberName);
			updatesLesson.put("postId", postId);
			updatesLesson.put("status", 1);
			updatesLesson.put("url", url);
			updatesLesson.put("updatedAt", System.currentTimeMillis() / 1000);
			ApiFuture<WriteResult> futureLessonMember = docRefLessonMember.update(updatesLesson);
			// update Account

			if (document.exists()) {
				// Update an existing document
				LOGGER.info("update memberId in account");
				ApiFuture<WriteResult> future = docRefAccount.update("memberId", memberId);
			} else {
				System.out.println("No such document account!");
			}
			lessonCheckNow = lessonCheckNow + 1;
			// create new lesson
			String docLessonNew = lessonCheckNow + facebookId;
			if (lessonCheckNow < 25) {
				LOGGER.info("docLessonNew: " + docLessonNew);
				// check doc LessonNew
				DocumentReference docRefLess = db.collection("LessonMember").document(docLessonNew);
				ApiFuture<DocumentSnapshot> futureLess = docRefLess.get();
				DocumentSnapshot documentLess = futureLess.get();
				if (documentLess.exists()) {
					LOGGER.info("docLessonNew exist!");
				} else {
					LOGGER.info("creat new docLessonNew!");
					Map<String, Object> data = new HashMap<>();
					data.put("lesson", lessonCheckNow);
					data.put("memberId", facebookId);
					data.put("memberName", "");
					data.put("postId", "");
					data.put("status", 0);
					data.put("url", "");
					data.put("uid", uid);
					data.put("accountId", facebookId);
					data.put("createdAt", System.currentTimeMillis() / 1000);
					data.put("updatedAt", System.currentTimeMillis() / 1000);
					ApiFuture<WriteResult> addedDocRef = db.collection("LessonMember").document(docLessonNew).set(data);
				}
				// end create new lesson
			}
			String uriReturn = "/lesson/" + String.valueOf(lessonCheckNow);
			response.getWriter().print(String.valueOf(uriReturn));
			response.setStatus(200);
			return;
		} else {
			response.setStatus(404);
			return;
		}
//		}
	}
}
