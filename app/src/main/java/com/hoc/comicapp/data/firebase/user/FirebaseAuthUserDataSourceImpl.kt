package com.hoc.comicapp.data.firebase.user

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.hoc.comicapp.data.firebase.entity._User
import com.hoc.comicapp.domain.thread.CoroutinesDispatcherProvider
import com.hoc.comicapp.domain.thread.RxSchedulerProvider
import com.hoc.comicapp.utils.Either
import com.hoc.comicapp.utils.Optional
import com.hoc.comicapp.utils.fold
import com.hoc.comicapp.utils.left
import com.hoc.comicapp.utils.map
import com.hoc.comicapp.utils.right
import com.hoc.comicapp.utils.snapshots
import com.hoc.comicapp.utils.toOptional
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber

class FirebaseAuthUserDataSourceImpl(
  private val firebaseAuth: FirebaseAuth,
  private val firebaseStorage: FirebaseStorage,
  private val firebaseFirestore: FirebaseFirestore,
  private val rxSchedulerProvider: RxSchedulerProvider,
  private val dispatcherProvider: CoroutinesDispatcherProvider
) : FirebaseAuthUserDataSource {
  override fun userObservable(): Observable<Either<Throwable, _User?>> {
    val uidObservable = Observable
      .create { emitter: ObservableEmitter<Optional<String>> ->
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
          if (!emitter.isDisposed) {
            val uid = auth.currentUser.toOptional().map { it.uid }
            emitter.onNext(uid)
          }
        }

        firebaseAuth.addAuthStateListener(authStateListener)
        emitter.setCancellable {
          firebaseAuth.removeAuthStateListener(authStateListener)
          Timber.d("Remove auth state listener")
        }
      }

    return uidObservable
      .distinctUntilChanged()
      .switchMap { uidOptional ->
        uidOptional.fold(
          ifEmpty = { Observable.just(null.right()) },
          ifSome = { uid ->
            firebaseFirestore
              .document("users/$uid")
              .snapshots()
              .map { it.toObject(_User::class.java)?.right() as Either<Throwable, _User?> }
              .onErrorReturn { it.left() }
              .subscribeOn(rxSchedulerProvider.io)
          }
        )
      }
      .subscribeOn(rxSchedulerProvider.io)
      .doOnNext { Timber.d("User = $it") }
      .replay(1)
      .refCount()
  }

  override suspend fun signOut() {
    withContext(dispatcherProvider.io) { firebaseAuth.signOut() }
  }

  override suspend fun register(email: String, password: String, fullName: String, avatar: Uri?) {
    withContext(dispatcherProvider.io) {

      val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
      val user = authResult.user ?: throw IllegalStateException("User is null")

      coroutineScope {
        val uploadPhotoDeferred = async {
          if (avatar != null) {
            Timber.d("Upload start")
            val reference = firebaseStorage.getReference("avatar_images/${user.uid}")
            reference.putFile(avatar).await()
            reference.downloadUrl.await().also { Timber.d("Upload done") }
          } else {
            null
          }
        }

        firebaseFirestore
          .document("users/${user.uid}")
          .set(
            _User(
              uid = user.uid,
              displayName = fullName,
              photoURL = "",
              email = email
            )
          )
          .await()

        val photoUri = uploadPhotoDeferred.await()

        awaitAll(
          async {
            user
              .updateProfile(
                UserProfileChangeRequest
                  .Builder()
                  .setDisplayName(fullName)
                  .setPhotoUri(photoUri)
                  .build()
              )
              .await()
          },
          async {
            firebaseFirestore
              .document("users/${user.uid}")
              .update("photo_url", photoUri?.toString() ?: "")
              .await()
          }
        )

      }
    }
  }

  override suspend fun login(email: String, password: String) {
    withContext(dispatcherProvider.io) {
      firebaseAuth.signInWithEmailAndPassword(email, password).await()
    }
  }
}