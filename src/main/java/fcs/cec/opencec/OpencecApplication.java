package fcs.cec.opencec;

import java.io.IOException;
import java.util.List;
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
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.database.annotations.Nullable;

import fcs.cec.opencec.entity.Account;
import fcs.cec.opencec.entity.Member;
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
					System.err.println("Listen failed: " + e);
					return;
				}
				for (DocumentChange dc : snapshots.getDocumentChanges()) {
					switch (dc.getType()) {
					case ADDED:
						// get data new member post
						MemberPost memberPost = dc.getDocument().toObject(MemberPost.class);
						String posterId = memberPost.getPosterId();
						String contentPost = memberPost.getContent();

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
						loggerApp.info("journeyDay: " + journeyDay);
						loggerApp.info("day: " + day);
						loggerApp.info("journey: " + journeyName);

						// find lesson
						String nameLesson = null;
						int lesson = 0;
						if (contentPost.toLowerCase().contains("les")) {
							Pattern p2 = Pattern.compile("(\\d+)");
							m = p2.matcher(contentPost.toLowerCase());
							if (m.find()) {
								lesson = Integer.parseInt(m.group());
								nameLesson = "lesson" + lesson;
							}
						}
						loggerApp.info("nameLesson: " + nameLesson);

						// get email in Account
						ApiFuture<QuerySnapshot> future = db.collection("Account")
								.whereEqualTo("memberId", "100005682791081").get();
						String email = null;
						Account account = null;
						try {
							account = future.get().getDocuments().get(0).toObject(Account.class);
							email = account.getEmail();
							loggerApp.info("email: " + email);
							if (email == null || email.length() == 0) {
								loggerApp.info("email not exist.");
								break;
							}
							// send email
							loggerApp.info("send email when new member post.");
							String urlSendMail = "http://httpmailservice.appspot.com/sendEmail";
							try {
								String contentSend = null;
								String journeyNext = null;
								String lessonNext = null;
								if ((journeyName != null && day != null) || (nameLesson != null && lesson != 0)) {

									if (journeyName != null && day != null) {
										if (journeyName.contentEquals("3") && day.equals("3")) {
											journeyNext = "5days/1";
											contentSend = "Chào bạn, đây là link bài ngày hành trình tiếp theo: http://35.226.165.231/"
													+ journeyNext;
											loggerApp.info("contentSend: " + contentSend);
										}
										if (journeyName.contentEquals("5") && day.equals("5")) {
											journeyNext = "7days/1";
											contentSend = "Chào bạn, đây là link bài ngày hành trình tiếp theo: http://35.226.165.231/"
													+ journeyNext;
											loggerApp.info("contentSend: " + contentSend);
										}
										if (journeyName.contentEquals("7") && day.equals("7")) {
											journeyNext = "10days/1";
											contentSend = "Chào bạn, đây là link bài ngày hành trình tiếp theo: http://35.226.165.231/"
													+ journeyNext;
											loggerApp.info("contentSend: " + contentSend);
										}
										if (journeyName.contentEquals("10") && day.equals("10")) {
											journeyNext = "21days/1";
											contentSend = "Chào bạn, đây là link bài ngày hành trình tiếp theo: http://35.226.165.231/"
													+ journeyNext;
											loggerApp.info("contentSend: " + contentSend);
										}
										if (journeyName.contentEquals("21") && day.equals("21")) {
											journeyNext = "45days/1";
											contentSend = "Chào bạn, đây là link bài ngày hành trình tiếp theo: http://35.226.165.231/"
													+ journeyNext;
											loggerApp.info("contentSend: " + contentSend);
										}
										if (journeyName.contentEquals("45") && day.equals("45")) {
											journeyNext = "90days/1";
											contentSend = "Chào bạn, đây là link bài ngày hành trình tiếp theo: http://35.226.165.231/"
													+ journeyNext;
											loggerApp.info("contentSend: " + contentSend);
										}
										if ((journeyName.equals("3") && Integer.parseInt(day) < 3)
												|| (journeyName.equals("5") && Integer.parseInt(day) < 5)
												|| (journeyName.equals("7") && Integer.parseInt(day) < 7)
												|| (journeyName.equals("10") && Integer.parseInt(day) < 10)
												|| (journeyName.equals("21") && Integer.parseInt(day) < 21)
												|| (journeyName.equals("45") && Integer.parseInt(day) < 45)
												|| (journeyName.equals("90") && Integer.parseInt(day) < 90)) {
											int dayNumber = Integer.parseInt(day) + 1;
											day = String.valueOf(dayNumber);
											contentSend = "Chào bạn, đây là link bài ngày hành trình tiếp theo: http://35.226.165.231/"
													+ journeyName + "days/" + day;
											loggerApp.info("contentSend: " + contentSend);
										} else {
											break;
										}
									} else {
										if (1 <= lesson && lesson < 24) {
											lessonNext = String.valueOf(lesson + 1);
											contentSend = "Chào bạn, đây là link bài học tiếp theo: http://35.226.165.231/lesson/"
													+ lessonNext;
											loggerApp.info("contentSend: " + contentSend);
										} else {
											break;
										}
									}
									Response content = Jsoup.connect(urlSendMail).ignoreContentType(true)
											.timeout(60 * 1000).data("subject", "CEC").data("to", email)
											.data("from", "opencecv2@gmail.com").data("content", contentSend)
											.method(Method.GET).followRedirects(true).ignoreHttpErrors(true).execute();
								} else {
									break;
								}
							} catch (IOException e1) {
								e1.printStackTrace();
							}
						} catch (InterruptedException | ExecutionException e2) {
							e2.printStackTrace();
						}

						break;
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
