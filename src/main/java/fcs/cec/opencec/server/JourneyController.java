package fcs.cec.opencec.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

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

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;

import fcs.cec.opencec.entity.Journey;
import fcs.cec.opencec.entity.JourneyDay;

@Controller
public class JourneyController {
	private static final Logger LOGGER = LoggerFactory.getLogger(JourneyController.class);
	static ArrayList<Journey> dayList = new ArrayList<Journey>();
	static {
		Document doc = null;
		String url = "https://script.googleusercontent.com/macros/echo?user_content_key=AH8p3WxEhxDFhaufV3L3moVwYAykzqrPyDsk6W5Nlh3M7uaqUwZuxSghoqQyW3kPOAnJS04oyJHl2F0LjZzr8dbUbiwZjhHem5_BxDlH2jW0nuo2oDemN9CCS2h10ox_1xSncGQajx_ryfhECjZEnCT0QRJ7P_-LtV3tAd8_b_dUnbO1rEvbeLLB2eAoIGhp1hENMaacOI9TktsviLkDHJlUq1JAmpDs&lib=MmSKrXssQcdpiSXxZX7nm1QZVzjmXS3D2";
		try {
			doc = Jsoup.connect(url).timeout(30000).get();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		Elements elements = doc.select("day");
		Element ePdf = null;
		Element videoMp4 = null;
		String name = null;
		String pdf = null;
		String video = null;
		Journey journey = null;
		for (Element element : elements) {
			name = element.attr("name");
			ePdf = element.child(0);
			pdf = ePdf.text();
			videoMp4 = element.child(1);
			video = videoMp4.text();
			journey = new Journey(name, pdf, video);
			dayList.add(journey);
		}
	}

	@RequestMapping(value = "journey/{name}/{day}", method = RequestMethod.GET)
	public String journeyDays(Model model, @PathVariable("name") String name, @PathVariable("day") String day,
			@CookieValue("idToken") String idToken, @CookieValue("facebookId") String facebookId)
			throws FirebaseAuthException, InterruptedException, ExecutionException {
		Firestore db = FirestoreOptions.getDefaultInstance().getService();
		FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
		String uid = decodedToken.getUid();
		int dayJourney = Integer.parseInt(day);
		LOGGER.info("dayJourney first: " + dayJourney);
		// journey day in 3days
		if (name.equals("3days")) {
			if (Integer.parseInt(day) < 1) {
				LOGGER.info("3days day < 1");
				return "error/404";
			}
			if ((Integer.parseInt(day) > 1) && (Integer.parseInt(day) < 4)) {
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
			if (Integer.parseInt(day) == 1) {
				LOGGER.info("3days day == 1");
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
			}
		}
		// end journey day in 3days

		// journey day in 5days
		if (name.equals("5days")) {
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
				// check day 5 in journeyday 5days
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

		String video = null;
		int indexStartJourneyDay = 0;
		int indexEndJourneyDay = 0;
		String storageJourneyName = null;
		int indexEndStorageJourneyName = 0;
		String journeyName = null;
		// get journey by journey name, by day
		for (Journey journey : dayList) {
			if (journey.getName().equals(day)) {
				LOGGER.info("journey get day: " + journey.getName());
				LOGGER.info("day param: " + day);
				video = journey.getVideoURL();
				indexStartJourneyDay = video.indexOf("opencec.appspot.com/");
				indexEndJourneyDay = video.indexOf("/day");
				storageJourneyName = video.substring(indexStartJourneyDay, indexEndJourneyDay);
				indexEndStorageJourneyName = storageJourneyName.indexOf("/");
				journeyName = storageJourneyName.substring(indexEndStorageJourneyName + 1);
				LOGGER.info("journeyName: " + journeyName);
				LOGGER.info("journey name: " + name);
				if (!journeyName.equals(name)) {
					return "error/404";
				}
				model.addAttribute("journey", journey);
			}
		}

		return "journeys/journeyDay";
	}
}
