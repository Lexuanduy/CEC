$('#nextLesson').click(function() {
	var uid = getCookie('uid');
	console.log("uid: " + uid);
	if (uid === undefined) {
		Swal({
			title : 'Please login before moving on to the next lesson!'
		});
		return;
	}
	Swal({
		title : 'Paste your video link below.'
	});
	$('#sendVideo').show();
	$('#urlLesson').show();
	$('#nextLesson').hide();
});

$('#sendVideo').click(function() {
	var urlVideo = $('#lastLesson').val();
	if (urlVideo == "") {
		Swal({
			title : 'Paste enter link your video.!'
		});
		return;
	}
	console.log("urlVideo: " + urlVideo);
	var displayName = getCookie('displayName');
	console.log("displayName: " + displayName);
	$.ajax({
		url : "/checkVideo?url=" + urlVideo + "&displayName=" + displayName,
		type : 'POST',
		success : function(data) {
			console.log('username: ' + data);
			if (data.status == 200) {
				console.log('ok');
			}
		},
		error : function(jqXHR, textStatus, errorThrown) {
			if (jqXHR.status == 404) {
				alert("error 404");
			}
			if (jqXHR.status == 403) {
				alert("error 403");
			}

			if (jqXHR.status == 405) {
				alert("error 405");
			}
		}
	});
});