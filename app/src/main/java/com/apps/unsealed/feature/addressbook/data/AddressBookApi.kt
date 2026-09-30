package com.apps.unsealed.feature.addressbook.data

import com.apps.unsealed.core.network.ApiResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface AddressBookApi {

    /**
     * Sends an "add to address book" request against another user — see
     * `docs/be/address_book_api.md` §"Add to Address Book (from Public Profile)".
     * On success, both users are mutually added to each other's address book.
     */
    @POST("api/v1/address-book")
    suspend fun addToAddressBook(@Body body: AddToAddressBookRequest): ApiResponse<AddToAddressBookResponseData>

    /**
     * Lists saved penpal contacts in viewer's address book.
     */
    @GET("api/v1/address-book")
    suspend fun getAddressBook(): ApiResponse<List<AddressBookContactDto>>

    /**
     * Deletes a contact from viewer's address book.
     */
    @DELETE("api/v1/address-book/{id}")
    suspend fun deleteContact(@Path("id") id: String): Response<Unit>
}
