
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
				console.log("idToken: " + accessToken);
				
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
facebookId = getCookie('facebookId');
var docLessonMember = numLesson + facebookId;
console.log("docLessonMember: " + docLessonMember);
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
					db.collection("LessonMember").doc(docLessonMember).get().then(function(doc) {
						if (doc.exists) {
							if(doc.data().status == 1){
				            	console.log("status: " + doc.data().status);
				            	numLesson = numLesson + 1;
				            	var nextVideoUrl = "/lesson/" + numLesson;
				            	window.location.href = nextVideoUrl;
				            }else {
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
				        													Swal({
				        														position : 'center',
				        														type : 'error',
				        														title : 'Đây không phải video của bạn .Vui lòng nhập lại link video bài học của bạn!',
				        														showConfirmButton : false,
				        														timer : 3000,
				        													});
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
						}
						else {
							console.log("No such document LessonMember!");
						}
					}).catch(function(error) {
					    console.log("Error getting document LessonMember:", error); 
					});
				});
// end next lesson

// next journey day
var uri = window.location.pathname;
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
var numDay = day*1;
$('#nextDay')
		.click(function() {
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
						        	if ((day < 3 && journey == "3days") || (day < 5 && journey == "5days") || (day < 7 && journey == "7days") || (day < 10 && journey == "10days") || (day < 21 && journey == "21days") || (day < 45 && journey == "45days") || (day <90 && journey == "90days")) {
						        		console.log("status: " + doc.data().status);
						            	numDay = numDay + 1;
						            	console.log("numday: " + numDay);
						            	var nextJourneyDayUrl = "/journey/" + journey + "/" + numDay;
						            	window.location.href = nextJourneyDayUrl;
						        	}
					            	if ((day >= 3 || day >= 5 || day >= 7 || day >= 10 || day >= 21 || day >= 45 || day >=90)) {
					            		if(journey == "3days" && day == 3){
							        		console.log("uri: " + "3days/3");
							        		window.location.href = "/journey/5days/1";
							        	}
							        	if(journey == "5days" && day == 5){
							        		console.log("uri: " + "5days/5");
							        		window.location.href = "/journey/7days/1";
							        	}
							        	if(journey == "7days" && day == 7){
							        		console.log("uri: " + "7days/7");
							        		window.location.href = "/journey/10days/1";
							        	}
							        	if(journey == "10days" && day == 10){
							        		console.log("uri: " + "10days/10");
							        		window.location.href = "/journey/21days/1";
							        	}
							        	if(journey == "21days" && day == 21){
							        		console.log("uri: " + "21days/21");
							        		window.location.href = "/journey/45days/1";
							        	}
							        	if(journey == "45days" && day == 45){
							        		console.log("uri: " + "45days/45");
							        		window.location.href = "/journey/90days/1";
							        	}
							        	if(journey == "90days" && day == 90){
							        		console.log("uri: " + "90days/90");
							        		return;
							        	}
					            	}
					            	return;
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
														window.location.href = data;
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
															Swal({
																position : 'center',
																title : 'Link video không phải là của bạn .Vui lòng nhập lại link video ngày hành trình của bạn!',
																showConfirmButton : false,
																timer : 3500,
															});
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
									return;
						        }
						    } else {
						        console.log("No such document JourneyDay!");
						    }
						}).catch(function(error) {
						    console.log("Error getting document JourneyDay:", error); 
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



