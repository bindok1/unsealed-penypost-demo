package com.apps.unsealed.feature.addressbook.data

import android.content.Context
import com.apps.unsealed.R
import com.apps.unsealed.core.network.toUserFacingMessage
import com.apps.unsealed.core.network.unwrap
import com.apps.unsealed.feature.auth.data.AuthResult
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Retrofit

@Singleton
class AddressBookRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val crashlytics: FirebaseCrashlytics,
    private val retrofit: Retrofit,
) {
    private val api: AddressBookApi by lazy { retrofit.create(AddressBookApi::class.java) }

    /** Sends an "add to address book" request against [userId] — see [AddressBookApi.addToAddressBook]. */
    suspend fun addToAddressBook(userId: String): AuthResult<AddToAddressBookResponseData> =
        runCatching {
            api.addToAddressBook(AddToAddressBookRequest(penpalUserId = userId)).unwrap()
        }.fold(
            onSuccess = { AuthResult.Success(it) },
            onFailure = {
                crashlytics.recordException(it)
                AuthResult.Error(it.toUserFacingMessage(context, R.string.error_address_book_add), it)
            },
        )

    /** Fetches the list of saved contacts from `GET /api/v1/address-book`. */
    suspend fun getAddressBook(): AuthResult<List<AddressBookContactDto>> =
        runCatching {
            api.getAddressBook().unwrap()
        }.fold(
            onSuccess = { AuthResult.Success(it) },
            onFailure = {
                crashlytics.recordException(it)
                AuthResult.Error(it.toUserFacingMessage(context, R.string.profile_address_book_load_error), it)
            },
        )

    /** Deletes contact with [id] from `DELETE /api/v1/address-book/{id}`. */
    suspend fun deleteContact(id: String): AuthResult<Unit> =
        runCatching {
            val response = api.deleteContact(id)
            if (!response.isSuccessful) {
                error("HTTP ${response.code()}: ${response.message()}")
            }
        }.fold(
            onSuccess = { AuthResult.Success(Unit) },
            onFailure = {
                crashlytics.recordException(it)
                AuthResult.Error(it.toUserFacingMessage(context, R.string.profile_address_book_delete_error), it)
            },
        )
}
