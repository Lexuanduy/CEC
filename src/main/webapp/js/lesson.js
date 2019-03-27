var lessonNumber = $.cookie("numLesson");
var uid = null;
$('#nextLesson').click(function() {
	uid = getCookie('uid');
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

$('#sendVideo').click(
		function() {
			var urlVideo = $('#lastLesson').val();
			if (urlVideo == "") {
				Swal({
					title : 'Paste enter link your video.!'
				});
				return;
			}
			uid = getCookie('uid');
			$.ajax({
				url : "/checkVideo?url=" + urlVideo + "&uid=" + uid
						+ "&lessonNumber=" + lessonNumber,
				type : 'POST',
				success : function(data) {
					console.log('data: ' + data);
					 window.location.href = data;
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