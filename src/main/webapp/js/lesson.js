$('#nextLesson').click(function() {
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
	console.log(urlVideo);
});