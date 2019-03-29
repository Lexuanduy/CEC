// Initialize Firebase
// TODO: Replace with your project's customized code snippet
var config = {
	apiKey : "AIzaSyB1VOLrgajZr5o879ijK2fWcvui394jNC4",
	authDomain : "opencec.firebaseapp.com",
	databaseURL : "https://opencec.firebaseio.com",
	projectId : "opencec",
	storageBucket : "opencec.appspot.com",
	messagingSenderId : "1003263080371",
};
firebase.initializeApp(config);
var db = firebase.firestore();
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
            	console.log(result);
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
			 document.cookie = 'facebookId' + '=;expires=Thu, 01 Jan 1970 00:00:01 GMT;';
			 document.cookie = 'uid' + '=;expires=Thu, 01 Jan 1970 00:00:01 GMT;';
			 document.cookie = 'photoURL' + '=;expires=Thu, 01 Jan 1970 00:00:01 GMT;';
			 document.cookie = 'displayName' + '=;expires=Thu, 01 Jan 1970 00:00:01 GMT;';
			 document.cookie = 'numLesson' + '=;expires=Thu, 01 Jan 1970 00:00:01 GMT;';
			 document.cookie = 'idToken' + '=;expires=Thu, 01 Jan 1970 00:00:01 GMT;';
		 console.log("Logout thành công");
		 $('#sign-out')[0].hidden = true;
		 }).catch(function (error) {
		 alert("Đã có lỗi xảy ra trong quá trình logout. Xin thử lại");
		 });
	});
});



