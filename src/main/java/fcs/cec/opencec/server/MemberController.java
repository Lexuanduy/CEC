package fcs.cec.opencec.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.QuerySnapshot;

import fcs.cec.opencec.entity.Member;

@Controller
public class MemberController {
	private static final Logger LOGGER = LoggerFactory.getLogger(MemberController.class);

	@GetMapping(value = "/members")
	public String members(Model model) throws InterruptedException, ExecutionException {
		Firestore db = FirestoreOptions.getDefaultInstance().getService();
		ApiFuture<QuerySnapshot> query = db.collection("Member").limit(100).get();
		List<Member> members = query.get().toObjects(Member.class);
//		List<HashMap<String, String>> listMap = new ArrayList<>();
//		for (Member member : members) {
//			HashMap<String, String> hashMap = new HashMap();
//			hashMap.put("id", member.getId());
//			hashMap.put("avatar", member.getAvatar());
//			hashMap.put("name", member.getName());
//			listMap.add(hashMap);
//		}
		model.addAttribute("listMap", members);
		return "member/member-list";
	}
}
