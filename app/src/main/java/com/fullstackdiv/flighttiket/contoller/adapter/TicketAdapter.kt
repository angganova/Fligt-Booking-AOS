package com.fullstackdiv.flighttiket.contoller.adapter

import android.text.TextUtils
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.Glide
import android.view.LayoutInflater
import android.view.ViewGroup
import com.github.ybq.android.spinkit.SpinKitView
import android.widget.TextView
import android.content.Context
import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.fullstackdiv.flighttiket.R
import com.fullstackdiv.flighttiket.rest.model.Ticket


/**
 * Created by Angga N P on 3/18/2019.
 */


class TicketsAdapter(
    private val context: Context,
    private val contactList: List<Ticket>,
    private val listener: TicketsAdapterListener
) : RecyclerView.Adapter<TicketsAdapter.MyViewHolder>() {

    inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var airlineName: TextView = view.findViewById(R.id.airline_name)
        var logo: ImageView = view.findViewById(R.id.logo)
        var stops: TextView = view.findViewById(R.id.number_of_stops)
        var seats: TextView = view.findViewById(R.id.number_of_seats)
        var departure: TextView = view.findViewById(R.id.departure)
        var arrival: TextView = view.findViewById(R.id.arrival)
        var duration: TextView = view.findViewById(R.id.duration)
        var price: TextView = view.findViewById(R.id.price)
        var loader: SpinKitView = view.findViewById(R.id.loader)

        init {
            view.setOnClickListener {
                // send selected contact in callback
                listener.onTicketSelected(contactList[adapterPosition]) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ticket, parent, false)

        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val ticket = contactList[position]

        Glide.with(context)
            .load(ticket.airline!!.logo)
            .apply(RequestOptions.circleCropTransform())
            .into(holder.logo)

        holder.airlineName.text = ticket.airline!!.name

        holder.departure.text = ticket.departure + " Dep"
        holder.arrival.text = ticket.arrival + " Dest"

        holder.duration.text = ticket.flightNumber
        holder.duration.append(", " + ticket.duration)
        holder.stops.text = "${ticket.numberOfStops} Stops"

        if (!TextUtils.isEmpty(ticket.instructions)) {
            holder.duration.append(", " + ticket.instructions)
        }

        if (ticket.price != null) {
            holder.price.text = "Rp. " + String.format("%.0f", ticket.price!!.price)
            holder.seats.text = ticket.price!!.seats + " Seats"
            holder.loader.visibility = View.INVISIBLE
            holder.price.visibility = View.VISIBLE
            holder.seats.visibility = View.VISIBLE
        } else {
            holder.loader.visibility = View.VISIBLE
            holder.price.visibility = View.INVISIBLE
            holder.seats.visibility = View.INVISIBLE
        }
    }

    interface TicketsAdapterListener {
        fun onTicketSelected(contact: Ticket)
    }

    override fun getItemCount(): Int {
        return contactList.size
    }
}