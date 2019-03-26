package com.fullstackdiv.flighttiket.rest

import com.fullstackdiv.flighttiket.rest.model.Price
import com.fullstackdiv.flighttiket.rest.model.Ticket
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Query


/**
 * Created by Angga N P on 3/18/2019.
 */

interface ApiService {

    @GET("airline-tickets.php")
    fun searchTickets(@Query("from") from: String, @Query("to") to: String): Single<List<Ticket>>

    @GET("airline-tickets-price.php")
    fun getPrice(@Query("flight_number") flightNumber: String, @Query("from") from: String, @Query("to") to: String): Single<Price>
}