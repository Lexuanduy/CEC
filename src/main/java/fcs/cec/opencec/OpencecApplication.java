package fcs.cec.opencec;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.spring5.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentChange;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.EventListener;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreException;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.database.annotations.Nullable;

import fcs.cec.opencec.entity.Account;
import fcs.cec.opencec.entity.MemberPost;
import nz.net.ultraq.thymeleaf.LayoutDialect;

@SpringBootApplication
public class OpencecApplication {
	static Logger loggerApp = Logger.getLogger(OpencecApplication.class.getName());

	public static void main(String[] args) {
		SpringApplication.run(OpencecApplication.class, args);

		Firestore db = FirestoreClient.getFirestore();
		db.collection("MemberPost").addSnapshotListener(new EventListener<QuerySnapshot>() {
			@Override
			public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirestoreException e) {
				if (e != null) {
					loggerApp.info("Listen failed: " + e);
					return;
				}
				for (DocumentChange dc : snapshots.getDocumentChanges()) {
					switch (dc.getType()) {
					case ADDED:
						// get data new member post
						if (dc.getDocument().contains("processed")) {
							loggerApp.info("member post checked!");
						} else {
							loggerApp.info("member post new!");
							MemberPost memberPost = dc.getDocument().toObject(MemberPost.class);
							String posterId = memberPost.getPosterId();
//							String posterId="100029203447977";
							String contentPost = memberPost.getContent();
							loggerApp.info("docId member post new: " + dc.getDocument().getId());

							// find journey day
							Matcher m = null;
							Pattern p = Pattern.compile("(\\d+)(/|\\.)(\\d+)");
							m = p.matcher(contentPost.toLowerCase());
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

							// find lesson
							int lesson = 0;
							if (contentPost.toLowerCase().contains("les")) {
								Pattern p2 = Pattern.compile("(\\d+)");
								m = p2.matcher(contentPost.toLowerCase());
								if (m.find()) {
									lesson = Integer.parseInt(m.group());
								}
							}
							loggerApp.info("journey: " + journeyName);
							loggerApp.info("day: " + day);
							loggerApp.info("lesson: " + lesson);

							// get Account by memberId
							loggerApp.info("get data in Account!");
							ApiFuture<QuerySnapshot> future = db.collection("Account")
									.whereEqualTo("memberId", posterId).get();
							loggerApp.info("future account: " + future);
							String email = null;
							Account account = null;
							String accountId = null;
							String uid = null;
							try {
								List<QueryDocumentSnapshot> documents = future.get().getDocuments();
								loggerApp.info("list documents account: " + documents);
								if (documents.isEmpty() == true) {
									loggerApp.info("list documents account is empty.");
									break;
								}
								for (DocumentSnapshot document : documents) {
									account = document.toObject(Account.class);
								}
								loggerApp.info("get account: " + account.toString());
								email = account.getEmail();
								accountId = account.getId();
								uid = account.getUid();
								loggerApp.info("email: " + email);
								loggerApp.info("accountId: " + accountId);
								loggerApp.info("uid: " + uid);
								if (email == null || email.length() == 0) {
									loggerApp.info("email run null || email lenght == 0, break.");
									break;
								}

								String urlSendMail = "http://httpmailservice.appspot.com/sendEmail";
								try {
									String contentSend = null;
									String journeyNext = null;
									String lessonNext = null;
									loggerApp.info("start check journey day || lesson when send email!!!");
									if ((journeyName == null || day == null) && (lesson == 0)) {
										loggerApp.info("journeyName == null || day == null || lesson == 0, break");
										break;
									}
									if (journeyName == null && day == null && lesson == 0) {
										loggerApp.info("journeyName == null && day == null && lesson == 0, break");
										break;
									}
									if (journeyName != null && day != null && lesson != 0) {
										loggerApp.info("journeyName != null && day != null && lesson != 0");
										if (lesson < 1 || lesson >= 24) {
											loggerApp.info("lesson < 1 || lesson >= 24, break");
											break;
										} else {
											// add lesson last
											loggerApp.info("add lesson last.");
											String docLessonMemberLast = lesson + accountId;
											loggerApp.info("docLessonMemberLast: " + docLessonMemberLast);
											Map<String, Object> docData = new HashMap<>();
											docData.put("lesson", lesson);
											docData.put("memberId", posterId);
											docData.put("memberName", "");
											docData.put("postId", "");
											docData.put("status", 1);
											docData.put("url", "");
											docData.put("uid", uid);
											docData.put("accountId", accountId);
											docData.put("createdAt", System.currentTimeMillis() / 1000);
											docData.put("updatedAt", System.currentTimeMillis() / 1000);
											ApiFuture<WriteResult> addLessonLast = db.collection("LessonMember")
													.document(docLessonMemberLast).set(docData);

											// check lesson next
											loggerApp.info("check lesson next");
											lessonNext = String.valueOf(lesson + 1);
											String docLessonMember = lessonNext + accountId;
											loggerApp.info("docLessonMember: " + docLessonMember);
											DocumentReference docRefLessonMember = db.collection("LessonMember")
													.document(docLessonMember);
											ApiFuture<DocumentSnapshot> futureLessonMember = docRefLessonMember.get();
											DocumentSnapshot documentLessonMember = futureLessonMember.get();
											if (documentLessonMember.exists()) {
												loggerApp.info("Document LessonMember exist!");
											} else {
												// add lesson next
												loggerApp.info("No such document LessonMember!");
												loggerApp.info("add lesson next");
												Map<String, Object> data = new HashMap<>();
												lesson = lesson + 1;
												data.put("lesson", lesson);
												data.put("memberId", posterId);
												data.put("memberName", "");
												data.put("postId", "");
												data.put("status", 0);
												data.put("url", "");
												data.put("uid", uid);
												data.put("accountId", accountId);
												data.put("createdAt", System.currentTimeMillis() / 1000);
												data.put("updatedAt", System.currentTimeMillis() / 1000);
												ApiFuture<WriteResult> addedDocRef = db.collection("LessonMember")
														.document(docLessonMember).set(data);
											}

											contentSend = "Chào bạn, đây là link bài học tiếp theo: https://cec.net.vn/lesson/"
													+ lessonNext;
											loggerApp.info("contentSend: " + contentSend);
										}

									} else {
										loggerApp.info("check journey day || lesson ok!!!");
										if (journeyName != null && day != null) {
											loggerApp.info("journey day document!!!");
											// set docId in journey day
											String docJourneyDay = null;
											if (!journeyName.contentEquals("3") && !journeyName.contentEquals("5")
													&& !journeyName.contentEquals("7")
													&& !journeyName.contentEquals("10")
													&& !journeyName.contentEquals("21")
													&& !journeyName.contentEquals("45")
													&& !journeyName.contentEquals("90")) {
												loggerApp.info("journey error!!!");
												break;
											}
											if (Integer.parseInt(day) < 1) {
												loggerApp.info("day < 1 || day error, break");
												break;
											}
											if (journeyName.contentEquals("3") && day.equals("3")) {
												loggerApp.info("3/3");
												loggerApp.info("add day 3/3");
												String docJourneyDayLast = "3days3" + accountId;
												Map<String, Object> dataLast = new HashMap<>();
												dataLast.put("day", 3);
												dataLast.put("journey", "3days");
												dataLast.put("memberId", posterId);
												dataLast.put("memberName", "");
												dataLast.put("postId", "");
												dataLast.put("status", 1);
												dataLast.put("url", "");
												dataLast.put("uid", uid);
												dataLast.put("accountId", accountId);
												dataLast.put("createdAt", System.currentTimeMillis() / 1000);
												dataLast.put("updatedAt", System.currentTimeMillis() / 1000);
												ApiFuture<WriteResult> addedDocLast = db.collection("JourneyDay")
														.document(docJourneyDayLast).set(dataLast);

												loggerApp.info("create 5days day 1.");
												docJourneyDay = "5days1" + accountId;
												loggerApp.info("docJourneyDay: " + docJourneyDay);
												// create new day in journey day
												DocumentReference docRef = db.collection("JourneyDay")
														.document(docJourneyDay);
												ApiFuture<DocumentSnapshot> futureJourneyDay = docRef.get();
												DocumentSnapshot document = futureJourneyDay.get();
												if (document.exists()) {
													loggerApp.info("Document JourneyDay exist!");
												} else {
													loggerApp.info("No such document JourneyDay!");
													Map<String, Object> data = new HashMap<>();
													data.put("day", 1);
													data.put("journey", "5days");
													data.put("memberId", posterId);
													data.put("memberName", "");
													data.put("postId", "");
													data.put("status", 0);
													data.put("url", "");
													data.put("uid", uid);
													data.put("accountId", accountId);
													data.put("createdAt", System.currentTimeMillis() / 1000);
													data.put("updatedAt", System.currentTimeMillis() / 1000);
													ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay")
															.document(docJourneyDay).set(data);
												}
												journeyNext = "5days/1";
												contentSend = "Chào bạn, đây là link bài ngày hành trình tiếp theo: https://cec.net.vn/journey/"
														+ journeyNext;
												loggerApp.info("contentSend: " + contentSend);
											}
											if (journeyName.contentEquals("5") && day.equals("5")) {
												loggerApp.info("5/5");
												loggerApp.info("add doc 5days day 5.");
												String docJourneyDayLast = "5days5" + accountId;
												Map<String, Object> dataLast = new HashMap<>();
												dataLast.put("day", 5);
												dataLast.put("journey", "5days");
												dataLast.put("memberId", posterId);
												dataLast.put("memberName", "");
												dataLast.put("postId", "");
												dataLast.put("status", 1);
												dataLast.put("url", "");
												dataLast.put("uid", uid);
												dataLast.put("accountId", accountId);
												dataLast.put("createdAt", System.currentTimeMillis() / 1000);
												dataLast.put("updatedAt", System.currentTimeMillis() / 1000);
												ApiFuture<WriteResult> addedDocLast = db.collection("JourneyDay")
														.document(docJourneyDayLast).set(dataLast);

												loggerApp.info("check 7days day 1.");
												docJourneyDay = "7days1" + accountId;
												loggerApp.info("docJourneyDay: " + docJourneyDay);
												// create new day in journey day
												DocumentReference docRef = db.collection("JourneyDay")
														.document(docJourneyDay);
												ApiFuture<DocumentSnapshot> futureJourneyDay = docRef.get();
												DocumentSnapshot document = futureJourneyDay.get();
												if (document.exists()) {
													loggerApp.info("Document JourneyDay exist!");
												} else {
													loggerApp.info("No such document 7days day 1!");
													Map<String, Object> data = new HashMap<>();
													data.put("day", 1);
													data.put("journey", "7days");
													data.put("memberId", posterId);
													data.put("memberName", "");
													data.put("postId", "");
													data.put("status", 0);
													data.put("url", "");
													data.put("uid", uid);
													data.put("accountId", accountId);
													data.put("createdAt", System.currentTimeMillis() / 1000);
													data.put("updatedAt", System.currentTimeMillis() / 1000);
													ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay")
															.document(docJourneyDay).set(data);
												}
												journeyNext = "7days/1";
												contentSend = "Chào bạn, đây là link bài ngày hành trình tiếp theo: https://cec.net.vn/journey/"
														+ journeyNext;
												loggerApp.info("contentSend: " + contentSend);
											}
											if (journeyName.contentEquals("7") && day.equals("7")) {
												loggerApp.info("7/7");
												loggerApp.info("add doc 7days day 7.");
												String docJourneyDayLast = "7days7" + accountId;
												Map<String, Object> dataLast = new HashMap<>();
												dataLast.put("day", 7);
												dataLast.put("journey", "7days");
												dataLast.put("memberId", posterId);
												dataLast.put("memberName", "");
												dataLast.put("postId", "");
												dataLast.put("status", 1);
												dataLast.put("url", "");
												dataLast.put("uid", uid);
												dataLast.put("accountId", accountId);
												dataLast.put("createdAt", System.currentTimeMillis() / 1000);
												dataLast.put("updatedAt", System.currentTimeMillis() / 1000);
												ApiFuture<WriteResult> addedDocLast = db.collection("JourneyDay")
														.document(docJourneyDayLast).set(dataLast);

												loggerApp.info("check doc 10days day 1.");
												docJourneyDay = "10days1" + accountId;
												loggerApp.info("docJourneyDay: " + docJourneyDay);
												// create new day in journey day
												DocumentReference docRef = db.collection("JourneyDay")
														.document(docJourneyDay);
												ApiFuture<DocumentSnapshot> futureJourneyDay = docRef.get();
												DocumentSnapshot document = futureJourneyDay.get();
												if (document.exists()) {
													loggerApp.info("Document 10days day 1 exist!");
												} else { 
													loggerApp.info("No such document 10days day 1!");
													Map<String, Object> data = new HashMap<>();
													data.put("day", 1);
													data.put("journey", "10days");
													data.put("memberId", posterId);
													data.put("memberName", "");
													data.put("postId", "");
													data.put("status", 0);
													data.put("url", "");
													data.put("uid", uid);
													data.put("accountId", accountId);
													data.put("createdAt", System.currentTimeMillis() / 1000);
													data.put("updatedAt", System.currentTimeMillis() / 1000);
													ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay")
															.document(docJourneyDay).set(data);
												}
												journeyNext = "10days/1";
												contentSend = "Chào bạn, đây là link bài ngày hành trình tiếp theo: https://cec.net.vn/journey/"
														+ journeyNext;
												loggerApp.info("contentSend: " + contentSend);
											}
											if (journeyName.contentEquals("10") && day.equals("10")) {
												loggerApp.info("10/10");
												loggerApp.info("add doc 10days day 10.");
												String docJourneyDayLast = "10days10" + accountId;
												Map<String, Object> dataLast = new HashMap<>();
												dataLast.put("day", 10);
												dataLast.put("journey", "10days");
												dataLast.put("memberId", posterId);
												dataLast.put("memberName", "");
												dataLast.put("postId", "");
												dataLast.put("status", 1);
												dataLast.put("url", "");
												dataLast.put("uid", uid);
												dataLast.put("accountId", accountId);
												dataLast.put("createdAt", System.currentTimeMillis() / 1000);
												dataLast.put("updatedAt", System.currentTimeMillis() / 1000);
												ApiFuture<WriteResult> addedDocLast = db.collection("JourneyDay")
														.document(docJourneyDayLast).set(dataLast);

												loggerApp.info("check doc 21days day 1"); 
												docJourneyDay = "21days1" + accountId;
												loggerApp.info("docJourneyDay: " + docJourneyDay);
												// create new day in journey day
												DocumentReference docRef = db.collection("JourneyDay")
														.document(docJourneyDay);
												ApiFuture<DocumentSnapshot> futureJourneyDay = docRef.get();
												DocumentSnapshot document = futureJourneyDay.get();
												if (document.exists()) {
													loggerApp.info("Document 21days day 1 exist!");
												} else {
													loggerApp.info("No such document 21days day 1!");
													Map<String, Object> data = new HashMap<>();
													data.put("day", 1);
													data.put("journey", "21days");
													data.put("memberId", posterId);
													data.put("memberName", "");
													data.put("postId", "");
													data.put("status", 0);
													data.put("url", "");
													data.put("uid", uid);
													data.put("accountId", accountId);
													data.put("createdAt", System.currentTimeMillis() / 1000);
													data.put("updatedAt", System.currentTimeMillis() / 1000);
													ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay")
															.document(docJourneyDay).set(data);
												}
												journeyNext = "21days/1";
												contentSend = "Chào bạn, đây là link bài ngày hành trình tiếp theo: https://cec.net.vn/journey/"
														+ journeyNext;
												loggerApp.info("contentSend: " + contentSend);
											}
											if (journeyName.contentEquals("21") && day.equals("21")) {
												loggerApp.info("21/21");
												loggerApp.info("add doc 21days day 21.");
												String docJourneyDayLast = "21days21" + accountId;
												Map<String, Object> dataLast = new HashMap<>();
												dataLast.put("day", 21);
												dataLast.put("journey", "21days");
												dataLast.put("memberId", posterId);
												dataLast.put("memberName", "");
												dataLast.put("postId", "");
												dataLast.put("status", 1);
												dataLast.put("url", "");
												dataLast.put("uid", uid);
												dataLast.put("accountId", accountId);
												dataLast.put("createdAt", System.currentTimeMillis() / 1000);
												dataLast.put("updatedAt", System.currentTimeMillis() / 1000);
												ApiFuture<WriteResult> addedDocLast = db.collection("JourneyDay")
														.document(docJourneyDayLast).set(dataLast);
												
												
												loggerApp.info("check 45days day 1.");
												String journeyNew = "45days1";
												docJourneyDay = journeyNew + accountId;
												loggerApp.info("docJourneyDay: " + docJourneyDay);
												// create new day in journey day
												DocumentReference docRef = db.collection("JourneyDay")
														.document(docJourneyDay);
												ApiFuture<DocumentSnapshot> futureJourneyDay = docRef.get();
												DocumentSnapshot document = futureJourneyDay.get();
												if (document.exists()) {
													loggerApp.info("Document 45days day 1 exist!");
												} else {
													loggerApp.info("No such document 45days day 1!");
													Map<String, Object> data = new HashMap<>();
													data.put("day", 1);
													data.put("journey", "45days");
													data.put("memberId", posterId);
													data.put("memberName", "");
													data.put("postId", "");
													data.put("status", 0);
													data.put("url", "");
													data.put("uid", uid);
													data.put("accountId", accountId);
													data.put("createdAt", System.currentTimeMillis() / 1000);
													data.put("updatedAt", System.currentTimeMillis() / 1000);
													ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay")
															.document(docJourneyDay).set(data);
												}
												journeyNext = "45days/1";
												contentSend = "Chào bạn, đây là link bài ngày hành trình tiếp theo: https://cec.net.vn/journey/"
														+ journeyNext;
												loggerApp.info("contentSend: " + contentSend);
											}
											if (journeyName.contentEquals("45") && day.equals("45")) {
												loggerApp.info("45/45");
												loggerApp.info("add doc 45days day 45.");
												String docJourneyDayLast = "45days45" + accountId;
												Map<String, Object> dataLast = new HashMap<>();
												dataLast.put("day", 45);
												dataLast.put("journey", "45days");
												dataLast.put("memberId", posterId);
												dataLast.put("memberName", "");
												dataLast.put("postId", "");
												dataLast.put("status", 1);
												dataLast.put("url", "");
												dataLast.put("uid", uid);
												dataLast.put("accountId", accountId);
												dataLast.put("createdAt", System.currentTimeMillis() / 1000);
												dataLast.put("updatedAt", System.currentTimeMillis() / 1000);
												ApiFuture<WriteResult> addedDocLast = db.collection("JourneyDay")
														.document(docJourneyDayLast).set(dataLast);
												
												
												loggerApp.info("check doc 90days day 1..");
												docJourneyDay = "90days1" + accountId;
												loggerApp.info("docJourneyDay: " + docJourneyDay);
												// create new day in journey day
												DocumentReference docRef = db.collection("JourneyDay")
														.document(docJourneyDay);
												ApiFuture<DocumentSnapshot> futureJourneyDay = docRef.get();
												DocumentSnapshot document = futureJourneyDay.get();
												if (document.exists()) {
													loggerApp.info("Document 90days day 1 exist!");
												} else {
													loggerApp.info("No such document 90days day 1!");
													Map<String, Object> data = new HashMap<>();
													data.put("day", 1);
													data.put("journey", "90days");
													data.put("memberId", posterId);
													data.put("memberName", "");
													data.put("postId", "");
													data.put("status", 0);
													data.put("url", "");
													data.put("uid", uid);
													data.put("accountId", accountId);
													data.put("createdAt", System.currentTimeMillis() / 1000);
													data.put("updatedAt", System.currentTimeMillis() / 1000);
													ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay")
															.document(docJourneyDay).set(data);
												}
												journeyNext = "90days/1";
												contentSend = "Chào bạn, đây là link bài ngày hành trình tiếp theo: https://cec.net.vn/journey/"
														+ journeyNext;
												loggerApp.info("contentSend: " + contentSend);
											}
											if (Integer.parseInt(day) >= 1 && ((journeyName.equals("3")
													&& Integer.parseInt(day) < 3)
													|| (journeyName.equals("5") && Integer.parseInt(day) < 5)
													|| (journeyName.equals("7") && Integer.parseInt(day) < 7)
													|| (journeyName.equals("10") && Integer.parseInt(day) < 10)
													|| (journeyName.equals("21") && Integer.parseInt(day) < 21)
													|| (journeyName.equals("45") && Integer.parseInt(day) < 45)
													|| (journeyName.equals("90") && Integer.parseInt(day) < 90))) {
												String docJourneyDayLast = journeyName + "days" + day + accountId;
												Map<String, Object> dataLast = new HashMap<>();
												dataLast.put("day", Integer.parseInt(day));
												String journey = journeyName + "days";
												dataLast.put("journey", journey);
												dataLast.put("memberId", posterId);
												dataLast.put("memberName", "");
												dataLast.put("postId", "");
												dataLast.put("status", 1);
												dataLast.put("url", "");
												dataLast.put("uid", uid);
												dataLast.put("accountId", accountId);
												dataLast.put("createdAt", System.currentTimeMillis() / 1000);
												dataLast.put("updatedAt", System.currentTimeMillis() / 1000);
												ApiFuture<WriteResult> addedDocLast = db.collection("JourneyDay")
														.document(docJourneyDayLast).set(dataLast);
												
												
												int dayNumber = Integer.parseInt(day) + 1;
												String dayNext = String.valueOf(dayNumber);
												// set docJourneyDay
												docJourneyDay = journeyName + "days" + dayNext + accountId;
												loggerApp.info("docJourneyDay: " + docJourneyDay);
												// create new day in journey day
												DocumentReference docRef = db.collection("JourneyDay")
														.document(docJourneyDay);
												ApiFuture<DocumentSnapshot> futureJourneyDay = docRef.get();
												DocumentSnapshot document = futureJourneyDay.get();
												if (document.exists()) {
													loggerApp.info("Document JourneyDay exist!");
												} else {
													loggerApp.info("No such document JourneyDay!");
													Map<String, Object> data = new HashMap<>();
													data.put("day", dayNumber);
													data.put("journey", journey);
													data.put("memberId", posterId);
													data.put("memberName", "");
													data.put("postId", "");
													data.put("status", 0);
													data.put("url", "");
													data.put("uid", uid);
													data.put("accountId", accountId);
													data.put("createdAt", System.currentTimeMillis() / 1000);
													data.put("updatedAt", System.currentTimeMillis() / 1000);
													ApiFuture<WriteResult> addedDocRef = db.collection("JourneyDay")
															.document(docJourneyDay).set(data);
												}
												contentSend = "Chào bạn, đây là link bài ngày hành trình tiếp theo: https://cec.net.vn/journey/"
														+ journeyName + "days/" + dayNumber;
												loggerApp.info("contentSend: " + contentSend);
											}
											if (Integer.parseInt(day) >= 1 && ((journeyName.equals("3")
													&& Integer.parseInt(day) >= 4)
													|| (journeyName.equals("5") && Integer.parseInt(day) >= 6)
													|| (journeyName.equals("7") && Integer.parseInt(day) >= 8)
													|| (journeyName.equals("10") && Integer.parseInt(day) >= 11)
													|| (journeyName.equals("21") && Integer.parseInt(day) >= 22)
													|| (journeyName.equals("45") && Integer.parseInt(day) >= 46)
													|| (journeyName.equals("90") && Integer.parseInt(day) >= 91))) {
												loggerApp.info("day does not match journey day");
												break;
											}
										} else {
											loggerApp.info("lesson member document!!!");
											if (lesson < 1 || lesson >= 24) {
												loggerApp.info("lesson < 1 || lesson >= 24, break");
												break;
											} else {
												lessonNext = String.valueOf(lesson + 1);
												String docLessonMember = lessonNext + accountId;
												loggerApp.info("docLessonMember: " + docLessonMember);
												DocumentReference docRefLessonMember = db.collection("LessonMember")
														.document(docLessonMember);
												ApiFuture<DocumentSnapshot> futureLessonMember = docRefLessonMember
														.get();
												DocumentSnapshot documentLessonMember = futureLessonMember.get();
												if (documentLessonMember.exists()) {
													loggerApp.info("Document LessonMember exist!");
												} else {
													// add lesson last
													loggerApp.info("add lesson last");
													String docLessonMemberLast = lesson + accountId;
													loggerApp.info("docLessonMemberLast: " + docLessonMemberLast);
													Map<String, Object> docData = new HashMap<>();
													docData.put("lesson", lesson);
													docData.put("memberId", posterId);
													docData.put("memberName", "");
													docData.put("postId", "");
													docData.put("status", 1);
													docData.put("url", "");
													docData.put("uid", uid);
													docData.put("accountId", accountId);
													docData.put("createdAt", System.currentTimeMillis() / 1000);
													docData.put("updatedAt", System.currentTimeMillis() / 1000);
													ApiFuture<WriteResult> addLessonLast = db.collection("LessonMember")
															.document(docLessonMemberLast).set(docData);

													// add lesson next
													loggerApp.info("No such document LessonMember!");
													loggerApp.info("add lesson next.");
													Map<String, Object> data = new HashMap<>();
													lesson = lesson + 1;
													data.put("lesson", lesson);
													data.put("memberId", posterId);
													data.put("memberName", "");
													data.put("postId", "");
													data.put("status", 0);
													data.put("url", "");
													data.put("uid", uid);
													data.put("accountId", accountId);
													data.put("createdAt", System.currentTimeMillis() / 1000);
													data.put("updatedAt", System.currentTimeMillis() / 1000);
													ApiFuture<WriteResult> addedDocRef = db.collection("LessonMember")
															.document(docLessonMember).set(data);
												}

												contentSend = "Chào bạn, đây là link bài học tiếp theo: https://cec.net.vn/lesson/"
														+ lessonNext;
												loggerApp.info("contentSend: " + contentSend);
											}
										}
									}
									if (contentSend == null) {
										loggerApp.info("contentSend == null");
										break;
									}
									Response content = Jsoup.connect(urlSendMail).ignoreContentType(true)
											.timeout(60 * 1000).data("subject", "CEC").data("to", email)
											.data("from", "opencecv2@gmail.com").data("content", contentSend)
											.method(Method.GET).followRedirects(true).ignoreHttpErrors(true).execute();
									// update member post
									String docMemberPost = dc.getDocument().getId();
									loggerApp.info("docMemberPost update: " + docMemberPost);
									DocumentReference docRefMemberPost = db.collection("MemberPost")
											.document(docMemberPost);
									ApiFuture<WriteResult> futureMemberPost = docRefMemberPost.update("processed",
											true);
								} catch (IOException e1) {
									e1.printStackTrace();
								}
							} catch (InterruptedException | ExecutionException e2) {
								e2.printStackTrace();
							}
							loggerApp.info("switch break.");
							break;
						}
					default:
						break;
					}
				}
			}
		});
	}

	@Bean
	public SpringTemplateEngine templateEngine(ApplicationContext ctx) {
		SpringTemplateEngine templateEngine = new SpringTemplateEngine();

		SpringResourceTemplateResolver templateResolver = new SpringResourceTemplateResolver();
		templateResolver.setCharacterEncoding("UTF-8");
		templateResolver.setApplicationContext(ctx);
		templateResolver.setPrefix("templates/");
		templateResolver.setSuffix(".html");
		templateResolver.setTemplateMode(TemplateMode.HTML);
		templateEngine.setTemplateResolver(templateResolver);
		templateEngine.addDialect(new LayoutDialect());
		return templateEngine;
	}
}
