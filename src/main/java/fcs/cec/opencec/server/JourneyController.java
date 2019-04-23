package fcs.cec.opencec.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

import com.fasterxml.jackson.databind.ObjectMapper;
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

import fcs.cec.opencec.entity.Account;
import fcs.cec.opencec.entity.Journey;
import fcs.cec.opencec.entity.JourneyDay;

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

//	@RequestMapping(value = "journey/3days/1", method = RequestMethod.GET)
//	public String journeyDaysFirst(Model model) {
//		for (Journey journey : dayList) {
//			if (journey.getName().equals("3days") && journey.getDay().equals("1")) {
//				model.addAttribute("journey", journey);
//			} else {
//				break;
//			}
//		}
//
//		return "journeys/journeyDay";
//	}

	@RequestMapping(value = "journey/{name}/{day}", method = RequestMethod.GET)
	public String journeyDays(Model model, @PathVariable("name") String name, @PathVariable("day") String day,
			@CookieValue(value = "idToken", required = false) String idToken)
			throws FirebaseAuthException, InterruptedException, ExecutionException {
		Firestore db = FirestoreOptions.getDefaultInstance().getService();
//		FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
//		String uid = decodedToken.getUid();
		int dayJourney = Integer.parseInt(day);
		LOGGER.info("dayJourney first: " + dayJourney);
		// journey day in 3days
		if (name.equals("3days")) {
			if (Integer.parseInt(day) < 1) {
				LOGGER.info("3days day < 1");
				return "error/404";
			}
			if ((Integer.parseInt(day) > 1) && (Integer.parseInt(day) < 4)) {
				// get facebookId, get uid
				LOGGER.info("idToken: " + idToken);
				FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
				String uid = decodedToken.getUid();
				// get doc id account
				ApiFuture<QuerySnapshot> futureAccount = db.collection("Account").whereEqualTo("uid", uid).get();
				List<QueryDocumentSnapshot> accountDocuments = futureAccount.get().getDocuments();
				String facebookId = null;
				for (DocumentSnapshot document : accountDocuments) {
					System.out.println(document.getId() + " => " + document.toObject(Account.class));
					facebookId = document.getId();
				}

				// create day 2 in journey day 3days
				if (Integer.parseInt(day) == 2) {
					LOGGER.info("3days day == 2");
					// create new journey day
					Map<String, Object> data = new HashMap<>();
					dayJourney = 2;
					data.put("day", dayJourney);
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
						return "error/404";
					}
					// create new journey day
					Map<String, Object> data = new HashMap<>();
					dayJourney = Integer.parseInt(day) + 1;
					LOGGER.info("dayJourney CHECK: " + dayJourney);
					// check day < 4 in 3days
					if (dayJourney < 4) {
						if (dayJourney == 3) {
							data.put("day", 1);
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
					return "error/404";
				}
			}
			if (Integer.parseInt(day) >= 4) {
				LOGGER.info("3days day >= 4");
				return "error/404";
			}
//			if (Integer.parseInt(day) == 1) {
//				LOGGER.info("3days day == 1");
//				// create new journey day
//				Map<String, Object> data = new HashMap<>();
//				dayJourney = Integer.parseInt(day) + 1;
//				data.put("day", dayJourney);
//				data.put("memberId", facebookId);
//				data.put("memberName", "");
//				data.put("postId", "");
//				data.put("status", 0);
//				data.put("url", "");
//				data.put("uid", uid);
//				data.put("accountId", facebookId);
//				data.put("createdAt", System.currentTimeMillis() / 1000);
//				data.put("updatedAt", System.currentTimeMillis() / 1000);
//				String docId = name + String.valueOf(dayJourney) + facebookId;
//				DocumentReference docRefJourneyDay = db.collection("JourneyDay").document(docId);
//				ApiFuture<DocumentSnapshot> futureJourneyDay = docRefJourneyDay.get();
//				DocumentSnapshot documentJourneyDay = futureJourneyDay.get();
//				if (documentJourneyDay.exists()) {
//					LOGGER.info("document JourneyDay eixst!");
//				} else {
//					ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay").document(docId).set(data);
//				}
//			}
		}
		// end journey day in 3days

		// journey day in 5days
		if (name.equals("5days")) {
			// get facebookId, get uid
			FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
			String uid = decodedToken.getUid();
			// get doc id account
			ApiFuture<QuerySnapshot> futureAccount = db.collection("Account").whereEqualTo("uid", uid).get();
			List<QueryDocumentSnapshot> accountDocuments = futureAccount.get().getDocuments();
			String facebookId = null;
			for (DocumentSnapshot document : accountDocuments) {
				System.out.println(document.getId() + " => " + document.toObject(Account.class));
				facebookId = document.getId();
			}

			if (Integer.parseInt(day) < 1) {
				LOGGER.info("5days day < 1");
				return "error/404";
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
						return "error/404";
					}
					// create new journey day
					Map<String, Object> data = new HashMap<>();
					dayJourney = Integer.parseInt(day) + 1;
					// check day < 6 in 5days
					if (dayJourney < 6) {
						if (dayJourney == 5) {
							data.put("day", 1);
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
					return "error/404";
				}
			}
			if (Integer.parseInt(day) >= 6) {
				LOGGER.info("5days day >= 6");
				return "error/404";
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
						return "error/404";
					}
					// create new journey day
					Map<String, Object> data = new HashMap<>();
					dayJourney = Integer.parseInt(day) + 1;
					data.put("day", dayJourney);
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
					return "error/404";
				}
			}
		}
		// end journey day in 5days

		// journey day in 7days
		if (name.equals("7days")) {
			// get facebookId, get uid
			FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
			String uid = decodedToken.getUid();
			// get doc id account
			ApiFuture<QuerySnapshot> futureAccount = db.collection("Account").whereEqualTo("uid", uid).get();
			List<QueryDocumentSnapshot> accountDocuments = futureAccount.get().getDocuments();
			String facebookId = null;
			for (DocumentSnapshot document : accountDocuments) {
				System.out.println(document.getId() + " => " + document.toObject(Account.class));
				facebookId = document.getId();
			}

			if (Integer.parseInt(day) < 1) {
				LOGGER.info("7days day < 1");
				return "error/404";
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
						return "error/404";
					}
					// create new journey day
					Map<String, Object> data = new HashMap<>();
					dayJourney = Integer.parseInt(day) + 1;
					// check day < 8 in 7days
					if (dayJourney < 8) {
						if (dayJourney == 7) {
							data.put("day", 1);
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
					return "error/404";
				}
			}
			if (Integer.parseInt(day) >= 8) {
				LOGGER.info("7days day >= 8");
				return "error/404";
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
						return "error/404";
					}
					// create new journey day
					Map<String, Object> data = new HashMap<>();
					dayJourney = Integer.parseInt(day) + 1;
					data.put("day", dayJourney);
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
					return "error/404";
				}
			}
		}
		// end journey day in 7days

		// journey day in 10days
		if (name.equals("10days")) {
			// get facebookId, get uid
			FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
			String uid = decodedToken.getUid();
			// get doc id account
			ApiFuture<QuerySnapshot> futureAccount = db.collection("Account").whereEqualTo("uid", uid).get();
			List<QueryDocumentSnapshot> accountDocuments = futureAccount.get().getDocuments();
			String facebookId = null;
			for (DocumentSnapshot document : accountDocuments) {
				System.out.println(document.getId() + " => " + document.toObject(Account.class));
				facebookId = document.getId();
			}

			if (Integer.parseInt(day) < 1) {
				LOGGER.info("10days day < 1");
				return "error/404";
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
						return "error/404";
					}
					// create new journey day
					Map<String, Object> data = new HashMap<>();
					dayJourney = Integer.parseInt(day) + 1;
					// check day < 11 in 10days
					if (dayJourney < 11) {
						if (dayJourney == 10) {
							data.put("day", 1);
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
					return "error/404";
				}
			}
			if (Integer.parseInt(day) >= 11) {
				LOGGER.info("10days day >= 11");
				return "error/404";
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
						return "error/404";
					}
					// create new journey day
					Map<String, Object> data = new HashMap<>();
					dayJourney = Integer.parseInt(day) + 1;
					data.put("day", dayJourney);
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
					return "error/404";
				}
			}
		}
		// end journey day in 10days

		// journey day in 21days
		if (name.equals("21days")) {
			// get facebookId, get uid
			FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
			String uid = decodedToken.getUid();
			// get doc id account
			ApiFuture<QuerySnapshot> futureAccount = db.collection("Account").whereEqualTo("uid", uid).get();
			List<QueryDocumentSnapshot> accountDocuments = futureAccount.get().getDocuments();
			String facebookId = null;
			for (DocumentSnapshot document : accountDocuments) {
				System.out.println(document.getId() + " => " + document.toObject(Account.class));
				facebookId = document.getId();
			}

			if (Integer.parseInt(day) < 1) {
				LOGGER.info("21days day < 1");
				return "error/404";
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
						return "error/404";
					}
					// create new journey day
					Map<String, Object> data = new HashMap<>();
					dayJourney = Integer.parseInt(day) + 1;
					// check day < 22 in 21days
					if (dayJourney < 22) {
						if (dayJourney == 21) {
							data.put("day", 1);
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
					return "error/404";
				}
			}
			if (Integer.parseInt(day) >= 22) {
				LOGGER.info("21days day >= 22");
				return "error/404";
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
						return "error/404";
					}
					// create new journey day
					Map<String, Object> data = new HashMap<>();
					dayJourney = Integer.parseInt(day) + 1;
					data.put("day", dayJourney);
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
					return "error/404";
				}
			}
		}
		// end journey day in 21days

		// journey day in 45days
		if (name.equals("45days")) {
			// get facebookId, get uid
			FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
			String uid = decodedToken.getUid();
			// get doc id account
			ApiFuture<QuerySnapshot> futureAccount = db.collection("Account").whereEqualTo("uid", uid).get();
			List<QueryDocumentSnapshot> accountDocuments = futureAccount.get().getDocuments();
			String facebookId = null;
			for (DocumentSnapshot document : accountDocuments) {
				System.out.println(document.getId() + " => " + document.toObject(Account.class));
				facebookId = document.getId();
			}

			if (Integer.parseInt(day) < 1) {
				LOGGER.info("45days day < 1");
				return "error/404";
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
						return "error/404";
					}
					// create new journey day
					Map<String, Object> data = new HashMap<>();
					dayJourney = Integer.parseInt(day) + 1;
					// check day < 46 in 45days
					if (dayJourney < 46) {
						if (dayJourney == 45) {
							data.put("day", 1);
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
					return "error/404";
				}
			}
			if (Integer.parseInt(day) >= 46) {
				LOGGER.info("45days day >= 46");
				return "error/404";
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
						return "error/404";
					}
					// create new journey day
					Map<String, Object> data = new HashMap<>();
					dayJourney = Integer.parseInt(day) + 1;
					data.put("day", dayJourney);
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
					return "error/404";
				}
			}
		}
		// end journey day in 45days

		// journey day in 90days
		if (name.equals("90days")) {
			// get facebookId, get uid
			FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
			String uid = decodedToken.getUid();
			// get doc id account
			ApiFuture<QuerySnapshot> futureAccount = db.collection("Account").whereEqualTo("uid", uid).get();
			List<QueryDocumentSnapshot> accountDocuments = futureAccount.get().getDocuments();
			String facebookId = null;
			for (DocumentSnapshot document : accountDocuments) {
				System.out.println(document.getId() + " => " + document.toObject(Account.class));
				facebookId = document.getId();
			}

			if (Integer.parseInt(day) < 1) {
				LOGGER.info("90days day < 1");
				return "error/404";
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
						return "error/404";
					}
					// create new journey day
					Map<String, Object> data = new HashMap<>();
					dayJourney = Integer.parseInt(day) + 1;
					// check day < 91 in 90days
					if (dayJourney < 91) {
//						if (dayJourney == 90) {
//							data.put("day", 1);
//							data.put("memberId", facebookId);
//							data.put("memberName", "");
//							data.put("postId", "");
//							data.put("status", 0);
//							data.put("url", "");
//							data.put("uid", uid);
//							data.put("accountId", facebookId);
//							data.put("createdAt", System.currentTimeMillis() / 1000);
//							data.put("updatedAt", System.currentTimeMillis() / 1000);
//							String docId = "90days1" + facebookId;
//							DocumentReference docRefJourneyDay = db.collection("JourneyDay").document(docId);
//							ApiFuture<DocumentSnapshot> futureJourneyDay = docRefJourneyDay.get();
//							DocumentSnapshot documentJourneyDay = futureJourneyDay.get();
//							if (documentJourneyDay.exists()) {
//								LOGGER.info("document JourneyDay exist!");
//							} else {
//								ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay").document(docId)
//										.set(data);
//							}
//						}
						data.put("day", dayJourney);
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
					return "error/404";
				}
			}
			if (Integer.parseInt(day) >= 91) {
				LOGGER.info("90days day >= 91");
				return "error/404";
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
						return "error/404";
					}
					// create new journey day
					Map<String, Object> data = new HashMap<>();
					dayJourney = Integer.parseInt(day) + 1;
					data.put("day", dayJourney);
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
					return "error/404";
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
	public void checkJourneyDay(Model model, @RequestParam String url, @CookieValue("facebookId") String facebookId,
			@RequestParam String journey, @RequestParam String numDay, HttpServletResponse response)
			throws IOException, FirebaseAuthException, InterruptedException, ExecutionException {
//		FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
//		String uid = decodedToken.getUid();
		Firestore db = FirestoreOptions.getDefaultInstance().getService();
		Document doc = Jsoup.connect(url).get();
		String object = doc.select("#m_story_permalink_view .bb").attr("data-ft");
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> map = mapper.readValue(object, Map.class);
		String postId = (String) map.get("top_level_post_id");
		String memberId = (String) map.get("content_owner_id_new");
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
		LOGGER.info("journey param: " + journey);
		LOGGER.info("numDay param: " + numDay);
		LOGGER.info("day: " + day);
		LOGGER.info("dayJourney: " + journeyName);
		if (journeyName == null || day == null) {
			LOGGER.info("journeyName null || day null.");
			response.setStatus(404);
		}
		if (journeyName == null && day == null) {
			LOGGER.info("journeyName null && day null.");
			response.setStatus(404);
		}
		if (!journeyName.equals(journey)) {
			LOGGER.info("journey in post # journey uri");
			response.setStatus(404);
		}
		if (!day.equals(numDay)) {
			LOGGER.info("day in post # day uri");
			response.setStatus(404);
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
		} else {
			if (journeyName.equals(journey) && day.equals(numDay)) {
				LOGGER.info("check ok");
				String docJourneyDay = journey + "days" + numDay + facebookId;
				LOGGER.info("docJourneyDay: " + docJourneyDay);
				DocumentReference docRefJourney = db.collection("JourneyDay").document(docJourneyDay);
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
				}
				if (journeyName.equals("5") && numDay.equals("5")) {
					LOGGER.info("5days5");
					uri = "/journey/7days/1";
					response.getWriter().println(uri);
					response.setStatus(200);
				}
				if (journeyName.equals("7") && numDay.equals("7")) {
					LOGGER.info("7days7");
					uri = "/journey/10days/1";
					response.getWriter().println(uri);
					response.setStatus(200);
				}
				if (journeyName.equals("10") && numDay.equals("10")) {
					LOGGER.info("10days10");
					uri = "/journey/21days/1";
					response.getWriter().println(uri);
					response.setStatus(200);
				}
				if (journeyName.equals("21") && numDay.equals("21")) {
					LOGGER.info("21days21");
					uri = "/journey/45days/1";
					response.getWriter().println(uri);
					response.setStatus(200);
				}
				if (journeyName.equals("45") && numDay.equals("45")) {
					LOGGER.info("45days45");
					uri = "/journey/90days/1";
					response.getWriter().println(uri);
					response.setStatus(200);
				}
				if (journeyName.equals("90") && numDay.equals("90")) {
					LOGGER.info("90days90");
					uri = "/journey/90days/90";
					response.getWriter().println(uri);
					response.setStatus(200);
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
				}
			}
		}
	}
}
