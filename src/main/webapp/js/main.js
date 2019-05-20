
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
        document.getElementById("photoURL").src = getCookie('photoURL');
    } else {
        $('#sign-in')[0].hidden = false;
        $('#sign-in').on('click', () => {
            var provider = new firebase.auth.FacebookAuthProvider();
            provider.setCustomParameters({
                'display': 'popup' // Login dưới dạng popup
            });
            firebase.auth().signInWithPopup(provider).then( function (result) {
// console.log(result);
            	// set hourse cookie
            	var now = new Date();
            	var time = now.getTime();
            	time += 3600 * 12 * 1000;
            	now.setTime(time);
            	
            	//
                var token = result.credential.accessToken; // Token facebook
				var obj = JSON.parse(JSON.stringify(result));
				facebookId = obj.additionalUserInfo.profile.id;
				uid = obj.user.uid;
				displayName = obj.user.displayName;
				photoURL = obj.user.photoURL;
				accessToken = obj.user.stsTokenManager.accessToken;
				
                $('#sign-in')[0].hidden = true;
                document.getElementById("photoURL").src= photoURL;
                // create cookie
                document.cookie = 
                	'facebookId=' + facebookId + 
                	'; expires=' + now.toUTCString() + 
                	'; path=/';
                document.cookie = 
                	'photoURL=' + photoURL + 
                	'; expires=' + now.toUTCString() + 
                	'; path=/';
                document.cookie = 
                	'uid=' + uid + 
                	'; expires=' + now.toUTCString() + 
                	'; path=/';
                document.cookie = 
                	'displayName=' + displayName + 
                	'; expires=' + now.toUTCString() + 
                	'; path=/';
                document.cookie = 
                	'idToken=' + accessToken + 
                	'; expires=' + now.toUTCString() + 
                	'; path=/';
//                window.location.href = window.location.pathname;
                window.location = window.location.href;
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

$('#sign-out').on('click',()=>{
	$('#logout').on('click',()=>{
		 firebase.auth().signOut().then(function () {
			 // delete cookie when logout
// console.log("log out");
// document.cookie = 'idToken' + '=;expires=Thu, 01 Jan 1970 00:00:01 GMT;';
// console.log("log out idToken");
// document.cookie = 'facebookId' + '=;expires=Thu, 01 Jan 1970 00:00:01 GMT;';
// document.cookie = 'uid' + '=;expires=Thu, 01 Jan 1970 00:00:01 GMT;';
// document.cookie = 'photoURL' + '=;expires=Thu, 01 Jan 1970 00:00:01 GMT;';
// document.cookie = 'displayName' + '=;expires=Thu, 01 Jan 1970 00:00:01 GMT;';
// document.cookie = 'numLesson' + '=;expires=Thu, 01 Jan 1970 00:00:01 GMT;';
// console.log("Logout thành công");
			 var cookies = document.cookie.split(";");

			    for (var i = 0; i < cookies.length; i++) {
			        var cookie = cookies[i];
			        var eqPos = cookie.indexOf("=");
			        var name = eqPos > -1 ? cookie.substr(0, eqPos) : cookie;
			        document.cookie = name + "=;expires=Thu, 01 Jan 1970 00:00:00 GMT"+'; path=/';
			    }
//			    window.location.href = window.location.href;
			    console.log("url href: " + window.location.href);
			    window.location = window.location.href;
		 $('#sign-out')[0].hidden = true;
		 }).catch(function (error) {
		 alert("Đã có lỗi xảy ra trong quá trình logout. Xin thử lại");
		 });
	});
});

// next lesson

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
					var uri = window.location.pathname;
					var numLesson = uri.slice(8);
					facebookId = getCookie('facebookId'); 
					var docLessonMember = numLesson + facebookId;
					console.log("docLessonMember: " + docLessonMember);
					numLesson = numLesson*1; 
					console.log("numlesson: " + numLesson);
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
				        				            	console.log("get cookie facebookId: " + getCookie('facebookId'));
				        				            	console.log("get cookie uid: " + getCookie('uid'));
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
				        								
				        								console.log("urlVideo: " + urlVideo);
				        								regexp =  /^(?:(?:https?|ftp):\/\/)?(?:(?!(?:10|127)(?:\.\d{1,3}){3})(?!(?:169\.254|192\.168)(?:\.\d{1,3}){2})(?!172\.(?:1[6-9]|2\d|3[0-1])(?:\.\d{1,3}){2})(?:[1-9]\d?|1\d\d|2[01]\d|22[0-3])(?:\.(?:1?\d{1,2}|2[0-4]\d|25[0-5])){2}(?:\.(?:[1-9]\d?|1\d\d|2[0-4]\d|25[0-4]))|(?:(?:[a-z\u00a1-\uffff0-9]-*)*[a-z\u00a1-\uffff0-9]+)(?:\.(?:[a-z\u00a1-\uffff0-9]-*)*[a-z\u00a1-\uffff0-9]+)*(?:\.(?:[a-z\u00a1-\uffff]{2,})))(?::\d{2,5})?(?:\/\S*)?$/;
												        if (regexp.test(urlVideo))
												        {
												          
												        }
												        else
												        {
												          Swal({
																position : 'center',
																type : 'error',
																title : 'Link video không đúng định dang. Vui lòng nhập lại!',
																showConfirmButton : false,
																timer : 2000,
															});
												          return;
												        }
				        								var indexGroup = 0;
				        								indexGroup = urlVideo.indexOf("s/");
				        								console.log("indexGroup: " + indexGroup);
				        								var checkGroup = urlVideo.slice(indexGroup + 2, 35);
				        								console.log("checkGroup: " + checkGroup);
				        								var postId;
				        								var URL;
				        								if(checkGroup != "cec"){
				        									console.log("!= cec");
				        									console.log("url valid groups: " + urlVideo.slice(23,29));
				        									var checkGroup1 = urlVideo.slice(23,29);
				        									if (checkGroup1 != "groups") {
				        										Swal({
																	position : 'center',
																	type : 'error',
																	title : 'Vui lòng nhập lại link video được đăng trên groups facebook cec!',
																	showConfirmButton : false,
																	timer : 2000,
																});
				        										return;
				        									}
				        									var indexId = 0;
				        									indexId = urlVideo.lastIndexOf("=");
				        									postId = urlVideo.slice(indexId + 1);
				        									console.log("postId: " + postId);
				        									URL = "https://m.facebook.com/groups/cec.edu.vn/permalink/"+postId;
				        									console.log("URLif: " + URL);
				        									
				        								}else{
					        									console.log("=cec");
					        									var strUrlCut = urlVideo.slice(8, (urlVideo
						        										.indexOf("facebook") - 1));
						        								var strUrlLast = urlVideo.slice((urlVideo
						        										.indexOf("facebook") - 1));
						        								var strHTTP = "https://m";
						        								URL = strHTTP + strUrlLast;
						        								console.log("URLelse: " + URL);
						        								var checkGroup2 = URL.slice(23,29);
						        								console.log("url valid groups: " + checkGroup2);
						        								if (checkGroup2 != "groups"){
						        									Swal({
																		position : 'center',
																		type : 'error',
																		title : 'Vui lòng nhập lại link video được đăng trên groups facebook cec!',
																		showConfirmButton : false,
																		timer : 2000,
																	});
					        										return;
						        								}
					        								}
			        									
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
				        												if (jqXHR.status == 400) {
				        													Swal({
				        														position : 'center',
				        														type : 'error',
				        														title : 'Link video đã tồn tại .Vui lòng nhập lại link video bài học của bạn!',
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
					var uri = window.location.pathname;
					console.log("uri: " + uri);
					var journeyDay = uri.slice(9);
					var indexDay = journeyDay.indexOf("/")*1;  
					var journey = journeyDay.slice(0,indexDay);
// var journeyName = (journeyDay.slice(0,indexDay)).slice(0,1);
					//
					var _indexdays = uri.indexOf("days")*1;
					var journeyName = uri.slice(9,_indexdays);
					//
					console.log("journey: " + journeyName);
					indexDay = indexDay + 1;
					var day = journeyDay.slice(indexDay);
					console.log("day: " + day);
					facebookId = getCookie('facebookId'); 
					var docJourneyDay = journey + day + facebookId;
					console.log("docJourneyDay: " + docJourneyDay);
					var numDay = day*1;
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
		        								
		        								console.log("urlVideo: " + urlVideo);
		        								regexp =  /^(?:(?:https?|ftp):\/\/)?(?:(?!(?:10|127)(?:\.\d{1,3}){3})(?!(?:169\.254|192\.168)(?:\.\d{1,3}){2})(?!172\.(?:1[6-9]|2\d|3[0-1])(?:\.\d{1,3}){2})(?:[1-9]\d?|1\d\d|2[01]\d|22[0-3])(?:\.(?:1?\d{1,2}|2[0-4]\d|25[0-5])){2}(?:\.(?:[1-9]\d?|1\d\d|2[0-4]\d|25[0-4]))|(?:(?:[a-z\u00a1-\uffff0-9]-*)*[a-z\u00a1-\uffff0-9]+)(?:\.(?:[a-z\u00a1-\uffff0-9]-*)*[a-z\u00a1-\uffff0-9]+)*(?:\.(?:[a-z\u00a1-\uffff]{2,})))(?::\d{2,5})?(?:\/\S*)?$/;
										        if (regexp.test(urlVideo))
										        {
										          
										        }
										        else
										        {
										          Swal({
														position : 'center',
														type : 'error',
														title : 'Link video không đúng định dang. Vui lòng nhập lại!',
														showConfirmButton : false,
														timer : 2000,
													});
										          return;
										        }
		        								var indexGroup = 0;
		        								indexGroup = urlVideo.indexOf("s/");
		        								console.log("indexGroup: " + indexGroup);
		        								var checkGroup = urlVideo.slice(indexGroup + 2, 35);
		        								console.log("checkGroup: " + checkGroup);
		        								var postId;
		        								var URL = null;
		        								if(checkGroup != "cec"){
		        									console.log("!= cec");
		        									console.log("url valid groups: " + urlVideo.slice(23,29));
		        									var checkGroup1 = urlVideo.slice(23,29);
		        									if (checkGroup1 != "groups") {
		        										Swal({
															position : 'center',
															type : 'error',
															title : 'Vui lòng nhập lại link video được đăng trên groups facebook cec!',
															showConfirmButton : false,
															timer : 2000,
														});
		        										return;
		        									}
		        									var indexId = 0;
		        									indexId = urlVideo.lastIndexOf("=");
		        									postId = urlVideo.slice(indexId + 1);
		        									console.log("postId: " + postId);
		        									URL = "https://m.facebook.com/groups/cec.edu.vn/permalink/"+postId;
		        									console.log("URLif: " + URL);
		        								}else{
		        									console.log("=cec");
		        									var strUrlCut = urlVideo.slice(8, (urlVideo
			        										.indexOf("facebook") - 1));
			        								var strUrlLast = urlVideo.slice((urlVideo
			        										.indexOf("facebook") - 1));
			        								var strHTTP = "https://m";
			        								URL = strHTTP + strUrlLast;
			        								console.log("URLelse: " + URL);
			        								var checkGroup2 = URL.slice(23,29);
			        								console.log("url valid groups: " + checkGroup2);
			        								if (checkGroup2 != "groups"){
			        									Swal({
															position : 'center',
															type : 'error',
															title : 'Vui lòng nhập lại link video được đăng trên groups facebook cec!',
															showConfirmButton : false,
															timer : 2000,
														});
		        										return;
			        								}
			        								}
		        								console.log("journey: " + journeyName);
		        								
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
														if (jqXHR.status == 400) {
															Swal({
																position : 'center',
																title : 'Link video đã tồn tại. Vui lòng nhập lại link video ngày hành trình của bạn!',
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
									return;
						        }
						    } else {
						        console.log("No such document JourneyDay!");
						    }
						}).catch(function(error) {
						    console.log("Error getting document JourneyDay:", error); 
						});
				});
// end next journey day



