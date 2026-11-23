class AuthManager {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // Patient Sign Up with Name, Mobile, Email, Password
    fun signUpPatient(
        name: String,
        mobile: String,
        email: String,
        pass: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: ""
                    val userData = mapOf(
                        "uid" to userId,
                        "name" to name,
                        "mobile" to mobile,
                        "email" to email,
                        "role" to "patient"
                    )
                    // Firestore mein patient profile save karna
                    firestore.collection("users").document(userId)
                        .set(userData)
                        .addOnSuccessListener { onResult(true, null) }
                        .addOnFailureListener { e -> onResult(false, e.message) }
                } else {
                    onResult(false, task.exception?.message)
                }
            }
    }

    // Patient Login
    fun loginUser(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.message)
                }
            }
    }
}
