var uid = null;
var uri = window.location.pathname;
var numLesson = uri.slice(8);

$('#nextLesson')
		.click(
				function() {
					uid = getCookie('uid');
					console.log("uid: " + uid);
					if (uid === undefined) {
						Swal({
							position : 'center',
							title : 'Vui lòng đăng nhập trước khi chuyển sang bài học tiếp theo!',
							showConfirmButton : false,
							timer : 2000,
						});
						return;
					}
					Swal({
						title : 'Paste your video link below.'
					});
					Swal({
						position : 'center',
						title : 'Vui lòng nhập link video của bạn về bài học này!',
						showConfirmButton : false,
						timer : 2000,
					});
					$('#sendVideo').show();
					$('#urlLesson').show();
					$('#nextLesson').hide();
				});

$('#sendVideo')
		.click(
				function() {
					var urlVideo = $('#lastLesson').val();
					if (urlVideo == "") {
						Swal({
							position : 'center',
							title : 'Vui lòng nhập video bài học của bạn trước khi chuyển sang bài học tiếp theo!',
							showConfirmButton : false,
							timer : 2000,
						});
						return;
					}
					var strUrlCut = urlVideo.slice(8,(urlVideo.indexOf("facebook") - 1));
					var strUrlLast = urlVideo.slice((urlVideo.indexOf("facebook") - 1));
					var strHTTP = "https://m";
					var URL = strHTTP + strUrlLast;
					console.log("URL: " + URL);
					$.ajax({
								url : "/checkVideo?url=" + URL + "&numLesson="
										+ numLesson,
								type : 'POST',
								success : function(data) {
									console.log('data: ' + data);
									window.location.href = data;
								},
								error : function(jqXHR, textStatus, errorThrown) {
									if (jqXHR.status == 404) {
										Swal({
											position : 'center',
											type : 'error',
											title : 'Link video không đúng .Vui lòng nhập lại link video bài học của bạn!',
											showConfirmButton : false,
											timer : 2000,
										});
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