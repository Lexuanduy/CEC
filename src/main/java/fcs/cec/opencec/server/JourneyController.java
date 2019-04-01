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

		// check journey day
		if (name.equals("3days")) {
			LOGGER.info("journey day 3 days.");
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
					dayJourney = dayJourney + 1;
					// check day < 4 in 3days
					if (dayJourney < 4) {
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
							LOGGER.info("document LessonMember eixst!");
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
				dayJourney = dayJourney + 1;
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
					LOGGER.info("document LessonMember eixst!");
				} else {
					ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay").document(docId).set(data);
				}
			}
		}

		String video = null;
		int indexStartJourneyDay = 0;
		int indexEndJourneyDay = 0;
		String storageJourneyName = null;
		int indexEndStorageJourneyName = 0;
		String journeyName = null;
		// get journey by journey name, by day
		for (Journey journey : dayList) {
			if (journey.getName().equals(day)) {
				video = journey.getVideoURL();
				indexStartJourneyDay = video.indexOf("opencec.appspot.com/");
				indexEndJourneyDay = video.indexOf("/day");
				storageJourneyName = video.substring(indexStartJourneyDay, indexEndJourneyDay);
				indexEndStorageJourneyName = storageJourneyName.indexOf("/");
				journeyName = storageJourneyName.substring(indexEndStorageJourneyName + 1);
				LOGGER.info("journeyName: " + journeyName);
				if (!journeyName.equals(name)) {
					return "error/404";
				}
				model.addAttribute("journey", journey);
			}
		}

		return "journeys/journeyDay";
	}
}
