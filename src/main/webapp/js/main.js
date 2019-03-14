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

// facebook auth
var provider = new firebase.auth.FacebookAuthProvider();
var photoURL = null;
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
                $('#sign-in')[0].hidden = true;
                document.getElementById("photoURL").src= user.photoURL;
                document.cookie = "photoURL=" + photoURL;
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
console.log("cookie" + document.cookie);

// $('#sign-out')[0].hidden = false;
$('#sign-out').on('click',()=>{
	$('#logout').on('click',()=>{
		 firebase.auth().signOut().then(function () {
		 console.log("Logout thành công");
		 $('#sign-out')[0].hidden = true;
		 }).catch(function (error) {
		 alert("Đã có lỗi xảy ra trong quá trình logout. Xin thử lại")
		 });
	});
});
