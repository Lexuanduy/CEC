package fcs.cec.opencec.server;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.cloud.FirestoreClient;

import fcs.cec.opencec.entity.Account;
import fcs.cec.opencec.entity.Journey;
import fcs.cec.opencec.entity.JourneyDay;
import fcs.cec.opencec.entity.Lesson;

@Controller
public class JourneyController {
	private static final Logger LOGGER = LoggerFactory.getLogger(JourneyController.class);
	static ArrayList<Journey> dayList = new ArrayList<Journey>();
	static {
		Document doc = null;
		String url = "https://script.googleusercontent.com/macros/echo?user_content_key=dmTT0L5ltyjs6C0mzfB8Kf1FkNPCAqiExMVyEY7hPKS9QIrht-BvnAzuAZYIZvlrnSaC13gWKjpsPFnfMb3lT9L5wfimIUbgm5_BxDlH2jW0nuo2oDemN9CCS2h10ox_1xSncGQajx_ryfhECjZEnCT0QRJ7P_-LtV3tAd8_b_dUnbO1rEvbeLLB2eAoIGhp1hENMaacOI9TktsviLkDHJlUq1JAmpDs&lib=MmSKrXssQcdpiSXxZX7nm1QZVzjmXS3D2";
		try {
			doc = Jsoup.connect(url).timeout(30000).get();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		Elements elements = doc.select("day");
		Element ePdf = null;
		Element videoMp4 = null;
		String day = null;
		String pdf = null;
		String video = null;
		int indexStartJourneyDay = 0;
		int indexText1 = 0;
		int indexText2 = 0;
		String journeyName = null;
		Journey journey = null;
		String text = null;
		String uri = null;
		for (Element element : elements) {
			day = element.attr("name");
			ePdf = element.child(0);
			pdf = ePdf.text();
			videoMp4 = element.child(1);
			video = videoMp4.text();
			indexStartJourneyDay = pdf.indexOf("opencec.appspot.com/");
			text = pdf.substring(indexStartJourneyDay);
			indexText1 = text.indexOf("/") + 1;
			uri = text.substring(indexText1);
			indexText2 = uri.indexOf("/");
			journeyName = uri.substring(0, indexText2);
			System.out.println(journeyName);
			System.out.println(day);
			journey = new Journey(journeyName, day, pdf, video);
			dayList.add(journey);
		}
	}

	@RequestMapping(value = "journey/{name}/{day}", method = RequestMethod.GET)
	public String journeyDays(Model model, @PathVariable("name") String name, @PathVariable("day") String day,
			@CookieValue(value = "idToken", required = false) String idToken)
			throws FirebaseAuthException, InterruptedException, ExecutionException {
//		Firestore db = FirestoreClient.getFirestore();
		Firestore db = FirestoreClient.getFirestore();
//		FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
//		String uid = decodedToken.getUid();
		int dayJourney = Integer.parseInt(day);
		LOGGER.info("dayJourney first: " + dayJourney);
		// journey day in 3days
		if (name.equals("3days")) {
			if (Integer.parseInt(day) < 1) {
				LOGGER.info("3days day < 1");
				return "error/error-journey";
			}
			if ((Integer.parseInt(day) > 1) && (Integer.parseInt(day) < 4)) {
				// get facebookId, get uid
				LOGGER.info("idToken: " + idToken);
				if (idToken == null) {
					LOGGER.info("check idToken null.");
					List<HashMap<String, String>> listMap = new ArrayList<>();
					HashMap<String, String> hashMap = new HashMap();
					String urlJourneyDay = "/journey/3days/" + day;
					hashMap.put("urlJourneyDay", urlJourneyDay);
					listMap.add(hashMap);
					model.addAttribute("journeyDay", listMap);
					return "check-idToken/check-token-journey";
				}
				FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
				String uid = decodedToken.getUid();
				// get doc id account
				ApiFuture<QuerySnapshot> futureAccount = db.collection("Account").whereEqualTo("uid", uid).get();
				List<QueryDocumentSnapshot> accountDocuments = futureAccount.get().getDocuments();
				String facebookId = null;
				for (DocumentSnapshot document : accountDocuments) {
					facebookId = document.getId();
				}

				// create day 2 in journey day 3days
				if (Integer.parseInt(day) == 2) {
					LOGGER.info("3days day == 2");
					// create new journey day
					Map<String, Object> data = new HashMap<>();
					dayJourney = 2;
					data.put("day", dayJourney);
					data.put("journey", "3days");
					data.put("memberId", facebookId);
					data.put("memberName", "");
					data.put("postId", "");
					data.put("status", 0);
					data.put("url", "");
					data.put("uid", uid);
					data.put("accountId", facebookId);
					data.put("createdAt", System.currentTimeMillis() / 1000);
					data.put("updatedAt", System.currentTimeMillis() / 1000);
					String docId = name + String.valueOf(dayJourney) + facebookId;
					DocumentReference docRefJourneyDay = db.collection("JourneyDay").document(docId);
					ApiFuture<DocumentSnapshot> futureJourneyDay = docRefJourneyDay.get();
					DocumentSnapshot documentJourneyDay = futureJourneyDay.get();
					if (documentJourneyDay.exists()) {
						LOGGER.info("document JourneyDay eixst!");
					} else {
						ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay").document(docId).set(data);
					}
				}

				int dayJourneyOld = dayJourney - 1;
				String docJourneyDay = name + dayJourneyOld + facebookId;
				LOGGER.info("doc journey day: " + docJourneyDay);
				DocumentReference docRef = db.collection("JourneyDay").document(docJourneyDay);
				ApiFuture<DocumentSnapshot> future = docRef.get();
				DocumentSnapshot document = future.get();
				JourneyDay journeyDay = null;
				if (document.exists()) {
					journeyDay = document.toObject(JourneyDay.class);
					int status = journeyDay.getStatus();
					if (status == 0) {
						LOGGER.info("status == 0");
						return "error/error-journey-old";
					}
					// create new journey day
					Map<String, Object> data = new HashMap<>();
					dayJourney = Integer.parseInt(day) + 1;
					LOGGER.info("dayJourney CHECK: " + dayJourney);
					// check day < 4 in 3days
					if (dayJourney < 4) {
						if (dayJourney == 3) {
							data.put("day", 1);
							data.put("journey", "5days");
							data.put("memberId", facebookId);
							data.put("memberName", "");
							data.put("postId", "");
							data.put("status", 0);
							data.put("url", "");
							data.put("uid", uid);
							data.put("accountId", facebookId);
							data.put("createdAt", System.currentTimeMillis() / 1000);
							data.put("updatedAt", System.currentTimeMillis() / 1000);
							String docId = "5days1" + facebookId;
							DocumentReference docRefJourneyDay = db.collection("JourneyDay").document(docId);
							ApiFuture<DocumentSnapshot> futureJourneyDay = docRefJourneyDay.get();
							DocumentSnapshot documentJourneyDay = futureJourneyDay.get();
							if (documentJourneyDay.exists()) {
								LOGGER.info("document JourneyDay eixst!");
							} else {
								ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay").document(docId)
										.set(data);
							}
						}
						data.put("day", dayJourney);
						data.put("journey", "3days");
						data.put("memberId", facebookId);
						data.put("memberName", "");
						data.put("postId", "");
						data.put("status", 0);
						data.put("url", "");
						data.put("uid", uid);
						data.put("accountId", facebookId);
						data.put("createdAt", System.currentTimeMillis() / 1000);
						data.put("updatedAt", System.currentTimeMillis() / 1000);
						String docId = name + String.valueOf(dayJourney) + facebookId;
						DocumentReference docRefJourneyDay = db.collection("JourneyDay").document(docId);
						ApiFuture<DocumentSnapshot> futureJourneyDay = docRefJourneyDay.get();
						DocumentSnapshot documentJourneyDay = futureJourneyDay.get();
						if (documentJourneyDay.exists()) {
							LOGGER.info("document JourneyDay eixst!");
						} else {
							ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay").document(docId).set(data);
						}
					}
				} else {
					LOGGER.info("No such document JourneyDay!");
					return "error/error-journey";
				}
			}
			if (Integer.parseInt(day) >= 4) {
				LOGGER.info("3days day >= 4");
				return "error/error-journey";
			}
		}
		// end journey day in 3days

		// journey day in 5days
		if (name.equals("5days")) {
			// get facebookId, get uid
			if (idToken == null) {
				LOGGER.info("check idToken null.");
				List<HashMap<String, String>> listMap = new ArrayList<>();
				HashMap<String, String> hashMap = new HashMap();
				String urlJourneyDay = "/journey/5days/" + day;
				hashMap.put("urlJourneyDay", urlJourneyDay);
				listMap.add(hashMap);
				model.addAttribute("journeyDay", listMap);
				return "check-idToken/check-token-journey";
			}
			FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
			String uid = decodedToken.getUid();
			// get doc id account
			ApiFuture<QuerySnapshot> futureAccount = db.collection("Account").whereEqualTo("uid", uid).get();
			List<QueryDocumentSnapshot> accountDocuments = futureAccount.get().getDocuments();
			String facebookId = null;
			for (DocumentSnapshot document : accountDocuments) {
				facebookId = document.getId();
			}

			if (Integer.parseInt(day) < 1) {
				LOGGER.info("5days day < 1");
				return "error/error-journey";
			}
			if ((Integer.parseInt(day) > 1) && (Integer.parseInt(day) < 6)) {

				int dayJourneyOld = dayJourney - 1;
				String docJourneyDay = name + dayJourneyOld + facebookId;
				LOGGER.info("doc journey day: " + docJourneyDay);
				DocumentReference docRef = db.collection("JourneyDay").document(docJourneyDay);
				ApiFuture<DocumentSnapshot> future = docRef.get();
				DocumentSnapshot document = future.get();
				JourneyDay journeyDay = null;
				if (document.exists()) {
					journeyDay = document.toObject(JourneyDay.class);
					int status = journeyDay.getStatus();
					if (status == 0) {
						LOGGER.info("status == 0");
						return "error/error-journey-old";
					}
					// create new journey day
					Map<String, Object> data = new HashMap<>();
					dayJourney = Integer.parseInt(day) + 1;
					// check day < 6 in 5days
					if (dayJourney < 6) {
						if (dayJourney == 5) {
							data.put("day", 1);
							data.put("journey", "7days");
							data.put("memberId", facebookId);
							data.put("memberName", "");
							data.put("postId", "");
							data.put("status", 0);
							data.put("url", "");
							data.put("uid", uid);
							data.put("accountId", facebookId);
							data.put("createdAt", System.currentTimeMillis() / 1000);
							data.put("updatedAt", System.currentTimeMillis() / 1000);
							String docId = "7days1" + facebookId;
							DocumentReference docRefJourneyDay = db.collection("JourneyDay").document(docId);
							ApiFuture<DocumentSnapshot> futureJourneyDay = docRefJourneyDay.get();
							DocumentSnapshot documentJourneyDay = futureJourneyDay.get();
							if (documentJourneyDay.exists()) {
								LOGGER.info("document JourneyDay eixst!");
							} else {
								ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay").document(docId)
										.set(data);
							}
						}
						data.put("day", dayJourney);
						data.put("journey", "5days");
						data.put("memberId", facebookId);
						data.put("memberName", "");
						data.put("postId", "");
						data.put("status", 0);
						data.put("url", "");
						data.put("uid", uid);
						data.put("accountId", facebookId);
						data.put("createdAt", System.currentTimeMillis() / 1000);
						data.put("updatedAt", System.currentTimeMillis() / 1000);
						String docId = name + String.valueOf(dayJourney) + facebookId;
						DocumentReference docRefJourneyDay = db.collection("JourneyDay").document(docId);
						ApiFuture<DocumentSnapshot> futureJourneyDay = docRefJourneyDay.get();
						DocumentSnapshot documentJourneyDay = futureJourneyDay.get();
						if (documentJourneyDay.exists()) {
							LOGGER.info("document JourneyDay eixst!");
						} else {
							ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay").document(docId).set(data);
						}
					}
				} else {
					LOGGER.info("No such document JourneyDay!");
					return "error/error-journey";
				}
			}
			if (Integer.parseInt(day) >= 6) {
				LOGGER.info("5days day >= 6");
				return "error/error-journey";
			}
			if (Integer.parseInt(day) == 1) {
				LOGGER.info("5days day == 1");
				// check day 3 in journeyday 3days
				String docJourneyDay = "3days3" + facebookId;
				LOGGER.info("doc journey day: " + docJourneyDay);
				DocumentReference docRef = db.collection("JourneyDay").document(docJourneyDay);
				ApiFuture<DocumentSnapshot> future = docRef.get();
				DocumentSnapshot document = future.get();
				JourneyDay journeyDay = null;
				if (document.exists()) {
					journeyDay = document.toObject(JourneyDay.class);
					int status = journeyDay.getStatus();
					if (status == 0) {
						LOGGER.info("status == 0");
						return "error/error-journey-old";
					}
					// create new journey day
					Map<String, Object> data = new HashMap<>();
					dayJourney = Integer.parseInt(day) + 1;
					data.put("day", dayJourney);
					data.put("journey", "5days");
					data.put("memberId", facebookId);
					data.put("memberName", "");
					data.put("postId", "");
					data.put("status", 0);
					data.put("url", "");
					data.put("uid", uid);
					data.put("accountId", facebookId);
					data.put("createdAt", System.currentTimeMillis() / 1000);
					data.put("updatedAt", System.currentTimeMillis() / 1000);
					String docId = name + String.valueOf(dayJourney) + facebookId;
					DocumentReference docRefJourneyDay = db.collection("JourneyDay").document(docId);
					LOGGER.info("docId: " + docId);
					ApiFuture<DocumentSnapshot> futureJourneyDay = docRefJourneyDay.get();
					DocumentSnapshot documentJourneyDay = futureJourneyDay.get();
					if (documentJourneyDay.exists()) {
						LOGGER.info("document JourneyDay eixst!");
					} else {
						ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay").document(docId).set(data);
					}
				} else {
					LOGGER.info("No such document JourneyDay!");
					return "error/error-journey";
				}
			}
		}
		// end journey day in 5days

		// journey day in 7days
		if (name.equals("7days")) {
			// get facebookId, get uid
			if (idToken == null) {
				LOGGER.info("check idToken null.");
				List<HashMap<String, String>> listMap = new ArrayList<>();
				HashMap<String, String> hashMap = new HashMap();
				String urlJourneyDay = "/journey/7days/" + day;
				hashMap.put("urlJourneyDay", urlJourneyDay);
				listMap.add(hashMap);
				model.addAttribute("journeyDay", listMap);
				return "check-idToken/check-token-journey";
			}
			FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
			String uid = decodedToken.getUid();
			// get doc id account
			ApiFuture<QuerySnapshot> futureAccount = db.collection("Account").whereEqualTo("uid", uid).get();
			List<QueryDocumentSnapshot> accountDocuments = futureAccount.get().getDocuments();
			String facebookId = null;
			for (DocumentSnapshot document : accountDocuments) {
				facebookId = document.getId();
			}

			if (Integer.parseInt(day) < 1) {
				LOGGER.info("7days day < 1");
				return "error/error-journey";
			}
			if ((Integer.parseInt(day) > 1) && (Integer.parseInt(day) < 8)) {

				int dayJourneyOld = dayJourney - 1;
				String docJourneyDay = name + dayJourneyOld + facebookId;
				LOGGER.info("doc journey day: " + docJourneyDay);
				DocumentReference docRef = db.collection("JourneyDay").document(docJourneyDay);
				ApiFuture<DocumentSnapshot> future = docRef.get();
				DocumentSnapshot document = future.get();
				JourneyDay journeyDay = null;
				if (document.exists()) {
					journeyDay = document.toObject(JourneyDay.class);
					int status = journeyDay.getStatus();
					if (status == 0) {
						LOGGER.info("status == 0");
						return "error/error-journey-old";
					}
					// create new journey day
					Map<String, Object> data = new HashMap<>();
					dayJourney = Integer.parseInt(day) + 1;
					// check day < 8 in 7days
					if (dayJourney < 8) {
						if (dayJourney == 7) {
							data.put("day", 1);
							data.put("journey", "10days");
							data.put("memberId", facebookId);
							data.put("memberName", "");
							data.put("postId", "");
							data.put("status", 0);
							data.put("url", "");
							data.put("uid", uid);
							data.put("accountId", facebookId);
							data.put("createdAt", System.currentTimeMillis() / 1000);
							data.put("updatedAt", System.currentTimeMillis() / 1000);
							String docId = "10days1" + facebookId;
							DocumentReference docRefJourneyDay = db.collection("JourneyDay").document(docId);
							ApiFuture<DocumentSnapshot> futureJourneyDay = docRefJourneyDay.get();
							DocumentSnapshot documentJourneyDay = futureJourneyDay.get();
							if (documentJourneyDay.exists()) {
								LOGGER.info("document JourneyDay eixst!");
							} else {
								ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay").document(docId)
										.set(data);
							}
						}
						data.put("day", dayJourney);
						data.put("journey", "7days");
						data.put("memberId", facebookId);
						data.put("memberName", "");
						data.put("postId", "");
						data.put("status", 0);
						data.put("url", "");
						data.put("uid", uid);
						data.put("accountId", facebookId);
						data.put("createdAt", System.currentTimeMillis() / 1000);
						data.put("updatedAt", System.currentTimeMillis() / 1000);
						String docId = name + String.valueOf(dayJourney) + facebookId;
						DocumentReference docRefJourneyDay = db.collection("JourneyDay").document(docId);
						ApiFuture<DocumentSnapshot> futureJourneyDay = docRefJourneyDay.get();
						DocumentSnapshot documentJourneyDay = futureJourneyDay.get();
						if (documentJourneyDay.exists()) {
							LOGGER.info("document JourneyDay eixst!");
						} else {
							ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay").document(docId).set(data);
						}
					}
				} else {
					LOGGER.info("No such document JourneyDay!");
					return "error/error-journey";
				}
			}
			if (Integer.parseInt(day) >= 8) {
				LOGGER.info("7days day >= 8");
				return "error/error-journey";
			}
			if (Integer.parseInt(day) == 1) {
				LOGGER.info("7days day == 1");
				// check day 5 in journeyday 5days
				String docJourneyDay = "5days5" + facebookId;
				LOGGER.info("doc journey day: " + docJourneyDay);
				DocumentReference docRef = db.collection("JourneyDay").document(docJourneyDay);
				ApiFuture<DocumentSnapshot> future = docRef.get();
				DocumentSnapshot document = future.get();
				JourneyDay journeyDay = null;
				if (document.exists()) {
					journeyDay = document.toObject(JourneyDay.class);
					int status = journeyDay.getStatus();
					if (status == 0) {
						LOGGER.info("status == 0");
						return "error/error-journey-old";
					}
					// create new journey day
					Map<String, Object> data = new HashMap<>();
					dayJourney = Integer.parseInt(day) + 1;
					data.put("day", dayJourney);
					data.put("journey", "7days");
					data.put("memberId", facebookId);
					data.put("memberName", "");
					data.put("postId", "");
					data.put("status", 0);
					data.put("url", "");
					data.put("uid", uid);
					data.put("accountId", facebookId);
					data.put("createdAt", System.currentTimeMillis() / 1000);
					data.put("updatedAt", System.currentTimeMillis() / 1000);
					String docId = name + String.valueOf(dayJourney) + facebookId;
					DocumentReference docRefJourneyDay = db.collection("JourneyDay").document(docId);
					ApiFuture<DocumentSnapshot> futureJourneyDay = docRefJourneyDay.get();
					DocumentSnapshot documentJourneyDay = futureJourneyDay.get();
					if (documentJourneyDay.exists()) {
						LOGGER.info("document JourneyDay eixst!");
					} else {
						ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay").document(docId).set(data);
					}
				} else {
					LOGGER.info("No such document JourneyDay!");
					return "error/error-journey";
				}
			}
		}
		// end journey day in 7days

		// journey day in 10days
		if (name.equals("10days")) {
			if (idToken == null) {
				LOGGER.info("check idToken null.");
				List<HashMap<String, String>> listMap = new ArrayList<>();
				HashMap<String, String> hashMap = new HashMap();
				String urlJourneyDay = "/journey/10days/" + day;
				hashMap.put("urlJourneyDay", urlJourneyDay);
				listMap.add(hashMap);
				model.addAttribute("journeyDay", listMap);
				return "check-idToken/check-token-journey";
			}
			// get facebookId, get uid
			FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
			String uid = decodedToken.getUid();
			// get doc id account
			ApiFuture<QuerySnapshot> futureAccount = db.collection("Account").whereEqualTo("uid", uid).get();
			List<QueryDocumentSnapshot> accountDocuments = futureAccount.get().getDocuments();
			String facebookId = null;
			for (DocumentSnapshot document : accountDocuments) {
				facebookId = document.getId();
			}

			if (Integer.parseInt(day) < 1) {
				LOGGER.info("10days day < 1");
				return "error/error-journey";
			}
			if ((Integer.parseInt(day) > 1) && (Integer.parseInt(day) < 11)) {

				int dayJourneyOld = dayJourney - 1;
				String docJourneyDay = name + dayJourneyOld + facebookId;
				LOGGER.info("doc journey day: " + docJourneyDay);
				DocumentReference docRef = db.collection("JourneyDay").document(docJourneyDay);
				ApiFuture<DocumentSnapshot> future = docRef.get();
				DocumentSnapshot document = future.get();
				JourneyDay journeyDay = null;
				if (document.exists()) {
					journeyDay = document.toObject(JourneyDay.class);
					int status = journeyDay.getStatus();
					if (status == 0) {
						LOGGER.info("status == 0");
						return "error/error-journey-old";
					}
					// create new journey day
					Map<String, Object> data = new HashMap<>();
					dayJourney = Integer.parseInt(day) + 1;
					// check day < 11 in 10days
					if (dayJourney < 11) {
						if (dayJourney == 10) {
							data.put("day", 1);
							data.put("journey", "21days");
							data.put("memberId", facebookId);
							data.put("memberName", "");
							data.put("postId", "");
							data.put("status", 0);
							data.put("url", "");
							data.put("uid", uid);
							data.put("accountId", facebookId);
							data.put("createdAt", System.currentTimeMillis() / 1000);
							data.put("updatedAt", System.currentTimeMillis() / 1000);
							String docId = "21days1" + facebookId;
							DocumentReference docRefJourneyDay = db.collection("JourneyDay").document(docId);
							ApiFuture<DocumentSnapshot> futureJourneyDay = docRefJourneyDay.get();
							DocumentSnapshot documentJourneyDay = futureJourneyDay.get();
							if (documentJourneyDay.exists()) {
								LOGGER.info("document JourneyDay eixst!");
							} else {
								ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay").document(docId)
										.set(data);
							}
						}
						data.put("day", dayJourney);
						data.put("journey", "10days");
						data.put("memberId", facebookId);
						data.put("memberName", "");
						data.put("postId", "");
						data.put("status", 0);
						data.put("url", "");
						data.put("uid", uid);
						data.put("accountId", facebookId);
						data.put("createdAt", System.currentTimeMillis() / 1000);
						data.put("updatedAt", System.currentTimeMillis() / 1000);
						String docId = name + String.valueOf(dayJourney) + facebookId;
						DocumentReference docRefJourneyDay = db.collection("JourneyDay").document(docId);
						ApiFuture<DocumentSnapshot> futureJourneyDay = docRefJourneyDay.get();
						DocumentSnapshot documentJourneyDay = futureJourneyDay.get();
						if (documentJourneyDay.exists()) {
							LOGGER.info("document JourneyDay eixst!");
						} else {
							ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay").document(docId).set(data);
						}
					}
				} else {
					LOGGER.info("No such document JourneyDay!");
					return "error/error-journey";
				}
			}
			if (Integer.parseInt(day) >= 11) {
				LOGGER.info("10days day >= 11");
				return "error/error-journey";
			}
			if (Integer.parseInt(day) == 1) {
				LOGGER.info("10days day == 1");
				// check day 7 in journeyday 7days
				String docJourneyDay = "7days7" + facebookId;
				LOGGER.info("doc journey day: " + docJourneyDay);
				DocumentReference docRef = db.collection("JourneyDay").document(docJourneyDay);
				ApiFuture<DocumentSnapshot> future = docRef.get();
				DocumentSnapshot document = future.get();
				JourneyDay journeyDay = null;
				if (document.exists()) {
					journeyDay = document.toObject(JourneyDay.class);
					int status = journeyDay.getStatus();
					if (status == 0) {
						LOGGER.info("status == 0");
						return "error/error-journey-old";
					}
					// create new journey day
					Map<String, Object> data = new HashMap<>();
					dayJourney = Integer.parseInt(day) + 1;
					data.put("day", dayJourney);
					data.put("journey", "10days");
					data.put("memberId", facebookId);
					data.put("memberName", "");
					data.put("postId", "");
					data.put("status", 0);
					data.put("url", "");
					data.put("uid", uid);
					data.put("accountId", facebookId);
					data.put("createdAt", System.currentTimeMillis() / 1000);
					data.put("updatedAt", System.currentTimeMillis() / 1000);
					String docId = name + String.valueOf(dayJourney) + facebookId;
					DocumentReference docRefJourneyDay = db.collection("JourneyDay").document(docId);
					ApiFuture<DocumentSnapshot> futureJourneyDay = docRefJourneyDay.get();
					DocumentSnapshot documentJourneyDay = futureJourneyDay.get();
					if (documentJourneyDay.exists()) {
						LOGGER.info("document JourneyDay eixst!");
					} else {
						ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay").document(docId).set(data);
					}
				} else {
					LOGGER.info("No such document JourneyDay!");
					return "error/error-journey";
				}
			}
		}
		// end journey day in 10days

		// journey day in 21days
		if (name.equals("21days")) {
			// get facebookId, get uid
			if (idToken == null) {
				LOGGER.info("check idToken null.");
				List<HashMap<String, String>> listMap = new ArrayList<>();
				HashMap<String, String> hashMap = new HashMap();
				String urlJourneyDay = "/journey/21days/" + day;
				hashMap.put("urlJourneyDay", urlJourneyDay);
				listMap.add(hashMap);
				model.addAttribute("journeyDay", listMap);
				return "check-idToken/check-token-journey";
			}
			FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
			String uid = decodedToken.getUid();
			// get doc id account
			ApiFuture<QuerySnapshot> futureAccount = db.collection("Account").whereEqualTo("uid", uid).get();
			List<QueryDocumentSnapshot> accountDocuments = futureAccount.get().getDocuments();
			String facebookId = null;
			for (DocumentSnapshot document : accountDocuments) {
				facebookId = document.getId();
			}

			if (Integer.parseInt(day) < 1) {
				LOGGER.info("21days day < 1");
				return "error/error-journey";
			}
			if ((Integer.parseInt(day) > 1) && (Integer.parseInt(day) < 22)) {

				int dayJourneyOld = dayJourney - 1;
				String docJourneyDay = name + dayJourneyOld + facebookId;
				LOGGER.info("doc journey day: " + docJourneyDay);
				DocumentReference docRef = db.collection("JourneyDay").document(docJourneyDay);
				ApiFuture<DocumentSnapshot> future = docRef.get();
				DocumentSnapshot document = future.get();
				JourneyDay journeyDay = null;
				if (document.exists()) {
					journeyDay = document.toObject(JourneyDay.class);
					int status = journeyDay.getStatus();
					if (status == 0) {
						LOGGER.info("status == 0");
						return "error/error-journey-old";
					}
					// create new journey day
					Map<String, Object> data = new HashMap<>();
					dayJourney = Integer.parseInt(day) + 1;
					// check day < 22 in 21days
					if (dayJourney < 22) {
						if (dayJourney == 21) {
							data.put("day", 1);
							data.put("journey", "45days");
							data.put("memberId", facebookId);
							data.put("memberName", "");
							data.put("postId", "");
							data.put("status", 0);
							data.put("url", "");
							data.put("uid", uid);
							data.put("accountId", facebookId);
							data.put("createdAt", System.currentTimeMillis() / 1000);
							data.put("updatedAt", System.currentTimeMillis() / 1000);
							String docId = "45days1" + facebookId;
							DocumentReference docRefJourneyDay = db.collection("JourneyDay").document(docId);
							ApiFuture<DocumentSnapshot> futureJourneyDay = docRefJourneyDay.get();
							DocumentSnapshot documentJourneyDay = futureJourneyDay.get();
							if (documentJourneyDay.exists()) {
								LOGGER.info("document JourneyDay exist!");
							} else {
								ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay").document(docId)
										.set(data);
							}
						}
						data.put("day", dayJourney);
						data.put("journey", "21days");
						data.put("memberId", facebookId);
						data.put("memberName", "");
						data.put("postId", "");
						data.put("status", 0);
						data.put("url", "");
						data.put("uid", uid);
						data.put("accountId", facebookId);
						data.put("createdAt", System.currentTimeMillis() / 1000);
						data.put("updatedAt", System.currentTimeMillis() / 1000);
						String docId = name + String.valueOf(dayJourney) + facebookId;
						DocumentReference docRefJourneyDay = db.collection("JourneyDay").document(docId);
						ApiFuture<DocumentSnapshot> futureJourneyDay = docRefJourneyDay.get();
						DocumentSnapshot documentJourneyDay = futureJourneyDay.get();
						if (documentJourneyDay.exists()) {
							LOGGER.info("document JourneyDay exist!");
						} else {
							ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay").document(docId).set(data);
						}
					}
				} else {
					LOGGER.info("No such document JourneyDay!");
					return "error/error-journey-old";
				}
			}
			if (Integer.parseInt(day) >= 22) {
				LOGGER.info("21days day >= 22");
				return "error/error-journey";
			}
			if (Integer.parseInt(day) == 1) {
				LOGGER.info("21days day == 1");
				// check day 10 in journeyday 10days
				String docJourneyDay = "10days10" + facebookId;
				LOGGER.info("doc journey day: " + docJourneyDay);
				DocumentReference docRef = db.collection("JourneyDay").document(docJourneyDay);
				ApiFuture<DocumentSnapshot> future = docRef.get();
				DocumentSnapshot document = future.get();
				JourneyDay journeyDay = null;
				if (document.exists()) {
					journeyDay = document.toObject(JourneyDay.class);
					int status = journeyDay.getStatus();
					if (status == 0) {
						LOGGER.info("status == 0");
						return "error/error-journey-old";
					}
					// create new journey day
					Map<String, Object> data = new HashMap<>();
					dayJourney = Integer.parseInt(day) + 1;
					data.put("day", dayJourney);
					data.put("journey", "21days");
					data.put("memberId", facebookId);
					data.put("memberName", "");
					data.put("postId", "");
					data.put("status", 0);
					data.put("url", "");
					data.put("uid", uid);
					data.put("accountId", facebookId);
					data.put("createdAt", System.currentTimeMillis() / 1000);
					data.put("updatedAt", System.currentTimeMillis() / 1000);
					String docId = name + String.valueOf(dayJourney) + facebookId;
					DocumentReference docRefJourneyDay = db.collection("JourneyDay").document(docId);
					ApiFuture<DocumentSnapshot> futureJourneyDay = docRefJourneyDay.get();
					DocumentSnapshot documentJourneyDay = futureJourneyDay.get();
					if (documentJourneyDay.exists()) {
						LOGGER.info("document JourneyDay exist!");
					} else {
						ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay").document(docId).set(data);
					}
				} else {
					LOGGER.info("No such document JourneyDay!");
					return "error/error-journey";
				}
			}
		}
		// end journey day in 21days

		// journey day in 45days
		if (name.equals("45days")) {
			// get facebookId, get uid
			if (idToken == null) {
				LOGGER.info("check idToken null.");
				List<HashMap<String, String>> listMap = new ArrayList<>();
				HashMap<String, String> hashMap = new HashMap();
				String urlJourneyDay = "/journey/45days/" + day;
				hashMap.put("urlJourneyDay", urlJourneyDay);
				listMap.add(hashMap);
				model.addAttribute("journeyDay", listMap);
				return "check-idToken/check-token-journey";
			}
			FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
			String uid = decodedToken.getUid();
			// get doc id account
			ApiFuture<QuerySnapshot> futureAccount = db.collection("Account").whereEqualTo("uid", uid).get();
			List<QueryDocumentSnapshot> accountDocuments = futureAccount.get().getDocuments();
			String facebookId = null;
			for (DocumentSnapshot document : accountDocuments) {
				facebookId = document.getId();
			}

			if (Integer.parseInt(day) < 1) {
				LOGGER.info("45days day < 1");
				return "error/error-journey";
			}
			if ((Integer.parseInt(day) > 1) && (Integer.parseInt(day) < 46)) {

				int dayJourneyOld = dayJourney - 1;
				String docJourneyDay = name + dayJourneyOld + facebookId;
				LOGGER.info("doc journey day: " + docJourneyDay);
				DocumentReference docRef = db.collection("JourneyDay").document(docJourneyDay);
				ApiFuture<DocumentSnapshot> future = docRef.get();
				DocumentSnapshot document = future.get();
				JourneyDay journeyDay = null;
				if (document.exists()) {
					journeyDay = document.toObject(JourneyDay.class);
					int status = journeyDay.getStatus();
					if (status == 0) {
						LOGGER.info("status == 0");
						return "error/error-journey-old";
					}
					// create new journey day
					Map<String, Object> data = new HashMap<>();
					dayJourney = Integer.parseInt(day) + 1;
					// check day < 46 in 45days
					if (dayJourney < 46) {
						if (dayJourney == 45) {
							data.put("day", 1);
							data.put("journey", "90days");
							data.put("memberId", facebookId);
							data.put("memberName", "");
							data.put("postId", "");
							data.put("status", 0);
							data.put("url", "");
							data.put("uid", uid);
							data.put("accountId", facebookId);
							data.put("createdAt", System.currentTimeMillis() / 1000);
							data.put("updatedAt", System.currentTimeMillis() / 1000);
							String docId = "90days1" + facebookId;
							DocumentReference docRefJourneyDay = db.collection("JourneyDay").document(docId);
							ApiFuture<DocumentSnapshot> futureJourneyDay = docRefJourneyDay.get();
							DocumentSnapshot documentJourneyDay = futureJourneyDay.get();
							if (documentJourneyDay.exists()) {
								LOGGER.info("document JourneyDay exist!");
							} else {
								ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay").document(docId)
										.set(data);
							}
						}
						data.put("day", dayJourney);
						data.put("journey", "45days");
						data.put("memberId", facebookId);
						data.put("memberName", "");
						data.put("postId", "");
						data.put("status", 0);
						data.put("url", "");
						data.put("uid", uid);
						data.put("accountId", facebookId);
						data.put("createdAt", System.currentTimeMillis() / 1000);
						data.put("updatedAt", System.currentTimeMillis() / 1000);
						String docId = name + String.valueOf(dayJourney) + facebookId;
						DocumentReference docRefJourneyDay = db.collection("JourneyDay").document(docId);
						ApiFuture<DocumentSnapshot> futureJourneyDay = docRefJourneyDay.get();
						DocumentSnapshot documentJourneyDay = futureJourneyDay.get();
						if (documentJourneyDay.exists()) {
							LOGGER.info("document JourneyDay exist!");
						} else {
							ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay").document(docId).set(data);
						}
					}
				} else {
					LOGGER.info("No such document JourneyDay!");
					return "error/error-journey";
				}
			}
			if (Integer.parseInt(day) >= 46) {
				LOGGER.info("45days day >= 46");
				return "error/error-journey";
			}
			if (Integer.parseInt(day) == 1) {
				LOGGER.info("45days day == 1");
				// check day 21 in journeyday 21days
				String docJourneyDay = "21days21" + facebookId;
				LOGGER.info("doc journey day: " + docJourneyDay);
				DocumentReference docRef = db.collection("JourneyDay").document(docJourneyDay);
				ApiFuture<DocumentSnapshot> future = docRef.get();
				DocumentSnapshot document = future.get();
				JourneyDay journeyDay = null;
				if (document.exists()) {
					journeyDay = document.toObject(JourneyDay.class);
					int status = journeyDay.getStatus();
					if (status == 0) {
						LOGGER.info("status == 0");
						return "error/error-journey-old";
					}
					// create new journey day
					Map<String, Object> data = new HashMap<>();
					dayJourney = Integer.parseInt(day) + 1;
					data.put("day", dayJourney);
					data.put("journey", "45days");
					data.put("memberId", facebookId);
					data.put("memberName", "");
					data.put("postId", "");
					data.put("status", 0);
					data.put("url", "");
					data.put("uid", uid);
					data.put("accountId", facebookId);
					data.put("createdAt", System.currentTimeMillis() / 1000);
					data.put("updatedAt", System.currentTimeMillis() / 1000);
					String docId = name + String.valueOf(dayJourney) + facebookId;
					DocumentReference docRefJourneyDay = db.collection("JourneyDay").document(docId);
					ApiFuture<DocumentSnapshot> futureJourneyDay = docRefJourneyDay.get();
					DocumentSnapshot documentJourneyDay = futureJourneyDay.get();
					if (documentJourneyDay.exists()) {
						LOGGER.info("document JourneyDay exist!");
					} else {
						ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay").document(docId).set(data);
					}
				} else {
					LOGGER.info("No such document JourneyDay!");
					return "error/error-journey";
				}
			}
		}
		// end journey day in 45days

		// journey day in 90days
		if (name.equals("90days")) {
			// get facebookId, get uid
			if (idToken == null) {
				LOGGER.info("check idToken null.");
				List<HashMap<String, String>> listMap = new ArrayList<>();
				HashMap<String, String> hashMap = new HashMap();
				String urlJourneyDay = "/journey/90days/" + day;
				hashMap.put("urlJourneyDay", urlJourneyDay);
				listMap.add(hashMap);
				model.addAttribute("journeyDay", listMap);
				return "check-idToken/check-token-journey";
			}
			FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
			String uid = decodedToken.getUid();
			// get doc id account
			ApiFuture<QuerySnapshot> futureAccount = db.collection("Account").whereEqualTo("uid", uid).get();
			List<QueryDocumentSnapshot> accountDocuments = futureAccount.get().getDocuments();
			String facebookId = null;
			for (DocumentSnapshot document : accountDocuments) {
				facebookId = document.getId();
			}

			if (Integer.parseInt(day) < 1) {
				LOGGER.info("90days day < 1");
				return "error/error-journey";
			}
			if ((Integer.parseInt(day) > 1) && (Integer.parseInt(day) < 91)) {

				int dayJourneyOld = dayJourney - 1;
				String docJourneyDay = name + dayJourneyOld + facebookId;
				LOGGER.info("doc journey day: " + docJourneyDay);
				DocumentReference docRef = db.collection("JourneyDay").document(docJourneyDay);
				ApiFuture<DocumentSnapshot> future = docRef.get();
				DocumentSnapshot document = future.get();
				JourneyDay journeyDay = null;
				if (document.exists()) {
					journeyDay = document.toObject(JourneyDay.class);
					int status = journeyDay.getStatus();
					if (status == 0) {
						LOGGER.info("status == 0");
						return "error/error-journey-old";
					}
					// create new journey day
					Map<String, Object> data = new HashMap<>();
					dayJourney = Integer.parseInt(day) + 1;
					// check day < 91 in 90days
					if (dayJourney < 91) {
						data.put("day", dayJourney);
						data.put("journey", "90days");
						data.put("memberId", facebookId);
						data.put("memberName", "");
						data.put("postId", "");
						data.put("status", 0);
						data.put("url", "");
						data.put("uid", uid);
						data.put("accountId", facebookId);
						data.put("createdAt", System.currentTimeMillis() / 1000);
						data.put("updatedAt", System.currentTimeMillis() / 1000);
						String docId = name + String.valueOf(dayJourney) + facebookId;
						DocumentReference docRefJourneyDay = db.collection("JourneyDay").document(docId);
						ApiFuture<DocumentSnapshot> futureJourneyDay = docRefJourneyDay.get();
						DocumentSnapshot documentJourneyDay = futureJourneyDay.get();
						if (documentJourneyDay.exists()) {
							LOGGER.info("document JourneyDay exist!");
						} else {
							ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay").document(docId).set(data);
						}
					}
				} else {
					LOGGER.info("No such document JourneyDay!");
					return "error/error-journey";
				}
			}
			if (Integer.parseInt(day) >= 91) {
				LOGGER.info("90days day >= 91");
				return "error/error-journey";
			}
			if (Integer.parseInt(day) == 1) {
				LOGGER.info("90days day == 1");
				// check day 45 in journeyday 45days
				String docJourneyDay = "45days45" + facebookId;
				LOGGER.info("doc journey day: " + docJourneyDay);
				DocumentReference docRef = db.collection("JourneyDay").document(docJourneyDay);
				ApiFuture<DocumentSnapshot> future = docRef.get();
				DocumentSnapshot document = future.get();
				JourneyDay journeyDay = null;
				if (document.exists()) {
					journeyDay = document.toObject(JourneyDay.class);
					int status = journeyDay.getStatus();
					if (status == 0) {
						LOGGER.info("status == 0");
						return "error/error-journey-old";
					}
					// create new journey day
					Map<String, Object> data = new HashMap<>();
					dayJourney = Integer.parseInt(day) + 1;
					data.put("day", dayJourney);
					data.put("journey", "90days");
					data.put("memberId", facebookId);
					data.put("memberName", "");
					data.put("postId", "");
					data.put("status", 0);
					data.put("url", "");
					data.put("uid", uid);
					data.put("accountId", facebookId);
					data.put("createdAt", System.currentTimeMillis() / 1000);
					data.put("updatedAt", System.currentTimeMillis() / 1000);
					String docId = name + String.valueOf(dayJourney) + facebookId;
					DocumentReference docRefJourneyDay = db.collection("JourneyDay").document(docId);
					ApiFuture<DocumentSnapshot> futureJourneyDay = docRefJourneyDay.get();
					DocumentSnapshot documentJourneyDay = futureJourneyDay.get();
					if (documentJourneyDay.exists()) {
						LOGGER.info("document JourneyDay exist!");
					} else {
						ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay").document(docId).set(data);
					}
				} else {
					LOGGER.info("No such document JourneyDay!");
					return "error/error-journey";
				}
			}
		}
		// end journey day in 90days

		for (Journey journey : dayList) {
			if (journey.getName().equals(name) && journey.getDay().equals(day)) {
				model.addAttribute("journey", journey);
			}
		}

		return "journeys/journeyDay";
	}

	@RequestMapping(value = "checkJourneyDay", method = RequestMethod.POST)
	public void checkJourneyDay(Model model, @RequestParam String url,
			@CookieValue(value = "idToken", required = true) String idToken, @RequestParam String journey,
			@RequestParam String numDay, HttpServletResponse response)
			throws IOException, FirebaseAuthException, InterruptedException, ExecutionException {
		LOGGER.info("url video check day: " + url);
		FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
		String uid = decodedToken.getUid();
//		Firestore db = FirestoreClient.getFirestore();
		Firestore db = FirestoreClient.getFirestore();
		Document doc = Jsoup.connect(url).get();
		int begin = doc.html().indexOf("content_owner_id_new&quot;:&quot;")
				+ "content_owner_id_new&quot;:&quot;".length();
		int end = doc.html().indexOf("&quot;", begin);
		String urlContent = doc.select("meta[property=\"og:url\"]").attr("content");
		int last = urlContent.lastIndexOf("=");
		String postId = urlContent.substring(last + 1);
		String memberId = doc.html().substring(begin, end);
		LOGGER.info("postId: " + postId);
		LOGGER.info("memberId: " + memberId);
		String memberName = doc.select("meta[property=\"og:title\"]").attr("content");
		String _journeyDay = doc.select(".bo p").text();
		Pattern p = Pattern.compile("(\\d+)(/|\\.)(\\d+)");
		Matcher m = p.matcher(_journeyDay.toLowerCase());
		String journeyDay = null;
		String journeyName = null;
		String day = null;
		if (m.find()) {
			String spliter[] = m.group().split("/|\\.");
			int current = Integer.parseInt(spliter[0]);
			int total = Integer.parseInt(spliter[1]);

			journeyDay = current + "/" + total;
			int lenght = journeyDay.length();
			day = journeyDay.substring(0, journeyDay.indexOf("/"));
			journeyName = journeyDay.substring((journeyDay.indexOf("/") + 1), lenght);
		}
		LOGGER.info("day: " + day);
		LOGGER.info("journeyName: " + journeyName);
		LOGGER.info("memberName: " + memberName);
		LOGGER.info("journey param: " + journey);
		LOGGER.info("numDay param: " + numDay);
		if (journeyName == null || day == null) {
			LOGGER.info("journeyName null || day null.");
			response.setStatus(404);
			return;
		}
		if (journeyName == null && day == null) {
			LOGGER.info("journeyName null && day null.");
			response.setStatus(404);
			return;
		}
		if (!journeyName.equals(journey)) {
			LOGGER.info("journey in post # journey uri");
			response.setStatus(404);
			return;
		}
		if (!day.equals(numDay)) {
			LOGGER.info("day in post # day uri");
			response.setStatus(404);
			return;
		}
		// get account by uid
		ApiFuture<QuerySnapshot> futureAcc = db.collection("Account").whereEqualTo("uid", uid).get();
		List<QueryDocumentSnapshot> accDocuments = futureAcc.get().getDocuments();
		String facebookId = null;
		for (DocumentSnapshot document : accDocuments) {
			facebookId = document.getId();
		}
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
		if (!account.getDisplayName().equals(memberName)) {
			LOGGER.info("An cap bai viet cua nguoi khac.");
			response.setStatus(405);
			return;
		} else {
			if (journeyName.equals(journey) && day.equals(numDay)) {
				LOGGER.info("check ok");
				String docJourneyDay = journey + "days" + numDay + facebookId;
				LOGGER.info("docJourneyDay: " + docJourneyDay);
				DocumentReference docRefJourney = db.collection("JourneyDay").document(docJourneyDay);

				// check url journey day old
				ApiFuture<QuerySnapshot> futureJourney = db.collection("JourneyDay")
						.whereEqualTo("accountId", facebookId).whereEqualTo("url", url).get();
				List<QueryDocumentSnapshot> journeyDocuments = futureJourney.get().getDocuments();
				if (!journeyDocuments.isEmpty()) {
					LOGGER.info("url journey day exits, break.");
					response.setStatus(400);
					return;
				}
				// end check

				Map<String, Object> updates = new HashMap<>();
				updates.put("memberId", memberId);
				updates.put("memberName", memberName);
				updates.put("postId", postId);
				updates.put("status", 1);
				updates.put("url", url);
				updates.put("updatedAt", System.currentTimeMillis() / 1000);
				ApiFuture<WriteResult> futureJourneyDay = docRefJourney.update(updates);
				// update Account
				if (document.exists()) {
					// Update an existing document
					LOGGER.info("update memberId in account");
					ApiFuture<WriteResult> future = docRefAccount.update("memberId", memberId);
				} else {
					System.out.println("No such document account!");
				}

				String uri = null;
				if (journeyName.equals("3") && numDay.equals("3")) {
					LOGGER.info("3days3");
					uri = "/journey/5days/1";
					response.getWriter().println(uri);
					response.setStatus(200);
					return;
				}
				if (journeyName.equals("5") && numDay.equals("5")) {
					LOGGER.info("5days5");
					uri = "/journey/7days/1";
					response.getWriter().println(uri);
					response.setStatus(200);
					return;
				}
				if (journeyName.equals("7") && numDay.equals("7")) {
					LOGGER.info("7days7");
					uri = "/journey/10days/1";
					response.getWriter().println(uri);
					response.setStatus(200);
					return;
				}
				if (journeyName.equals("10") && numDay.equals("10")) {
					LOGGER.info("10days10");
					uri = "/journey/21days/1";
					response.getWriter().println(uri);
					response.setStatus(200);
					return;
				}
				if (journeyName.equals("21") && numDay.equals("21")) {
					LOGGER.info("21days21");
					uri = "/journey/45days/1";
					response.getWriter().println(uri);
					response.setStatus(200);
					return;
				}
				if (journeyName.equals("45") && numDay.equals("45")) {
					LOGGER.info("45days45");
					uri = "/journey/90days/1";
					response.getWriter().println(uri);
					response.setStatus(200);
					return;
				}
				if (journeyName.equals("90") && numDay.equals("90")) {
					LOGGER.info("90days90");
					uri = "/journey/90days/90";
					response.getWriter().println(uri);
					response.setStatus(200);
					return;
				}
				if ((journeyName.equals("3") && (Integer.parseInt(numDay) < 3))
						|| (journeyName.equals("5") && (Integer.parseInt(numDay) < 5))
						|| (journeyName.equals("7") && (Integer.parseInt(numDay) < 7))
						|| (journeyName.equals("10") && (Integer.parseInt(numDay) < 10))
						|| (journeyName.equals("21") && (Integer.parseInt(numDay) < 21))
						|| (journeyName.equals("45") && (Integer.parseInt(numDay) < 45))
						|| (journeyName.equals("90") && (Integer.parseInt(numDay) < 90))) {
					LOGGER.info(journeyName + "days/" + numDay);
					int dayNumber = Integer.parseInt(numDay) + 1;
					day = String.valueOf(dayNumber);
					uri = "/journey/" + journey + "days" + "/" + day;
					response.getWriter().println(uri);
					response.setStatus(200);
					return;
				}
			} else {
				response.setStatus(404);
				return;
			}
		}
	}

	@RequestMapping(value = "/journey/3days", method = RequestMethod.GET)
	public String evt3days(Model model, HttpServletRequest request)
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
		LOGGER.info("idToken: " + idToken);
		List<HashMap<String, String>> listJourneyActive = new ArrayList<>();
		List<HashMap<String, String>> listDayLock = new ArrayList<>();
		if (idToken == null) {
			HashMap<String, String> hashMap = new HashMap();
			String nameDay = "Day " + String.valueOf(1);
			hashMap.put("nameDay", nameDay);
			hashMap.put("keyDay", String.valueOf(1));
			LOGGER.info("nameDay: " + nameDay);
			LOGGER.info("keyDay: " + String.valueOf(1));
			listJourneyActive.add(hashMap);

			for (Journey journey : dayList) {
				if (journey.getName().equals("3days")) {
					if (Integer.parseInt(journey.getDay()) > 1) {
						HashMap<String, String> hashMapLock = new HashMap();
						hashMapLock.put("keyDay", journey.getDay());
						LOGGER.info("keyDay: " + journey.getDay());
						String nameDayLock = "Day " + journey.getDay();
						hashMapLock.put("nameDay", nameDayLock);
						LOGGER.info("nameDay: " + nameDayLock);
						listDayLock.add(hashMapLock);
					}
				}
			}
		} else {
			FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
			String uid = decodedToken.getUid();
//			Firestore db = FirestoreClient.getFirestore();
			Firestore db = FirestoreClient.getFirestore();
			// get account id
			ApiFuture<QuerySnapshot> future = db.collection("Account").whereEqualTo("uid", uid).get();
			List<QueryDocumentSnapshot> documents = future.get().getDocuments();
			String accountId = null;
			for (DocumentSnapshot document : documents) {
				accountId = document.getId();
			}
			LOGGER.info("account Id: " + accountId);
			// get list day learned
			ApiFuture<QuerySnapshot> futureJourneyDay = db.collection("JourneyDay").whereEqualTo("accountId", accountId)
					.whereEqualTo("journey", "3days").whereEqualTo("status", 1).get();
			List<QueryDocumentSnapshot> journeyDocuments = futureJourneyDay.get().getDocuments();
			int numDays = journeyDocuments.size();
			LOGGER.info("numDays: " + numDays);

			JourneyDay journeyDay = new JourneyDay();
			// get account by uid
			ApiFuture<QuerySnapshot> futureAcc = db.collection("Account").whereEqualTo("uid", uid).get();
			List<QueryDocumentSnapshot> accDocuments = futureAcc.get().getDocuments();
			String facebookId = null;
			for (DocumentSnapshot docAcc : accDocuments) {
				facebookId = docAcc.getId();
			}
			if (numDays == 0) {
				HashMap<String, String> hashMap = new HashMap();
				String nameDay = "Day " + String.valueOf(1);
				hashMap.put("nameDay", nameDay);
				hashMap.put("keyDay", String.valueOf(1));
				LOGGER.info("nameDay: " + nameDay);
				LOGGER.info("keyDay: " + String.valueOf(1));
				listJourneyActive.add(hashMap);
			} else {
				for (DocumentSnapshot document : journeyDocuments) {
					journeyDay = document.toObject(JourneyDay.class);
					HashMap<String, String> hashMap = new HashMap();
					String nameDay = "Day " + String.valueOf(journeyDay.getDay());
					hashMap.put("nameDay", nameDay);
					hashMap.put("keyDay", String.valueOf(journeyDay.getDay()));
					LOGGER.info("nameDay: " + nameDay);
					LOGGER.info("keyDay: " + String.valueOf(journeyDay.getDay()));
					listJourneyActive.add(hashMap);
				}
				if (numDays < 3) {
					String docIdNext = "3days" + String.valueOf(numDays + 1) + facebookId;
					DocumentReference docRef = db.collection("JourneyDay").document(docIdNext);
					ApiFuture<DocumentSnapshot> futureCheck = docRef.get();
					DocumentSnapshot document = futureCheck.get();
					if (document.exists()) {
						LOGGER.info("doc Journey Day Check exist!");
					} else {
						Map<String, Object> data = new HashMap<>();
						data.put("day", numDays + 1);
						data.put("journey", "3days");
						data.put("memberId", facebookId);
						data.put("memberName", "");
						data.put("postId", "");
						data.put("status", 0);
						data.put("url", "");
						data.put("uid", uid);
						data.put("accountId", facebookId);
						data.put("createdAt", System.currentTimeMillis() / 1000);
						data.put("updatedAt", System.currentTimeMillis() / 1000);
						ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay").document(docIdNext).set(data);
					}

					HashMap<String, String> hashMapNext = new HashMap();
					String nameDayNext = "Day " + String.valueOf(numDays + 1);
					hashMapNext.put("nameDay", nameDayNext);
					hashMapNext.put("keyDay", String.valueOf(numDays + 1));
					LOGGER.info("nameDay: " + nameDayNext);
					LOGGER.info("keyDay: " + String.valueOf(numDays + 1));
					listJourneyActive.add(hashMapNext);

					// create doc 5days day 1
					String docIdNext5days = "5days" + String.valueOf(1) + facebookId;
					DocumentReference docRef5days = db.collection("JourneyDay").document(docIdNext5days);
					ApiFuture<DocumentSnapshot> futureCheck5days = docRef5days.get();
					DocumentSnapshot document5days = futureCheck5days.get();
					if (document5days.exists()) {
						LOGGER.info("doc 5days day 1 Journey Day Check exist!");
					} else {
						LOGGER.info("doc 5days day 1 Journey Day Check not exist!");

						Map<String, Object> data = new HashMap<>();
						data.put("day", 1);
						data.put("journey", "5days");
						data.put("memberId", facebookId);
						data.put("memberName", "");
						data.put("postId", "");
						data.put("status", 0);
						data.put("url", "");
						data.put("uid", uid);
						data.put("accountId", facebookId);
						data.put("createdAt", System.currentTimeMillis() / 1000);
						data.put("updatedAt", System.currentTimeMillis() / 1000);
						ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay").document(docIdNext5days)
								.set(data);
					}
				}

			}

			// get list journey
			numDays = numDays + 1;
			for (Journey journey : dayList) {
				if (journey.getName().equals("3days")) {
					if (Integer.parseInt(journey.getDay()) > numDays) {
						HashMap<String, String> hashMap = new HashMap();
						hashMap.put("keyDay", journey.getDay());
						LOGGER.info("keyDay: " + journey.getDay());
						String nameDay = "Day " + journey.getDay();
						hashMap.put("nameDay", nameDay);
						LOGGER.info("nameDay: " + nameDay);
						listDayLock.add(hashMap);
					}
				}
			}
		}

		model.addAttribute("activeDays", listJourneyActive);
		model.addAttribute("lockDays", listDayLock);
		return "journeys/3days";
	}

	@RequestMapping(value = "/journey/5days", method = RequestMethod.GET)
	public String evt5days(Model model, HttpServletRequest request)
			throws InterruptedException, ExecutionException, FirebaseAuthException, UnsupportedEncodingException {
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
		LOGGER.info("idToken: " + idToken);
		List<HashMap<String, String>> listJourneyActive = new ArrayList<>();
		List<HashMap<String, String>> listDayLock = new ArrayList<>();
		if (idToken == null) {
			for (Journey journey : dayList) {
				if (journey.getName().equals("5days")) {
					HashMap<String, String> hashMap = new HashMap();
					hashMap.put("keyDay", journey.getDay());
					LOGGER.info("keyDay: " + journey.getDay());
					String nameDay = "Day " + journey.getDay();
					hashMap.put("nameDay", nameDay);
					LOGGER.info("nameDay: " + nameDay);
					listDayLock.add(hashMap);
				}
			}
		} else {
			FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
			String uid = decodedToken.getUid();
//			Firestore db = FirestoreClient.getFirestore();
			Firestore db = FirestoreClient.getFirestore();
			// get account id
			ApiFuture<QuerySnapshot> future = db.collection("Account").whereEqualTo("uid", uid).get();
			List<QueryDocumentSnapshot> documents = future.get().getDocuments();
			String accountId = null;
			for (DocumentSnapshot document : documents) {
				accountId = document.getId();
			}
			LOGGER.info("account Id: " + accountId);
			// get list day learned
			ApiFuture<QuerySnapshot> futureJourneyDay = db.collection("JourneyDay").whereEqualTo("accountId", accountId)
					.whereEqualTo("journey", "5days").whereEqualTo("status", 1).get();
			List<QueryDocumentSnapshot> journeyDocuments = futureJourneyDay.get().getDocuments();
			int numDays = journeyDocuments.size();
			LOGGER.info("numDays: " + numDays);

			JourneyDay journeyDay = new JourneyDay();
			// get account by uid
			ApiFuture<QuerySnapshot> futureAcc = db.collection("Account").whereEqualTo("uid", uid).get();
			List<QueryDocumentSnapshot> accDocuments = futureAcc.get().getDocuments();
			String facebookId = null;
			for (DocumentSnapshot docAcc : accDocuments) {
				facebookId = docAcc.getId();
			}
			if (numDays == 0) {
				// det 3days day3 check status
				String docId3days3 = "3days3" + accountId;
				LOGGER.info("docId3days3: " + docId3days3);
				DocumentReference docRef = db.collection("JourneyDay").document(docId3days3);
				ApiFuture<DocumentSnapshot> future3days2 = docRef.get();
				DocumentSnapshot document = future3days2.get();
				if (document.exists()) {
					LOGGER.info("document 3days day 3 exist");
					journeyDay = document.toObject(JourneyDay.class);
					if (journeyDay.getStatus() == 1) {
						LOGGER.info("document 3days day 3 status == 1");
						HashMap<String, String> hashMap = new HashMap();
						numDays = 1;
						String nameDay = "Day " + String.valueOf(1);
						hashMap.put("nameDay", nameDay);
						hashMap.put("keyDay", String.valueOf(1));
						LOGGER.info("nameDay: " + nameDay);
						LOGGER.info("keyDay: " + String.valueOf(1));
						listJourneyActive.add(hashMap);
					} else {
						LOGGER.info("document 3days day 3 status == 0");
					}
				} else {
					LOGGER.info("doc 3days day 3 not exist.");
					Map<String, Object> data = new HashMap<>();
					data.put("day", 3);
					data.put("journey", "3days");
					data.put("memberId", facebookId);
					data.put("memberName", "");
					data.put("postId", "");
					data.put("status", 0);
					data.put("url", "");
					data.put("uid", uid);
					data.put("accountId", facebookId);
					data.put("createdAt", System.currentTimeMillis() / 1000);
					data.put("updatedAt", System.currentTimeMillis() / 1000);
					ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay").document(docId3days3).set(data);
				}
				for (Journey journey : dayList) {
					if (journey.getName().equals("5days")) {
						if (Integer.parseInt(journey.getDay()) > numDays) {
							HashMap<String, String> hashMap = new HashMap();
							hashMap.put("keyDay", journey.getDay());
							LOGGER.info("keyDay: " + journey.getDay());
							String nameDay = "Day " + journey.getDay();
							hashMap.put("nameDay", nameDay);
							LOGGER.info("nameDay: " + nameDay);
							listDayLock.add(hashMap);
						}
					}
				}
			} else {
				for (DocumentSnapshot document : journeyDocuments) {
					journeyDay = document.toObject(JourneyDay.class);
					HashMap<String, String> hashMap = new HashMap();
					String nameDay = "Day " + String.valueOf(journeyDay.getDay());
					hashMap.put("nameDay", nameDay);
					hashMap.put("keyDay", String.valueOf(journeyDay.getDay()));
					LOGGER.info("nameDay: " + nameDay);
					LOGGER.info("keyDay: " + String.valueOf(journeyDay.getDay()));
					listJourneyActive.add(hashMap);
				}
				if (numDays < 5) {

					String docIdNext = "5days" + String.valueOf(numDays + 1) + facebookId;
					DocumentReference docRef = db.collection("JourneyDay").document(docIdNext);
					ApiFuture<DocumentSnapshot> futureCheck = docRef.get();
					DocumentSnapshot document = futureCheck.get();
					if (document.exists()) {
						LOGGER.info("doc Journey Day Check exist!");
					} else {
						Map<String, Object> data = new HashMap<>();
						data.put("day", numDays + 1);
						data.put("journey", "5days");
						data.put("memberId", facebookId);
						data.put("memberName", "");
						data.put("postId", "");
						data.put("status", 0);
						data.put("url", "");
						data.put("uid", uid);
						data.put("accountId", facebookId);
						data.put("createdAt", System.currentTimeMillis() / 1000);
						data.put("updatedAt", System.currentTimeMillis() / 1000);
						ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay").document(docIdNext).set(data);
					}

					HashMap<String, String> hashMapNext = new HashMap();
					String nameDayNext = "Day " + String.valueOf(numDays + 1);
					hashMapNext.put("nameDay", nameDayNext);
					hashMapNext.put("keyDay", String.valueOf(numDays + 1));
					LOGGER.info("nameDay: " + nameDayNext);
					LOGGER.info("keyDay: " + String.valueOf(numDays + 1));
					listJourneyActive.add(hashMapNext);

					// create doc 7days day 1
					String docIdNext7days1 = "7days" + String.valueOf(1) + facebookId;
					DocumentReference docRef7days1 = db.collection("JourneyDay").document(docIdNext7days1);
					ApiFuture<DocumentSnapshot> futureCheck7days1 = docRef7days1.get();
					DocumentSnapshot document7days1 = futureCheck7days1.get();
					if (document7days1.exists()) {
						LOGGER.info("doc 7days day 1 Journey Day Check exist!");
					} else {
						LOGGER.info("doc 7days day 1 Journey Day Check not exist!");
						Map<String, Object> data = new HashMap<>();
						data.put("day", 1);
						data.put("journey", "7days");
						data.put("memberId", facebookId);
						data.put("memberName", "");
						data.put("postId", "");
						data.put("status", 0);
						data.put("url", "");
						data.put("uid", uid);
						data.put("accountId", facebookId);
						data.put("createdAt", System.currentTimeMillis() / 1000);
						data.put("updatedAt", System.currentTimeMillis() / 1000);
						ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay").document(docIdNext7days1)
								.set(data);
					}
				}

				// get list journey
				numDays = numDays + 1;
				for (Journey journey : dayList) {
					if (journey.getName().equals("5days")) {
						if (Integer.parseInt(journey.getDay()) > numDays) {
							HashMap<String, String> hashMap = new HashMap();
							hashMap.put("keyDay", journey.getDay());
							LOGGER.info("keyDay: " + journey.getDay());
							String nameDay = "Day " + journey.getDay();
							hashMap.put("nameDay", nameDay);
							LOGGER.info("nameDay: " + nameDay);
							listDayLock.add(hashMap);
						}
					}
				}
			}

		}

		model.addAttribute("activeDays", listJourneyActive);
		model.addAttribute("lockDays", listDayLock);
		return "journeys/5days";
	}

	@RequestMapping(value = "/journey/7days", method = RequestMethod.GET)
	public String evt7days(Model model, HttpServletRequest request)
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
		LOGGER.info("idToken: " + idToken);
		List<HashMap<String, String>> listJourneyActive = new ArrayList<>();
		List<HashMap<String, String>> listDayLock = new ArrayList<>();
		if (idToken == null) {
			LOGGER.info("idToken null.");
			for (Journey journey : dayList) {
				if (journey.getName().equals("7days")) {
					if (Integer.parseInt(journey.getDay()) > 0) {
						HashMap<String, String> hashMap = new HashMap();
						hashMap.put("keyDay", journey.getDay());
						LOGGER.info("keyDay: " + journey.getDay());
						String nameDay = "Day " + journey.getDay();
						hashMap.put("nameDay", nameDay);
						LOGGER.info("nameDay: " + nameDay);
						listDayLock.add(hashMap);
					}
				}
			}
		} else {
			LOGGER.info("idToken != null");
			FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
			String uid = decodedToken.getUid();
			Firestore db = FirestoreClient.getFirestore();
			// get account id
			ApiFuture<QuerySnapshot> future = db.collection("Account").whereEqualTo("uid", uid).get();
			List<QueryDocumentSnapshot> documents = future.get().getDocuments();
			String accountId = null;
			for (DocumentSnapshot document : documents) {
				accountId = document.getId();
			}
			LOGGER.info("account Id: " + accountId);
			// get list day learned
			ApiFuture<QuerySnapshot> futureJourneyDay = db.collection("JourneyDay").whereEqualTo("accountId", accountId)
					.whereEqualTo("journey", "7days").whereEqualTo("status", 1).get();
			List<QueryDocumentSnapshot> journeyDocuments = futureJourneyDay.get().getDocuments();
			int numDays = journeyDocuments.size();
			LOGGER.info("numDays size: " + numDays);
			JourneyDay journeyDay = new JourneyDay();

			// get account by uid
			ApiFuture<QuerySnapshot> futureAcc = db.collection("Account").whereEqualTo("uid", uid).get();
			List<QueryDocumentSnapshot> accDocuments = futureAcc.get().getDocuments();
			String facebookId = null;
			for (DocumentSnapshot docAcc : accDocuments) {
				facebookId = docAcc.getId();
			}
			if (numDays == 0) {
				// check status 5days day 5
				String docId5days5 = "5days5" + accountId;
				LOGGER.info("docId5days5: " + docId5days5);
				DocumentReference docRef = db.collection("JourneyDay").document(docId5days5);
				ApiFuture<DocumentSnapshot> future5days = docRef.get();
				DocumentSnapshot document = future5days.get();
				if (document.exists()) {
					LOGGER.info("doc 5days day 5 exist.");
					journeyDay = document.toObject(JourneyDay.class);
					if (journeyDay.getStatus() == 1) {
						LOGGER.info("doc 5days day 5 status == 1.");
						HashMap<String, String> hashMap = new HashMap();
						numDays = 1;
						String nameDay = "Day " + String.valueOf(1);
						hashMap.put("nameDay", nameDay);
						hashMap.put("keyDay", String.valueOf(1));
						LOGGER.info("nameDay: " + nameDay);
						LOGGER.info("keyDay: " + String.valueOf(1));
						listJourneyActive.add(hashMap);
					} else {
						LOGGER.info("doc 5days day 5 status == 0.");
					}
				} else {
					LOGGER.info("doc 5days day 5 not exist.");

					Map<String, Object> data = new HashMap<>();
					data.put("day", 5);
					data.put("journey", "5days");
					data.put("memberId", facebookId);
					data.put("memberName", "");
					data.put("postId", "");
					data.put("status", 0);
					data.put("url", "");
					data.put("uid", uid);
					data.put("accountId", facebookId);
					data.put("createdAt", System.currentTimeMillis() / 1000);
					data.put("updatedAt", System.currentTimeMillis() / 1000);
					ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay").document(docId5days5).set(data);
				}

				for (Journey journey : dayList) {
					if (journey.getName().equals("7days")) {
						if (Integer.parseInt(journey.getDay()) > numDays) {
							HashMap<String, String> hashMap = new HashMap();
							hashMap.put("keyDay", journey.getDay());
							LOGGER.info("keyDay: " + journey.getDay());
							String nameDay = "Day " + journey.getDay();
							hashMap.put("nameDay", nameDay);
							LOGGER.info("nameDay: " + nameDay);
							listDayLock.add(hashMap);
						}
					}
				}
			} else {
				for (DocumentSnapshot document : journeyDocuments) {
					journeyDay = document.toObject(JourneyDay.class);
					HashMap<String, String> hashMap = new HashMap();
					String nameDay = "Day " + String.valueOf(journeyDay.getDay());
					hashMap.put("nameDay", nameDay);
					hashMap.put("keyDay", String.valueOf(journeyDay.getDay()));
					LOGGER.info("nameDay: " + nameDay);
					LOGGER.info("keyDay: " + String.valueOf(journeyDay.getDay()));
					listJourneyActive.add(hashMap);
				}
				if (numDays < 7) {
					String docIdNext = "7days" + String.valueOf(numDays + 1) + facebookId;
					DocumentReference docRef = db.collection("JourneyDay").document(docIdNext);
					ApiFuture<DocumentSnapshot> futureCheck = docRef.get();
					DocumentSnapshot document = futureCheck.get();
					if (document.exists()) {
						LOGGER.info("doc Journey Day Check exist!");
					} else {

						Map<String, Object> data = new HashMap<>();
						data.put("day", numDays + 1);
						data.put("journey", "7days");
						data.put("memberId", facebookId);
						data.put("memberName", "");
						data.put("postId", "");
						data.put("status", 0);
						data.put("url", "");
						data.put("uid", uid);
						data.put("accountId", facebookId);
						data.put("createdAt", System.currentTimeMillis() / 1000);
						data.put("updatedAt", System.currentTimeMillis() / 1000);
						ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay").document(docIdNext).set(data);
					}

					HashMap<String, String> hashMapNext = new HashMap();
					String nameDayNext = "Day " + String.valueOf(numDays + 1);
					hashMapNext.put("nameDay", nameDayNext);
					hashMapNext.put("keyDay", String.valueOf(numDays + 1));
					LOGGER.info("nameDay: " + nameDayNext);
					LOGGER.info("keyDay: " + String.valueOf(numDays + 1));
					listJourneyActive.add(hashMapNext);

					// create doc 10days day 1
					String docIdNext10days1 = "10days" + String.valueOf(1) + facebookId;
					DocumentReference docRef10days1 = db.collection("JourneyDay").document(docIdNext10days1);
					ApiFuture<DocumentSnapshot> futureCheck10days1 = docRef10days1.get();
					DocumentSnapshot document10days1 = futureCheck10days1.get();
					if (document10days1.exists()) {
						LOGGER.info("doc 10days day 1 Journey Day Check exist!");
					} else {
						LOGGER.info("doc 10days day 1 Journey Day Check not exist!");
						Map<String, Object> data = new HashMap<>();
						data.put("day", 1);
						data.put("journey", "10days");
						data.put("memberId", facebookId);
						data.put("memberName", "");
						data.put("postId", "");
						data.put("status", 0);
						data.put("url", "");
						data.put("uid", uid);
						data.put("accountId", facebookId);
						data.put("createdAt", System.currentTimeMillis() / 1000);
						data.put("updatedAt", System.currentTimeMillis() / 1000);
						ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay").document(docIdNext10days1)
								.set(data);
					}
				}

				for (Journey journey : dayList) {
					if (journey.getName().equals("7days")) {
						if (Integer.parseInt(journey.getDay()) > (numDays + 1)) {
							HashMap<String, String> hashMap = new HashMap();
							hashMap.put("keyDay", journey.getDay());
							LOGGER.info("keyDay: " + journey.getDay());
							String nameDay = "Day " + journey.getDay();
							hashMap.put("nameDay", nameDay);
							LOGGER.info("nameDay: " + nameDay);
							listDayLock.add(hashMap);
						}
					}
				}
			}
		}
		model.addAttribute("activeDays", listJourneyActive);
		model.addAttribute("lockDays", listDayLock);
		return "journeys/7days";
	}

	@RequestMapping(value = "/journey/10days", method = RequestMethod.GET)
	public String evt10days(Model model, HttpServletRequest request)
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
		LOGGER.info("idToken: " + idToken);
		List<HashMap<String, String>> listJourneyActive = new ArrayList<>();
		List<HashMap<String, String>> listDayLock = new ArrayList<>();
		if (idToken == null) {
			LOGGER.info("idToken null.");
			for (Journey journey : dayList) {
				if (journey.getName().equals("10days")) {
					if (Integer.parseInt(journey.getDay()) > 0) {
						HashMap<String, String> hashMap = new HashMap();
						hashMap.put("keyDay", journey.getDay());
						LOGGER.info("keyDay: " + journey.getDay());
						String nameDay = "Day " + journey.getDay();
						hashMap.put("nameDay", nameDay);
						LOGGER.info("nameDay: " + nameDay);
						listDayLock.add(hashMap);
					}
				}
			}
		} else {
			LOGGER.info("idToken != null");
			FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
			String uid = decodedToken.getUid();
			Firestore db = FirestoreClient.getFirestore();
			// get account id
			ApiFuture<QuerySnapshot> future = db.collection("Account").whereEqualTo("uid", uid).get();
			List<QueryDocumentSnapshot> documents = future.get().getDocuments();
			String accountId = null;
			for (DocumentSnapshot document : documents) {
				accountId = document.getId();
			}
			LOGGER.info("account Id: " + accountId);
			// get list day learned
			ApiFuture<QuerySnapshot> futureJourneyDay = db.collection("JourneyDay").whereEqualTo("accountId", accountId)
					.whereEqualTo("journey", "10days").whereEqualTo("status", 1).get();
			List<QueryDocumentSnapshot> journeyDocuments = futureJourneyDay.get().getDocuments();
			int numDays = journeyDocuments.size();
			LOGGER.info("numDays size: " + numDays);
			JourneyDay journeyDay = new JourneyDay();

			// get account by uid
			ApiFuture<QuerySnapshot> futureAcc = db.collection("Account").whereEqualTo("uid", uid).get();
			List<QueryDocumentSnapshot> accDocuments = futureAcc.get().getDocuments();
			String facebookId = null;
			for (DocumentSnapshot docAcc : accDocuments) {
				facebookId = docAcc.getId();
			}

			if (numDays == 0) {
				// check status 7days day 7
				String docId7days7 = "7days7" + accountId;
				LOGGER.info("docId7days7: " + docId7days7);
				DocumentReference docRef = db.collection("JourneyDay").document(docId7days7);
				ApiFuture<DocumentSnapshot> future7days = docRef.get();
				DocumentSnapshot document = future7days.get();
				if (document.exists()) {
					LOGGER.info("doc 7days day 7 exist.");
					journeyDay = document.toObject(JourneyDay.class);
					if (journeyDay.getStatus() == 1) {
						LOGGER.info("doc 7days day 7 status == 1.");
						HashMap<String, String> hashMap = new HashMap();
						numDays = 1;
						String nameDay = "Day " + String.valueOf(1);
						hashMap.put("nameDay", nameDay);
						hashMap.put("keyDay", String.valueOf(1));
						LOGGER.info("nameDay: " + nameDay);
						LOGGER.info("keyDay: " + String.valueOf(1));
						listJourneyActive.add(hashMap);
					} else {
						LOGGER.info("doc 7days day 7 status == 0.");
					}
				} else {
					LOGGER.info("doc 7days day 7 not exist.");

					Map<String, Object> data = new HashMap<>();
					data.put("day", 7);
					data.put("journey", "7days");
					data.put("memberId", facebookId);
					data.put("memberName", "");
					data.put("postId", "");
					data.put("status", 0);
					data.put("url", "");
					data.put("uid", uid);
					data.put("accountId", facebookId);
					data.put("createdAt", System.currentTimeMillis() / 1000);
					data.put("updatedAt", System.currentTimeMillis() / 1000);
					ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay").document(docId7days7).set(data);
				}

				for (Journey journey : dayList) {
					if (journey.getName().equals("10days")) {
						if (Integer.parseInt(journey.getDay()) > numDays) {
							HashMap<String, String> hashMap = new HashMap();
							hashMap.put("keyDay", journey.getDay());
							LOGGER.info("keyDay: " + journey.getDay());
							String nameDay = "Day " + journey.getDay();
							hashMap.put("nameDay", nameDay);
							LOGGER.info("nameDay: " + nameDay);
							listDayLock.add(hashMap);
						}
					}
				}
			} else {
				for (DocumentSnapshot document : journeyDocuments) {
					journeyDay = document.toObject(JourneyDay.class);
					HashMap<String, String> hashMap = new HashMap();
					String nameDay = "Day " + String.valueOf(journeyDay.getDay());
					hashMap.put("nameDay", nameDay);
					hashMap.put("keyDay", String.valueOf(journeyDay.getDay()));
					LOGGER.info("nameDay: " + nameDay);
					LOGGER.info("keyDay: " + String.valueOf(journeyDay.getDay()));
					listJourneyActive.add(hashMap);
				}
				if (numDays < 10) {
					String docIdNext = "10days" + String.valueOf(numDays + 1) + facebookId;
					DocumentReference docRef = db.collection("JourneyDay").document(docIdNext);
					ApiFuture<DocumentSnapshot> futureCheck = docRef.get();
					DocumentSnapshot document = futureCheck.get();
					if (document.exists()) {
						LOGGER.info("doc Journey Day Check exist!");
					} else {

						Map<String, Object> data = new HashMap<>();
						data.put("day", numDays + 1);
						data.put("journey", "10days");
						data.put("memberId", facebookId);
						data.put("memberName", "");
						data.put("postId", "");
						data.put("status", 0);
						data.put("url", "");
						data.put("uid", uid);
						data.put("accountId", facebookId);
						data.put("createdAt", System.currentTimeMillis() / 1000);
						data.put("updatedAt", System.currentTimeMillis() / 1000);
						ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay").document(docIdNext).set(data);
					}

					HashMap<String, String> hashMapNext = new HashMap();
					String nameDayNext = "Day " + String.valueOf(numDays + 1);
					hashMapNext.put("nameDay", nameDayNext);
					hashMapNext.put("keyDay", String.valueOf(numDays + 1));
					LOGGER.info("nameDay: " + nameDayNext);
					LOGGER.info("keyDay: " + String.valueOf(numDays + 1));
					listJourneyActive.add(hashMapNext);

					// create doc 21days day 1
					String docIdNext21days1 = "21days" + String.valueOf(1) + facebookId;
					DocumentReference docRef21days1 = db.collection("JourneyDay").document(docIdNext21days1);
					ApiFuture<DocumentSnapshot> futureCheck21days1 = docRef21days1.get();
					DocumentSnapshot document21days1 = futureCheck21days1.get();
					if (document21days1.exists()) {
						LOGGER.info("doc 21days day1 Journey Day Check exist!");
					} else {
						LOGGER.info("doc 21days day 1 Journey Day Check not exist!");
						Map<String, Object> data = new HashMap<>();
						data.put("day", 1);
						data.put("journey", "21days");
						data.put("memberId", facebookId);
						data.put("memberName", "");
						data.put("postId", "");
						data.put("status", 0);
						data.put("url", "");
						data.put("uid", uid);
						data.put("accountId", facebookId);
						data.put("createdAt", System.currentTimeMillis() / 1000);
						data.put("updatedAt", System.currentTimeMillis() / 1000);
						ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay").document(docIdNext21days1)
								.set(data);
					}
				}

				for (Journey journey : dayList) {
					if (journey.getName().equals("10days")) {
						if (Integer.parseInt(journey.getDay()) > (numDays + 1)) {
							HashMap<String, String> hashMap = new HashMap();
							hashMap.put("keyDay", journey.getDay());
							LOGGER.info("keyDay: " + journey.getDay());
							String nameDay = "Day " + journey.getDay();
							hashMap.put("nameDay", nameDay);
							LOGGER.info("nameDay: " + nameDay);
							listDayLock.add(hashMap);
						}
					}
				}

			}

		}

		model.addAttribute("activeDays", listJourneyActive);
		model.addAttribute("lockDays", listDayLock);
		return "journeys/10days";
	}

	@RequestMapping(value = "/journey/21days", method = RequestMethod.GET)
	public String evt21days(Model model, HttpServletRequest request)
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
		LOGGER.info("idToken: " + idToken);
		List<HashMap<String, String>> listJourneyActive = new ArrayList<>();
		List<HashMap<String, String>> listDayLock = new ArrayList<>();
		if (idToken == null) {
			LOGGER.info("idToken null.");
			for (Journey journey : dayList) {
				if (journey.getName().equals("21days")) {
					if (Integer.parseInt(journey.getDay()) > 0) {
						HashMap<String, String> hashMap = new HashMap();
						hashMap.put("keyDay", journey.getDay());
						LOGGER.info("keyDay: " + journey.getDay());
						String nameDay = "Day " + journey.getDay();
						hashMap.put("nameDay", nameDay);
						LOGGER.info("nameDay: " + nameDay);
						listDayLock.add(hashMap);
					}
				}
			}
		} else {
			LOGGER.info("idToken != null");
			FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
			String uid = decodedToken.getUid();
			Firestore db = FirestoreClient.getFirestore();
			// get account id
			ApiFuture<QuerySnapshot> future = db.collection("Account").whereEqualTo("uid", uid).get();
			List<QueryDocumentSnapshot> documents = future.get().getDocuments();
			String accountId = null;
			for (DocumentSnapshot document : documents) {
				accountId = document.getId();
			}
			LOGGER.info("account Id: " + accountId);
			// get list day learned
			ApiFuture<QuerySnapshot> futureJourneyDay = db.collection("JourneyDay").whereEqualTo("accountId", accountId)
					.whereEqualTo("journey", "21days").whereEqualTo("status", 1).get();
			List<QueryDocumentSnapshot> journeyDocuments = futureJourneyDay.get().getDocuments();
			int numDays = journeyDocuments.size();
			LOGGER.info("numDays size: " + numDays);
			JourneyDay journeyDay = new JourneyDay();

			// get account by uid
			ApiFuture<QuerySnapshot> futureAcc = db.collection("Account").whereEqualTo("uid", uid).get();
			List<QueryDocumentSnapshot> accDocuments = futureAcc.get().getDocuments();
			String facebookId = null;
			for (DocumentSnapshot docAcc : accDocuments) {
				facebookId = docAcc.getId();
			}

			if (numDays == 0) {
				// check status 10days day 10
				String docId10days10 = "10days10" + accountId;
				LOGGER.info("docId10days10: " + docId10days10);
				DocumentReference docRef = db.collection("JourneyDay").document(docId10days10);
				ApiFuture<DocumentSnapshot> future10days = docRef.get();
				DocumentSnapshot document = future10days.get();
				if (document.exists()) {
					LOGGER.info("doc 10days day 10 exist.");
					journeyDay = document.toObject(JourneyDay.class);
					if (journeyDay.getStatus() == 1) {
						LOGGER.info("doc 10days day 10 status == 1.");
						HashMap<String, String> hashMap = new HashMap();
						numDays = 1;
						String nameDay = "Day " + String.valueOf(1);
						hashMap.put("nameDay", nameDay);
						hashMap.put("keyDay", String.valueOf(1));
						LOGGER.info("nameDay: " + nameDay);
						LOGGER.info("keyDay: " + String.valueOf(1));
						listJourneyActive.add(hashMap);
					} else {
						LOGGER.info("doc 10days day 10 status == 0.");
					}
				} else {
					LOGGER.info("doc 10days day 10 not exist.");

					Map<String, Object> data = new HashMap<>();
					data.put("day", 10);
					data.put("journey", "10days");
					data.put("memberId", facebookId);
					data.put("memberName", "");
					data.put("postId", "");
					data.put("status", 0);
					data.put("url", "");
					data.put("uid", uid);
					data.put("accountId", facebookId);
					data.put("createdAt", System.currentTimeMillis() / 1000);
					data.put("updatedAt", System.currentTimeMillis() / 1000);
					ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay").document(docId10days10).set(data);
				}

				for (Journey journey : dayList) {
					if (journey.getName().equals("21days")) {
						if (Integer.parseInt(journey.getDay()) > numDays) {
							HashMap<String, String> hashMap = new HashMap();
							hashMap.put("keyDay", journey.getDay());
							LOGGER.info("keyDay: " + journey.getDay());
							String nameDay = "Day " + journey.getDay();
							hashMap.put("nameDay", nameDay);
							LOGGER.info("nameDay: " + nameDay);
							listDayLock.add(hashMap);
						}
					}
				}
			} else {
				for (DocumentSnapshot document : journeyDocuments) {
					journeyDay = document.toObject(JourneyDay.class);
					HashMap<String, String> hashMap = new HashMap();
					String nameDay = "Day " + String.valueOf(journeyDay.getDay());
					hashMap.put("nameDay", nameDay);
					hashMap.put("keyDay", String.valueOf(journeyDay.getDay()));
					LOGGER.info("nameDay: " + nameDay);
					LOGGER.info("keyDay: " + String.valueOf(journeyDay.getDay()));
					listJourneyActive.add(hashMap);
				}
				if (numDays < 21) {
					String docIdNext = "21days" + String.valueOf(numDays + 1) + facebookId;
					DocumentReference docRef = db.collection("JourneyDay").document(docIdNext);
					ApiFuture<DocumentSnapshot> futureCheck = docRef.get();
					DocumentSnapshot document = futureCheck.get();
					if (document.exists()) {
						LOGGER.info("doc Journey Day Check exist!");
					} else {

						Map<String, Object> data = new HashMap<>();
						data.put("day", numDays + 1);
						data.put("journey", "21days");
						data.put("memberId", facebookId);
						data.put("memberName", "");
						data.put("postId", "");
						data.put("status", 0);
						data.put("url", "");
						data.put("uid", uid);
						data.put("accountId", facebookId);
						data.put("createdAt", System.currentTimeMillis() / 1000);
						data.put("updatedAt", System.currentTimeMillis() / 1000);
						ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay").document(docIdNext).set(data);
					}

					HashMap<String, String> hashMapNext = new HashMap();
					String nameDayNext = "Day " + String.valueOf(numDays + 1);
					hashMapNext.put("nameDay", nameDayNext);
					hashMapNext.put("keyDay", String.valueOf(numDays + 1));
					LOGGER.info("nameDay: " + nameDayNext);
					LOGGER.info("keyDay: " + String.valueOf(numDays + 1));
					listJourneyActive.add(hashMapNext);

					// create doc 45days day 1
					String docIdNext45days1 = "45days" + String.valueOf(1) + facebookId;
					DocumentReference docRef45days1 = db.collection("JourneyDay").document(docIdNext45days1);
					ApiFuture<DocumentSnapshot> futureCheck45days1 = docRef45days1.get();
					DocumentSnapshot document45days1 = futureCheck45days1.get();
					if (document45days1.exists()) {
						LOGGER.info("doc 45days day1 Journey Day Check exist!");
					} else {
						LOGGER.info("doc 45days day 1 Journey Day Check not exist!");
						Map<String, Object> data = new HashMap<>();
						data.put("day", 1);
						data.put("journey", "45days");
						data.put("memberId", facebookId);
						data.put("memberName", "");
						data.put("postId", "");
						data.put("status", 0);
						data.put("url", "");
						data.put("uid", uid);
						data.put("accountId", facebookId);
						data.put("createdAt", System.currentTimeMillis() / 1000);
						data.put("updatedAt", System.currentTimeMillis() / 1000);
						ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay").document(docIdNext45days1)
								.set(data);
					}
				}
				for (Journey journey : dayList) {
					if (journey.getName().equals("21days")) {
						if (Integer.parseInt(journey.getDay()) > (numDays + 1)) {
							HashMap<String, String> hashMap = new HashMap();
							hashMap.put("keyDay", journey.getDay());
							LOGGER.info("keyDay: " + journey.getDay());
							String nameDay = "Day " + journey.getDay();
							hashMap.put("nameDay", nameDay);
							LOGGER.info("nameDay: " + nameDay);
							listDayLock.add(hashMap);
						}
					}
				}

			}
		}

		model.addAttribute("activeDays", listJourneyActive);
		model.addAttribute("lockDays", listDayLock);
		return "journeys/21days";
	}

	@RequestMapping(value = "/journey/45days", method = RequestMethod.GET)
	public String evt45days(Model model, HttpServletRequest request)
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
		LOGGER.info("idToken: " + idToken);
		List<HashMap<String, String>> listJourneyActive = new ArrayList<>();
		List<HashMap<String, String>> listDayLock = new ArrayList<>();
		if (idToken == null) {
			LOGGER.info("idToken null.");
			for (Journey journey : dayList) {
				if (journey.getName().equals("45days")) {
					if (Integer.parseInt(journey.getDay()) > 0) {
						HashMap<String, String> hashMap = new HashMap();
						hashMap.put("keyDay", journey.getDay());
						LOGGER.info("keyDay: " + journey.getDay());
						String nameDay = "Day " + journey.getDay();
						hashMap.put("nameDay", nameDay);
						LOGGER.info("nameDay: " + nameDay);
						listDayLock.add(hashMap);
					}
				}
			}
		} else {
			LOGGER.info("idToken != null");
			FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
			String uid = decodedToken.getUid();
			Firestore db = FirestoreClient.getFirestore();
			// get account id
			ApiFuture<QuerySnapshot> future = db.collection("Account").whereEqualTo("uid", uid).get();
			List<QueryDocumentSnapshot> documents = future.get().getDocuments();
			String accountId = null;
			for (DocumentSnapshot document : documents) {
				accountId = document.getId();
			}
			LOGGER.info("account Id: " + accountId);
			// get list day learned
			ApiFuture<QuerySnapshot> futureJourneyDay = db.collection("JourneyDay").whereEqualTo("accountId", accountId)
					.whereEqualTo("journey", "45days").whereEqualTo("status", 1).get();
			List<QueryDocumentSnapshot> journeyDocuments = futureJourneyDay.get().getDocuments();
			int numDays = journeyDocuments.size();
			LOGGER.info("numDays size: " + numDays);
			JourneyDay journeyDay = new JourneyDay();
			// get account by uid
			ApiFuture<QuerySnapshot> futureAcc = db.collection("Account").whereEqualTo("uid", uid).get();
			List<QueryDocumentSnapshot> accDocuments = futureAcc.get().getDocuments();
			String facebookId = null;
			for (DocumentSnapshot docAcc : accDocuments) {
				facebookId = docAcc.getId();
			}
			if (numDays == 0) {
				// check status 21days day 21
				String docId21days21 = "21days21" + accountId;
				LOGGER.info("docId21days21: " + docId21days21);
				DocumentReference docRef = db.collection("JourneyDay").document(docId21days21);
				ApiFuture<DocumentSnapshot> future21days = docRef.get();
				DocumentSnapshot document = future21days.get();
				if (document.exists()) {
					LOGGER.info("doc 21days day 21 exist.");
					journeyDay = document.toObject(JourneyDay.class);
					if (journeyDay.getStatus() == 1) {
						LOGGER.info("doc 21days day 21 status == 1.");
						HashMap<String, String> hashMap = new HashMap();
						numDays = 1;
						String nameDay = "Day " + String.valueOf(1);
						hashMap.put("nameDay", nameDay);
						hashMap.put("keyDay", String.valueOf(1));
						LOGGER.info("nameDay: " + nameDay);
						LOGGER.info("keyDay: " + String.valueOf(1));
						listJourneyActive.add(hashMap);
					} else {
						LOGGER.info("doc 21days day 21 status == 0.");
					}
				} else {
					LOGGER.info("doc 21days day 21 not exist.");

					Map<String, Object> data = new HashMap<>();
					data.put("day", 21);
					data.put("journey", "21days");
					data.put("memberId", facebookId);
					data.put("memberName", "");
					data.put("postId", "");
					data.put("status", 0);
					data.put("url", "");
					data.put("uid", uid);
					data.put("accountId", facebookId);
					data.put("createdAt", System.currentTimeMillis() / 1000);
					data.put("updatedAt", System.currentTimeMillis() / 1000);
					ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay").document(docId21days21).set(data);
				}

				for (Journey journey : dayList) {
					if (journey.getName().equals("45days")) {
						if (Integer.parseInt(journey.getDay()) > numDays) {
							HashMap<String, String> hashMap = new HashMap();
							hashMap.put("keyDay", journey.getDay());
							LOGGER.info("keyDay: " + journey.getDay());
							String nameDay = "Day " + journey.getDay();
							hashMap.put("nameDay", nameDay);
							LOGGER.info("nameDay: " + nameDay);
							listDayLock.add(hashMap);
						}
					}
				}
			} else {
				for (DocumentSnapshot document : journeyDocuments) {
					journeyDay = document.toObject(JourneyDay.class);
					HashMap<String, String> hashMap = new HashMap();
					String nameDay = "Day " + String.valueOf(journeyDay.getDay());
					hashMap.put("nameDay", nameDay);
					hashMap.put("keyDay", String.valueOf(journeyDay.getDay()));
					LOGGER.info("nameDay: " + nameDay);
					LOGGER.info("keyDay: " + String.valueOf(journeyDay.getDay()));
					listJourneyActive.add(hashMap);
				}
				if (numDays < 45) {
					String docIdNext = "45days" + String.valueOf(numDays + 1) + facebookId;
					DocumentReference docRef = db.collection("JourneyDay").document(docIdNext);
					ApiFuture<DocumentSnapshot> futureCheck = docRef.get();
					DocumentSnapshot document = futureCheck.get();
					if (document.exists()) {
						LOGGER.info("doc Journey Day Check exist!");
					} else {

						Map<String, Object> data = new HashMap<>();
						data.put("day", numDays + 1);
						data.put("journey", "45days");
						data.put("memberId", facebookId);
						data.put("memberName", "");
						data.put("postId", "");
						data.put("status", 0);
						data.put("url", "");
						data.put("uid", uid);
						data.put("accountId", facebookId);
						data.put("createdAt", System.currentTimeMillis() / 1000);
						data.put("updatedAt", System.currentTimeMillis() / 1000);
						ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay").document(docIdNext).set(data);
					}

					HashMap<String, String> hashMapNext = new HashMap();
					String nameDayNext = "Day " + String.valueOf(numDays + 1);
					hashMapNext.put("nameDay", nameDayNext);
					hashMapNext.put("keyDay", String.valueOf(numDays + 1));
					LOGGER.info("nameDay: " + nameDayNext);
					LOGGER.info("keyDay: " + String.valueOf(numDays + 1));
					listJourneyActive.add(hashMapNext);

					// create doc 90days day 1
					String docIdNext90days1 = "90days" + String.valueOf(1) + facebookId;
					DocumentReference docRef90days1 = db.collection("JourneyDay").document(docIdNext90days1);
					ApiFuture<DocumentSnapshot> futureCheck90days1 = docRef90days1.get();
					DocumentSnapshot document90days1 = futureCheck90days1.get();
					if (document90days1.exists()) {
						LOGGER.info("doc 90days day1 Journey Day Check exist!");
					} else {
						LOGGER.info("doc 90days day 1 Journey Day Check not exist!");
						Map<String, Object> data = new HashMap<>();
						data.put("day", 1);
						data.put("journey", "90days");
						data.put("memberId", facebookId);
						data.put("memberName", "");
						data.put("postId", "");
						data.put("status", 0);
						data.put("url", "");
						data.put("uid", uid);
						data.put("accountId", facebookId);
						data.put("createdAt", System.currentTimeMillis() / 1000);
						data.put("updatedAt", System.currentTimeMillis() / 1000);
						ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay").document(docIdNext90days1)
								.set(data);
					}
				}
				for (Journey journey : dayList) {
					if (journey.getName().equals("45days")) {
						if (Integer.parseInt(journey.getDay()) > (numDays + 1)) {
							HashMap<String, String> hashMap = new HashMap();
							hashMap.put("keyDay", journey.getDay());
							LOGGER.info("keyDay: " + journey.getDay());
							String nameDay = "Day " + journey.getDay();
							hashMap.put("nameDay", nameDay);
							LOGGER.info("nameDay: " + nameDay);
							listDayLock.add(hashMap);
						}
					}
				}
			}

		}

		model.addAttribute("activeDays", listJourneyActive);
		model.addAttribute("lockDays", listDayLock);
		return "journeys/45days";
	}

	@RequestMapping(value = "/journey/90days", method = RequestMethod.GET)
	public String evt90days(Model model, HttpServletRequest request)
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
		LOGGER.info("idToken: " + idToken);
		List<HashMap<String, String>> listJourneyActive = new ArrayList<>();
		List<HashMap<String, String>> listDayLock = new ArrayList<>();
		if (idToken == null) {
			LOGGER.info("idToken null.");
			for (Journey journey : dayList) {
				if (journey.getName().equals("90days")) {
					if (Integer.parseInt(journey.getDay()) > 0) {
						HashMap<String, String> hashMap = new HashMap();
						hashMap.put("keyDay", journey.getDay());
						LOGGER.info("keyDay: " + journey.getDay());
						String nameDay = "Day " + journey.getDay();
						hashMap.put("nameDay", nameDay);
						LOGGER.info("nameDay: " + nameDay);
						listDayLock.add(hashMap);
					}
				}
			}
		} else {
			LOGGER.info("idToken != null");
			FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
			String uid = decodedToken.getUid();
			Firestore db = FirestoreClient.getFirestore();
			// get account id
			ApiFuture<QuerySnapshot> future = db.collection("Account").whereEqualTo("uid", uid).get();
			List<QueryDocumentSnapshot> documents = future.get().getDocuments();
			String accountId = null;
			for (DocumentSnapshot document : documents) {
				accountId = document.getId();
			}
			LOGGER.info("account Id: " + accountId);
			// get list day learned
			ApiFuture<QuerySnapshot> futureJourneyDay = db.collection("JourneyDay").whereEqualTo("accountId", accountId)
					.whereEqualTo("journey", "90days").whereEqualTo("status", 1).get();
			List<QueryDocumentSnapshot> journeyDocuments = futureJourneyDay.get().getDocuments();
			int numDays = journeyDocuments.size();
			LOGGER.info("numDays size: " + numDays);
			JourneyDay journeyDay = new JourneyDay();
			// get account by uid
			ApiFuture<QuerySnapshot> futureAcc = db.collection("Account").whereEqualTo("uid", uid).get();
			List<QueryDocumentSnapshot> accDocuments = futureAcc.get().getDocuments();
			String facebookId = null;
			for (DocumentSnapshot docAcc : accDocuments) {
				facebookId = docAcc.getId();
			}
			if (numDays == 0) {
				// check status 45days day 45
				String docId45days45 = "45days45" + accountId;
				LOGGER.info("docId45days45: " + docId45days45);
				DocumentReference docRef = db.collection("JourneyDay").document(docId45days45);
				ApiFuture<DocumentSnapshot> future45days = docRef.get();
				DocumentSnapshot document = future45days.get();
				if (document.exists()) {
					LOGGER.info("doc 45days day 45 exist.");
					journeyDay = document.toObject(JourneyDay.class);
					if (journeyDay.getStatus() == 1) {
						LOGGER.info("doc 45days day 45 status == 1.");
						HashMap<String, String> hashMap = new HashMap();
						numDays = 1;
						String nameDay = "Day " + String.valueOf(1);
						hashMap.put("nameDay", nameDay);
						hashMap.put("keyDay", String.valueOf(1));
						LOGGER.info("nameDay: " + nameDay);
						LOGGER.info("keyDay: " + String.valueOf(1));
						listJourneyActive.add(hashMap);
					} else {
						LOGGER.info("doc 45days day 45 status == 0.");
					}
				} else {
					LOGGER.info("doc 45days day 45 not exist.");

					Map<String, Object> data = new HashMap<>();
					data.put("day", 45);
					data.put("journey", "45days");
					data.put("memberId", facebookId);
					data.put("memberName", "");
					data.put("postId", "");
					data.put("status", 0);
					data.put("url", "");
					data.put("uid", uid);
					data.put("accountId", facebookId);
					data.put("createdAt", System.currentTimeMillis() / 1000);
					data.put("updatedAt", System.currentTimeMillis() / 1000);
					ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay").document(docId45days45).set(data);
				}

				for (Journey journey : dayList) {
					if (journey.getName().equals("90days")) {
						if (Integer.parseInt(journey.getDay()) > numDays) {
							HashMap<String, String> hashMap = new HashMap();
							hashMap.put("keyDay", journey.getDay());
							LOGGER.info("keyDay: " + journey.getDay());
							String nameDay = "Day " + journey.getDay();
							hashMap.put("nameDay", nameDay);
							LOGGER.info("nameDay: " + nameDay);
							listDayLock.add(hashMap);
						}
					}
				}
			} else {
				for (DocumentSnapshot document : journeyDocuments) {
					journeyDay = document.toObject(JourneyDay.class);
					HashMap<String, String> hashMap = new HashMap();
					String nameDay = "Day " + String.valueOf(journeyDay.getDay());
					hashMap.put("nameDay", nameDay);
					hashMap.put("keyDay", String.valueOf(journeyDay.getDay()));
					LOGGER.info("nameDay: " + nameDay);
					LOGGER.info("keyDay: " + String.valueOf(journeyDay.getDay()));
					listJourneyActive.add(hashMap);
				}
				if (numDays < 90) {
					String docIdNext = "90days" + String.valueOf(numDays + 1) + facebookId;
					DocumentReference docRef = db.collection("JourneyDay").document(docIdNext);
					ApiFuture<DocumentSnapshot> futureCheck = docRef.get();
					DocumentSnapshot document = futureCheck.get();
					if (document.exists()) {
						LOGGER.info("doc Journey Day Check exist!");
					} else {

						Map<String, Object> data = new HashMap<>();
						data.put("day", numDays + 1);
						data.put("journey", "90days");
						data.put("memberId", facebookId);
						data.put("memberName", "");
						data.put("postId", "");
						data.put("status", 0);
						data.put("url", "");
						data.put("uid", uid);
						data.put("accountId", facebookId);
						data.put("createdAt", System.currentTimeMillis() / 1000);
						data.put("updatedAt", System.currentTimeMillis() / 1000);
						ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay").document(docIdNext).set(data);
					}

					HashMap<String, String> hashMapNext = new HashMap();
					String nameDayNext = "Day " + String.valueOf(numDays + 1);
					hashMapNext.put("nameDay", nameDayNext);
					hashMapNext.put("keyDay", String.valueOf(numDays + 1));
					LOGGER.info("nameDay: " + nameDayNext);
					LOGGER.info("keyDay: " + String.valueOf(numDays + 1));
					listJourneyActive.add(hashMapNext);

				}
				for (Journey journey : dayList) {
					if (journey.getName().equals("90days")) {
						if (Integer.parseInt(journey.getDay()) > (numDays + 1)) {
							HashMap<String, String> hashMap = new HashMap();
							hashMap.put("keyDay", journey.getDay());
							LOGGER.info("keyDay: " + journey.getDay());
							String nameDay = "Day " + journey.getDay();
							hashMap.put("nameDay", nameDay);
							LOGGER.info("nameDay: " + nameDay);
							listDayLock.add(hashMap);
						}
					}
				}
			}

		}

		model.addAttribute("activeDays", listJourneyActive);
		model.addAttribute("lockDays", listDayLock);
		return "journeys/90days";
	}

	@RequestMapping(value = "openLockDay", method = RequestMethod.POST)
	public void checkOldDay(Model model, @RequestParam String url,
			@CookieValue(value = "idToken", required = true) String idToken, @RequestParam String journey,
			@RequestParam String numDay, HttpServletResponse response) throws FirebaseAuthException, JsonParseException,
			JsonMappingException, IOException, InterruptedException, ExecutionException {
		LOGGER.info("urlvideo open day: " + url);
		FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
		String uid = decodedToken.getUid();
		Firestore db = FirestoreClient.getFirestore();
		// get account by uid
		ApiFuture<QuerySnapshot> futureAcc = db.collection("Account").whereEqualTo("uid", uid).get();
		List<QueryDocumentSnapshot> accDocuments = futureAcc.get().getDocuments();
		String facebookId = null;
		for (DocumentSnapshot document : accDocuments) {
			facebookId = document.getId();
		}
		if (facebookId == null) {
			LOGGER.info("facebookId null");
			return;
		}
		// check exist last day
		String docLast = journey + "days" + numDay + facebookId;
		LOGGER.info("docLast: " + docLast);
		DocumentReference docRefLast = db.collection("JourneyDay").document(docLast);
		ApiFuture<DocumentSnapshot> futureLast = docRefLast.get();
		DocumentSnapshot documentLast = futureLast.get();
		if (!documentLast.exists()) {
			LOGGER.info("last day not exist.");
			response.setStatus(406);
			return;
		}
		Document doc = Jsoup.connect(url).get();
		int begin = doc.html().indexOf("content_owner_id_new&quot;:&quot;")
				+ "content_owner_id_new&quot;:&quot;".length();
		int end = doc.html().indexOf("&quot;", begin);
		String urlContent = doc.select("meta[property=\"og:url\"]").attr("content");
		int last = urlContent.lastIndexOf("=");
		String postId = urlContent.substring(last + 1);
		String memberId = doc.html().substring(begin, end);
		LOGGER.info("postId: " + postId);
		LOGGER.info("memberId: " + memberId);
		String memberName = doc.select("meta[property=\"og:title\"]").attr("content");
		String _journeyDay = doc.select(".bo p").text();
		Pattern p = Pattern.compile("(\\d+)(/|\\.)(\\d+)");
		Matcher m = p.matcher(_journeyDay.toLowerCase());
		String journeyDay = null;
		String journeyName = null;
		String day = null;
		if (m.find()) {
			String spliter[] = m.group().split("/|\\.");
			int current = Integer.parseInt(spliter[0]);
			int total = Integer.parseInt(spliter[1]);

			journeyDay = current + "/" + total;
			int lenght = journeyDay.length();
			day = journeyDay.substring(0, journeyDay.indexOf("/"));
			journeyName = journeyDay.substring((journeyDay.indexOf("/") + 1), lenght);
		}
		LOGGER.info("day: " + day);
		LOGGER.info("journeyName: " + journeyName);
		LOGGER.info("memberName: " + memberName);
		LOGGER.info("journey param: " + journey);
		LOGGER.info("numDay param: " + numDay);
		if (journeyName == null || day == null) {
			LOGGER.info("journeyName null || day null.");
			response.setStatus(404);
			return;
		}
		if (journeyName == null && day == null) {
			LOGGER.info("journeyName null && day null.");
			response.setStatus(404);
			return;
		}
		if (!journeyName.equals(journey)) {
			LOGGER.info("journey in post # journey uri");
			response.setStatus(404);
			return;
		}
		if (!day.equals(numDay)) {
			LOGGER.info("day in post # day uri");
			response.setStatus(404);
			return;
		}

		// end check
		// check name account
		String docAccount = facebookId;
		DocumentReference docRefAccount = db.collection("Account").document(docAccount);
		ApiFuture<DocumentSnapshot> futureAccount = docRefAccount.get();
		DocumentSnapshot document = futureAccount.get();
		Account account = document.toObject(Account.class);
		if (!account.getDisplayName().equals(memberName)) {
			LOGGER.info("An cap bai viet cua nguoi khac.");
			response.setStatus(405);
			return;
		} else {
			if (journeyName.equals(journey) && day.equals(numDay)) {
				String docJourneyDay = journey + "days" + numDay + facebookId;
				LOGGER.info("docJourneyDay: " + docJourneyDay);
				DocumentReference docRefJourney = db.collection("JourneyDay").document(docJourneyDay);
				// check document Journey day moment
				ApiFuture<DocumentSnapshot> futureJourneyDay = docRefJourney.get();
				DocumentSnapshot documentJourneyDay = futureJourneyDay.get();
				if (!documentJourneyDay.exists()) {
					Map<String, Object> data = new HashMap<>();
					data.put("day", Integer.parseInt(numDay));
					data.put("journey", journey + "days");
					data.put("memberId", facebookId);
					data.put("memberName", "");
					data.put("postId", "");
					data.put("status", 0);
					data.put("url", "");
					data.put("uid", uid);
					data.put("accountId", facebookId);
					data.put("createdAt", System.currentTimeMillis() / 1000);
					data.put("updatedAt", System.currentTimeMillis() / 1000);
					ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay").document(docJourneyDay).set(data);
				}

				// check url journey day old
				ApiFuture<QuerySnapshot> futureJourney = db.collection("JourneyDay")
						.whereEqualTo("accountId", facebookId).whereEqualTo("url", url).get();
				List<QueryDocumentSnapshot> journeyDocuments = futureJourney.get().getDocuments();
				if (!journeyDocuments.isEmpty()) {
					LOGGER.info("url journey day exits, break.");
					response.setStatus(400);
					return;
				}
				// end check
				Map<String, Object> updates = new HashMap<>();
				updates.put("memberId", memberId);
				updates.put("memberName", memberName);
				updates.put("postId", postId);
				updates.put("status", 1);
				updates.put("url", url);
				updates.put("updatedAt", System.currentTimeMillis() / 1000);
				ApiFuture<WriteResult> futureJD = docRefJourney.update(updates);
				// update Account
				if (document.exists()) {
					// Update an existing document
					LOGGER.info("update memberId in account");
					ApiFuture<WriteResult> future = docRefAccount.update("memberId", memberId);
				} else {
					System.out.println("No such document account!");
				}

				if (journeyName.equals("3") && numDay.equals("3")) {
					LOGGER.info("3days3");
					response.setStatus(200);
					return;
				}
				if (journeyName.equals("5") && numDay.equals("5")) {
					LOGGER.info("5days5");
					response.setStatus(200);
					return;
				}
				if (journeyName.equals("7") && numDay.equals("7")) {
					LOGGER.info("7days7");
					response.setStatus(200);
					return;
				}
				if (journeyName.equals("10") && numDay.equals("10")) {
					LOGGER.info("10days10");
					response.setStatus(200);
					return;
				}
				if (journeyName.equals("21") && numDay.equals("21")) {
					LOGGER.info("21days21");
					response.setStatus(200);
					return;
				}
				if (journeyName.equals("45") && numDay.equals("45")) {
					LOGGER.info("45days45");
					response.setStatus(200);
					return;
				}
				if (journeyName.equals("90") && numDay.equals("90")) {
					LOGGER.info("90days90");
					response.setStatus(200);
					return;
				}
				if ((journeyName.equals("3") && (Integer.parseInt(numDay) < 3))
						|| (journeyName.equals("5") && (Integer.parseInt(numDay) < 5))
						|| (journeyName.equals("7") && (Integer.parseInt(numDay) < 7))
						|| (journeyName.equals("10") && (Integer.parseInt(numDay) < 10))
						|| (journeyName.equals("21") && (Integer.parseInt(numDay) < 21))
						|| (journeyName.equals("45") && (Integer.parseInt(numDay) < 45))
						|| (journeyName.equals("90") && (Integer.parseInt(numDay) < 90))) {
					LOGGER.info(journeyName + "days/" + numDay);
					int dayNumber = Integer.parseInt(numDay) + 1;
					day = String.valueOf(dayNumber);
					response.setStatus(200);
					return;
				}
			} else {
				response.setStatus(404);
				return;
			}
		}
	}

	@RequestMapping(value = "/journey", method = RequestMethod.GET)
	public String evtJourneys(HttpServletRequest request) throws UnsupportedEncodingException {
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

		if (idToken == null) {
			LOGGER.info("idToken null.");
		}
		return "journeys/journey-all";
	}
}
