package com.fullstackdiv.flighttiket.contoller

import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fullstackdiv.flighttiket.R
import com.fullstackdiv.flighttiket.contoller.adapter.TicketsAdapter
import com.fullstackdiv.flighttiket.rest.ApiClient
import com.fullstackdiv.flighttiket.rest.ApiService
import com.fullstackdiv.flighttiket.rest.model.Price
import com.fullstackdiv.flighttiket.rest.model.Ticket
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit




class MainActivity : AppCompatActivity(), TicketsAdapter.TicketsAdapterListener {

    private val disposable = CompositeDisposable()

    private var apiService: ApiService? = null
    private var mAdapter: TicketsAdapter? = null
    private val ticketsList = ArrayList<Ticket>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_action_back)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        supportActionBar!!.title = "$from > $to"

        apiService = ApiClient.client.create(ApiService::class.java)

        mAdapter = TicketsAdapter(this, ticketsList, this)


        recycler_view.layoutManager = GridLayoutManager(this, 1)
        recycler_view.addItemDecoration(GridSpacingItemDecoration(1, dpToPx(5), true))
        recycler_view.itemAnimator = DefaultItemAnimator()
        recycler_view.adapter = mAdapter

        getData()

        test()
    }

    fun test(){
        Observable.interval(1, TimeUnit.SECONDS)
            .take(30) // up to 30 items
            .map { v -> v + 1 } // shift it to 1 .. 30
            .subscribe { println(it) }

        Thread.sleep(35000)
    }

    fun getData(){
        val ticketsObservable = getTickets(from, to).replay()

        /**
         * Fetching all tickets first
         * Observable emits List<Ticket> at once
         * All the items will be added to RecyclerView
        </Ticket> */
        disposable.add(
            ticketsObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<List<Ticket>>() {
                    override fun onNext(tickets: List<Ticket>) {
                        // Refreshing list
                        ticketsList.clear()
                        ticketsList.addAll(tickets)
                        mAdapter!!.notifyDataSetChanged()
                    }

                    override fun onError(e: Throwable) {
                        showError(e)
                    }

                    override fun onComplete() {
                        println("ALL Ticket Fetched")
                        recycler_view.visibility = View.VISIBLE
                    }
                })
        )

        /**
         * Fetching individual ticket price
         * First FlatMap converts single List<Ticket> to multiple emissions
         * Second FlatMap makes HTTP call on each Ticket emission
        </Ticket> */
        disposable.add(
            ticketsObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                /**
                 * Converting List<Ticket> emission to single Ticket emissions
                </Ticket> */
                .flatMap { t -> Observable.fromIterable(t) }
                /**
                 * Fetching price on each Ticket emission
                 */
                .flatMap { t -> getPriceObservable(t) }
                .subscribeWith(object : DisposableObserver<Ticket>() {
                    override fun onNext(ticket: Ticket) {
                        val position = ticketsList.indexOf(ticket)

                        if (position == -1) {
                            // Ticket not found in the list
                            // This shouldn't happen

                            return
                        }

                        ticketsList[position] = ticket
                        mAdapter!!.notifyItemChanged(position)
                    }

                    override fun onError(e: Throwable) {
                        showError(e)
                    }

                    override fun onComplete() {
                        println("All Price Fetched")
                    }
                })
        )

        // Calling connect to start emission
        ticketsObservable.connect()

        setAct()
    }

    fun setAct(){
        swr.isRefreshing = false
        swr.setOnRefreshListener {
            recycler_view.visibility = View.INVISIBLE
            getData()
        }
    }

    /**
     * Making Retrofit call to fetch all tickets
     */
    private fun getTickets(from: String, to: String): Observable<List<Ticket>> {
        return apiService!!.searchTickets(from, to)
            .toObservable()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * Making Retrofit call to get single ticket price
     * get price HTTP call returns Price object, but
     * map() operator is used to change the return type to Ticket
     */
    private fun getPriceObservable(ticket: Ticket): Observable<Ticket> {
        return apiService!!
            .getPrice(ticket.flightNumber!!, ticket.from!!, ticket.to!!)
            .toObservable()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map { t: Price -> ticket.price = t
            ticket}
    }



    inner class GridSpacingItemDecoration(
        private val spanCount: Int,
        private val spacing: Int,
        private val includeEdge: Boolean
    ) : RecyclerView.ItemDecoration() {

        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            val position = parent.getChildAdapterPosition(view) // item position
            val column = position % spanCount // item column

            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount // spacing - column * ((1f / spanCount) * spacing)
                outRect.right = (column + 1) * spacing / spanCount // (column + 1) * ((1f / spanCount) * spacing)

                if (position < spanCount) { // top edge
                    outRect.top = spacing
                }
                outRect.bottom = spacing // item bottom
            } else {
                outRect.left = column * spacing / spanCount // column * ((1f / spanCount) * spacing)
                outRect.right =
                    spacing - (column + 1) * spacing / spanCount // spacing - (column + 1) * ((1f /    spanCount) * spacing)
                if (position >= spanCount) {
                    outRect.top = spacing // item top
                }
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        val r = resources
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), r.displayMetrics))
    }

    override fun onTicketSelected(contact: Ticket) {

    }

    /**
     * Snackbar shows observer error
     */
    private fun showError(e: Throwable) {
        Log.e(TAG, "showError: " + e.message)

//        val snackbar = Snackbar
//            .make(coordinatorLayout, e.message, Snackbar.LENGTH_LONG)
//        val sbView = snackbar.getView()
//        val textView = sbView.findViewById(R.id.snackbar_text)
//        textView.setTextColor(Color.YELLOW)
//        snackbar.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private val from = "DEL"
        private val to = "HYD"
    }
}