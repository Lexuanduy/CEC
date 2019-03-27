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
const db = firebase.firestore();

// facebook auth
var provider = new firebase.auth.FacebookAuthProvider();
var photoURL = null;
var uid = null;
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
            firebase.auth().signInWithPopup(provider).then(function (result) {
                var token = result.credential.accessToken; // Token facebook
															// của user
                var user = result.user; // Thông tin của user
                photoURL = user.photoURL;
                uid = user.uid;
                $('#sign-in')[0].hidden = true;
                document.getElementById("photoURL").src= user.photoURL;
                // create cookie
				 document.cookie = 'photoURL=' + photoURL;
				 document.cookie = 'uid=' + user.uid;
				 document.cookie = 'displayName=' + user.displayName;
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
// console.log('photoURL: ' + getCookie('photoURL'));
// console.log('uid: ' + getCookie('uid'));
// console.log('displayName: ' + getCookie('displayName'));

// set avatar from photoURL in cookie
document.getElementById("photoURL").src= getCookie('photoURL');

$('#sign-out').on('click',()=>{
	$('#logout').on('click',()=>{
		 firebase.auth().signOut().then(function () {
			 // delete cookie when logout
			 document.cookie = 'photoURL' + '=;expires=Thu, 01 Jan 1970 00:00:01 GMT;';
			 document.cookie = 'uid' + '=;expires=Thu, 01 Jan 1970 00:00:01 GMT;';
			 document.cookie = 'displayName' + '=;expires=Thu, 01 Jan 1970 00:00:01 GMT;';
			 document.cookie = 'numLesson' + '=;expires=Thu, 01 Jan 1970 00:00:01 GMT;';
		 console.log("Logout thành công");
		 $('#sign-out')[0].hidden = true;
		 }).catch(function (error) {
		 alert("Đã có lỗi xảy ra trong quá trình logout. Xin thử lại")
		 });
	});
});



