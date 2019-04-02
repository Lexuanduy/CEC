
firebase.initializeApp({
	apiKey : "AIzaSyB1VOLrgajZr5o879ijK2fWcvui394jNC4",
	authDomain : "opencec.firebaseapp.com",
	databaseURL : "https://opencec.firebaseio.com",
	projectId : "opencec",
	storageBucket : "opencec.appspot.com",
	messagingSenderId : "1003263080371",
});
const db = firebase.firestore();
var facebookId;
var uid;
var displayName;
var photoURL;
var accessToken;
// facebook auth
var provider = new firebase.auth.FacebookAuthProvider();
firebase.auth().onAuthStateChanged(function (user) {
    if (user) {
        $('#sign-out')[0].hidden = false;
        
    } else {
        $('#sign-in')[0].hidden = false;
        $('#sign-in').on('click', () => {
            var provider = new firebase.auth.FacebookAuthProvider();
            provider.setCustomParameters({
                'display': 'popup' // Login dưới dạng popup
            });
            firebase.auth().signInWithPopup(provider).then( function (result) {
// console.log(result);
                var token = result.credential.accessToken; // Token facebook
                console.log("token: " + token);
				var obj = JSON.parse(JSON.stringify(result));
				facebookId = obj.additionalUserInfo.profile.id;
				uid = obj.user.uid;
				displayName = obj.user.displayName;
				photoURL = obj.user.photoURL;
				accessToken = obj.user.stsTokenManager.accessToken;
				
                $('#sign-in')[0].hidden = true;
                document.getElementById("photoURL").src= photoURL;
                // create cookie
                 document.cookie = 'facebookId=' + facebookId;
				 document.cookie = 'photoURL=' + photoURL;
				 document.cookie = 'uid=' + uid;
				 document.cookie = 'displayName=' + displayName;
				 document.cookie = 'idToken=' + accessToken;
		            
// $('#displayName').html() = user.displayName;
            }).catch(function (error) {
                var errorCode = error.code;
                var errorMessage = error.message;
                var email = error.email;
                var credential = error.credential;
            });
        });
    }
});

// get cookie
function getCookie(key) {
	  const regexp = new RegExp(`.*${key}=([^;]*)`);
	  const result = regexp.exec(document.cookie);
	  if(result) {
	    return result [1];
	  }
	}


// set avatar from photoURL in cookie
// document.getElementById("photoURL").src= getCookie('photoURL');

$('#sign-out').on('click',()=>{
	$('#logout').on('click',()=>{
		 firebase.auth().signOut().then(function () {
			 // delete cookie when logout
			 console.log("log out");
			 document.cookie = 'idToken' + '=;expires=Thu, 01 Jan 1970 00:00:01 GMT;';
			 console.log("log out idToken");
			 document.cookie = 'facebookId' + '=;expires=Thu, 01 Jan 1970 00:00:01 GMT;';
			 document.cookie = 'uid' + '=;expires=Thu, 01 Jan 1970 00:00:01 GMT;';
			 document.cookie = 'photoURL' + '=;expires=Thu, 01 Jan 1970 00:00:01 GMT;';
			 document.cookie = 'displayName' + '=;expires=Thu, 01 Jan 1970 00:00:01 GMT;';
			 document.cookie = 'numLesson' + '=;expires=Thu, 01 Jan 1970 00:00:01 GMT;';
		 console.log("Logout thành công");
		 $('#sign-out')[0].hidden = true;
		 }).catch(function (error) {
		 alert("Đã có lỗi xảy ra trong quá trình logout. Xin thử lại");
		 });
	});
});

// next lesson
var uri = window.location.pathname;
var numLesson = uri.slice(8);
numLesson = numLesson*1; 
console.log("numlesson: " + numLesson);
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
					console.log("click send video check");
					db.collection("LessonMember").where("lesson", "==", numLesson).where("uid", "==", uid)
				    .get()
				    .then(function(querySnapshot) {
				        querySnapshot.forEach(function(doc) {
				            if(doc.data().status == 1){
				            	console.log("status: " + doc.data().status);
				            	numLesson = numLesson + 1;
				            	var nextVideoUrl = "/lesson/" + numLesson;
				            	window.location.href = nextVideoUrl;
				            }
				            else {
								$('#sendVideo').show();
								$('#urlLesson').show();
								$('#nextLesson').hide();
								Swal({
									title : 'Paste your video link below.'
								});
								Swal({
									position : 'center',
									title : 'Vui lòng nhập link video của bạn về bài học này!',
									showConfirmButton : false,
									timer : 2000,
								});
				            	$('#sendVideo')
				        		.click(
				        				function() {
				        				            	console.log("status: 0");
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
				        								var strUrlCut = urlVideo.slice(8, (urlVideo
				        										.indexOf("facebook") - 1));
				        								var strUrlLast = urlVideo.slice((urlVideo
				        										.indexOf("facebook") - 1));
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
				        												if (jqXHR.status == 401) {
				        													Swal({
				        														position : 'center',
				        														title : 'Phiên bản đã hết hạn, vui lòng đăng nhập lại!',
				        														showConfirmButton : false,
				        														timer : 3000,
				        													});
				        												}
				        											}
				        										});
				        				});
				            }
				        });
				    })
				    .catch(function(error) {
				        console.log("Error getting documents: ", error);
				    });
				});
// end next lesson

// next journey day
var uri = window.location.pathname;
// numLesson = numLesson*1;
console.log("uri: " + uri);
var journeyDay = uri.slice(9);
var indexDay = journeyDay.indexOf("/")*1;  
var journey = journeyDay.slice(0,indexDay);
var journeyName = (journeyDay.slice(0,indexDay)).slice(0,1); 
console.log("journey: " + journey);
indexDay = indexDay + 1;
var day = journeyDay.slice(indexDay);
console.log("day: " + day);
facebookId = getCookie('facebookId'); 
var docJourneyDay = journey + day + facebookId;
console.log("docJourneyDay: " + docJourneyDay);
var numDay = 0;
numDay = day*1;
$('#nextDay')
		.click(
				function() {
					uid = getCookie('uid');
					console.log("uid: " + uid);
					if (uid === undefined) {
						Swal({
							position : 'center',
							title : 'Vui lòng đăng nhập trước khi chuyển sang tiếp theo của hành trình!',
							showConfirmButton : false,
							timer : 2000,
						});
						return;
					}
					console.log("click send video journey day check.");
					// check day in journey
						db.collection("JourneyDay").doc(docJourneyDay).get().then(function(doc) {
						    if (doc.exists) {
						        console.log("Document data:", doc.data());
						        if(doc.data().status == 1){
					            	console.log("status: " + doc.data().status);
					            	var nextJourneyDayUrl = "/journey/" + journey + "/" + numDay;
					            	window.location.href = nextJourneyDayUrl;
					            }
						        else {
						        	console.log("status == 0");
						        	$('#sendVideoDay').show();
									$('#urlDay').show();
									$('#nextDay').hide();
									Swal({
										title : 'Paste your video link below.'
									});
									Swal({
										position : 'center',
										title : 'Vui lòng nhập link video của bạn về bài học này!',
										showConfirmButton : false,
										timer : 2000,
									});
									$('#sendVideoDay').click(
					        				function() {
					        					var urlVideo = $('#lastDay').val();
		        								if (urlVideo == "") {
		        									Swal({
		        										position : 'center',
		        										title : 'Vui lòng nhập video ngày hành trình của bạn trước khi chuyển sang ngày tiếp theo!',
		        										showConfirmButton : false,
		        										timer : 2000,
		        									});
		        									return;
		        								}
		        								var strUrlCut = urlVideo.slice(8, (urlVideo
		        										.indexOf("facebook") - 1));
		        								var strUrlLast = urlVideo.slice((urlVideo
		        										.indexOf("facebook") - 1));
		        								var strHTTP = "https://m";
		        								var URL = strHTTP + strUrlLast;
		        								console.log("URL: " + URL);
		        								$.ajax({
		        									url : "/checkJourneyDay?url=" + URL + "&journey="
													+ journeyName + "&numDay=" + numDay,
													type : 'POST',
													success : function(data) {
														console.log('data: ' + data);
// window.location.href = data;
													},
													error : function(jqXHR, textStatus, errorThrown) {
														if (jqXHR.status == 404) {
															Swal({
																position : 'center',
																type : 'error',
																title : 'Link video không đúng .Vui lòng nhập lại link video ngày hành trình của bạn!',
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
														if (jqXHR.status == 401) {
															Swal({
																position : 'center',
																title : 'Phiên bản đã hết hạn, vui lòng đăng nhập lại!',
																showConfirmButton : false,
																timer : 3000,
															});
														}
													}
		        								});
					        				});
						        }
						    } else {
						        console.log("No such document!");
						    }
						}).catch(function(error) {
						    console.log("Error getting document:", error); 
						});
					// end check day
// db.collection("JourneyDay").where("lesson", "==", numLesson).where("uid",
// "==", uid)
// .get()
// .then(function(querySnapshot) {
// querySnapshot.forEach(function(doc) {
// if(doc.data().status == 1){
// console.log("status: " + doc.data().status);
// numLesson = numLesson + 1;
// var nextVideoUrl = "/lesson/" + numLesson;
// window.location.href = nextVideoUrl;
// }
// else {
// $('#sendVideoDay').show();
// $('#urlDay').show();
// $('#nextDay').hide();
// Swal({
// title : 'Paste your video link below.'
// });
// Swal({
// position : 'center',
// title : 'Vui lòng nhập link video của bạn về bài học này!',
// showConfirmButton : false,
// timer : 2000,
// });
// $('#sendVideoDay')
// .click(
// function() {
// console.log("status: 0");
// var urlVideo = $('#lastDay').val();
// if (urlVideo == "") {
// Swal({
// position : 'center',
// title : 'Vui lòng nhập video bài học của bạn trước khi chuyển sang bài học
// tiếp theo!',
// showConfirmButton : false,
// timer : 2000,
// });
// return;
// }
// var strUrlCut = urlVideo.slice(8, (urlVideo
// .indexOf("facebook") - 1));
// var strUrlLast = urlVideo.slice((urlVideo
// .indexOf("facebook") - 1));
// var strHTTP = "https://m";
// var URL = strHTTP + strUrlLast;
// console.log("URL: " + URL);
// $.ajax({
// url : "/checkVideo?url=" + URL + "&numLesson="
// + numLesson,
// type : 'POST',
// success : function(data) {
// console.log('data: ' + data);
// window.location.href = data;
// },
// error : function(jqXHR, textStatus, errorThrown) {
// if (jqXHR.status == 404) {
// Swal({
// position : 'center',
// type : 'error',
// title : 'Link video không đúng .Vui lòng nhập lại link video bài học của
// bạn!',
// showConfirmButton : false,
// timer : 2000,
// });
// }
// if (jqXHR.status == 403) {
// alert("error 403");
// }
// if (jqXHR.status == 405) {
// alert("error 405");
// }
// if (jqXHR.status == 401) {
// Swal({
// position : 'center',
// title : 'Phiên bản đã hết hạn, vui lòng đăng nhập lại!',
// showConfirmButton : false,
// timer : 3000,
// });
// }
// }
// });
// });
// }
// });
// })
// .catch(function(error) {
// console.log("Error getting documents: ", error);
// });
				});
// end next journey day



