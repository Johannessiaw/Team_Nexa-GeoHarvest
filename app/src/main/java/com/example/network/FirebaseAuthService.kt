package com.example.network

import android.app.Activity
import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.AuthResult
import java.util.concurrent.TimeUnit

/**
 * Genuine Firebase Auth service coordinating Google Sign-In, 
 * phone OTP credential verification, and live account bindings.
 */
class FirebaseAuthService {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    fun getFirebaseAuth(): FirebaseAuth = auth

    /**
     * Signs in using a Google ID token from Google Sign-In API.
     */
    fun signInWithGoogleToken(
        idToken: String,
        onSuccess: (AuthResult) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val result = task.result
                        if (result != null) {
                            onSuccess(result)
                        } else {
                            onFailure(Exception("AuthResult was empty"))
                        }
                    } else {
                        onFailure(task.exception ?: Exception("Google Sign-In credential auth failed"))
                    }
                }
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    /**
     * Triggers the Firebase PhoneAuthOptions flow for sending verification SMS codes.
     */
    fun startPhoneVerification(
        phoneNumber: String,
        activity: Activity,
        onCodeSent: (String, PhoneAuthProvider.ForceResendingToken) -> Unit,
        onVerificationCompleted: (PhoneAuthCredential) -> Unit,
        onVerificationFailed: (FirebaseException) -> Unit
    ) {
        try {
            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    Log.d("FirebaseAuthService", "onVerificationCompleted: $credential")
                    onVerificationCompleted(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Log.e("FirebaseAuthService", "onVerificationFailed", e)
                    onVerificationFailed(e)
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    Log.d("FirebaseAuthService", "onCodeSent: $verificationId")
                    onCodeSent(verificationId, token)
                }
            }

            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build()

            PhoneAuthProvider.verifyPhoneNumber(options)
        } catch (e: Exception) {
            onVerificationFailed(FirebaseException(e.message ?: "Failed to verify phone number", e))
        }
    }

    /**
     * Signs in with the verification ID and OTP code received via SMS.
     */
    fun signInWithOtpCode(
        verificationId: String,
        otpCode: String,
        onSuccess: (AuthResult) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            val credential = PhoneAuthProvider.getCredential(verificationId, otpCode)
            auth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val result = task.result
                        if (result != null) {
                            onSuccess(result)
                        } else {
                            onFailure(Exception("AuthResult was empty"))
                        }
                    } else {
                        onFailure(task.exception ?: Exception("Invalid OTP code submitted"))
                    }
                }
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    /**
     * Signs out from the active Firebase session.
     */
    fun signOut() {
        auth.signOut()
    }

    /**
     * Checks if a user is currently signed into Firebase.
     */
    fun getCurrentUser() = auth.currentUser
}
