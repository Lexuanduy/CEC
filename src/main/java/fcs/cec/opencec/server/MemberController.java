package fcs.cec.opencec.server;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.QuerySnapshot;

import fcs.cec.opencec.entity.Member;

@Controller
public class MemberController {
	private static final Logger LOGGER = LoggerFactory.getLogger(MemberController.class);

	@GetMapping(value = "/members", params = "page")
	public String members(Model model, @RequestParam(value = "page", required = false) int page)
			throws InterruptedException, ExecutionException {
		LOGGER.info("page: " + page);
		// ko co page thi page = 1
		if (page < 1) {
			page = 1;
		}
		int limit = 50;
		int offset = (page - 1) * limit;
		Firestore db = FirestoreOptions.getDefaultInstance().getService();
		ApiFuture<QuerySnapshot> query = db.collection("Member").offset(offset).limit(limit).get();
		List<Member> members = query.get().toObjects(Member.class);
		ApiFuture<QuerySnapshot> queryTotal = db.collection("Member").get();
		List<Member> totalMembers = queryTotal.get().toObjects(Member.class);
		int totalPages = totalMembers.size() / limit + 1;
		if (totalPages > 0) {
			List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages).boxed().collect(Collectors.toList());
			model.addAttribute("pageNumbers", pageNumbers);
		}
		model.addAttribute("totalPages", totalPages);
		model.addAttribute("listMap", members);
		return "member/member-list";
	}
}
