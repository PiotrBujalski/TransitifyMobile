package com.example.transitifymobile

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import org.bson.Document
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date

class TicketsMenuActivity : AppCompatActivity() {

    private lateinit var balanceTextView: TextView
    private lateinit var profileImageButton: ImageButton
    private lateinit var ticketsImageButton: ImageButton
    private lateinit var shopImageButton: ImageButton
    private lateinit var walletImageButton: ImageButton

    private lateinit var mongoClient: MongoClient
    private lateinit var mongoDatabase: MongoDatabase
    private lateinit var usersCollection: MongoCollection<Document>
    private lateinit var ticketsCollection: MongoCollection<Document>
    private lateinit var ticketsContainer: LinearLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tickets_menu)

        balanceTextView = findViewById(R.id.balanceTextView)
        profileImageButton = findViewById(R.id.profileImageButton)
        ticketsImageButton = findViewById(R.id.ticketsImageButton)
        shopImageButton = findViewById(R.id.shopImageButton)
        walletImageButton = findViewById(R.id.walletImageButton)
        ticketsContainer = findViewById(R.id.ticketsContainer)

        try {
            mongoClient = MongoDBHelper.connect()
            mongoDatabase = MongoDBHelper.getDatabase(mongoClient)
            usersCollection = mongoDatabase.getCollection("Users")
            ticketsCollection = mongoDatabase.getCollection("Tickets")
        } catch (e: Exception) {
            e.printStackTrace()
            showToast("Error connecting to the database: ${e.message}")
        }

        fetchUserBalance()

        fetchAndDisplayTickets()

        profileImageButton.setOnClickListener {
            showPopupMenu(it)
        }

        val userId = intent.getIntExtra("userId", -1)

        ticketsImageButton.setOnClickListener {
            val intent = Intent(this@TicketsMenuActivity, TicketsMenuActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
            finish()
        }

        shopImageButton.setOnClickListener {
            val intent = Intent(this@TicketsMenuActivity, ShopMenuActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
            finish()
        }

        walletImageButton.setOnClickListener {
            val intent = Intent(this@TicketsMenuActivity, PaymentMenuActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
            finish()
        }
    }

    private fun fetchAndDisplayTickets() {
        val userId = intent.getIntExtra("userId", -1)

        GlobalScope.launch(Dispatchers.IO) {
            val tickets = ticketsCollection.find(Document("UserId", userId)).toList()

            withContext(Dispatchers.Main) {
                ticketsContainer.removeAllViews()  // Clear existing views

                for (ticket in tickets) {
                    val ticketView = layoutInflater.inflate(R.layout.ticket_item, null)

                    val typeTextView: TextView = ticketView.findViewById(R.id.ticketTypeTextView)
                    val timeTextView: TextView = ticketView.findViewById(R.id.ticketTimeTextView)
                    val priceTextView: TextView = ticketView.findViewById(R.id.ticketPriceTextView)

                    val ticketType = ticket.getString("Type")
                    typeTextView.text = "$ticketType"
                    timeTextView.text = "${ticket.getInteger("Time")} minut"
                    priceTextView.text = "${ticket.getDouble("Price")} zł"

                    if (ticketType == "Ulgowy") {
                        ticketView.setBackgroundColor(resources.getColor(android.R.color.holo_orange_light))
                    } else {
                        ticketView.setBackgroundColor(resources.getColor(android.R.color.holo_green_light))
                    }

                    val activateButton: Button = ticketView.findViewById(R.id.myActivateButton)
                    val showButton: Button = ticketView.findViewById(R.id.myShowButton)

                    val isActive = ticket.getBoolean("IsActive") ?: false
                    val ticketId = ticket.getInteger("TicketId")
                    activateButton.tag = ticketId

                    val ticketTime = ticket.getInteger("Time")

                    if (isActive) {
                        activateButton.visibility = View.GONE
                        showButton.visibility = View.VISIBLE
                        showButton.setOnClickListener {
                            showToast("Show button clicked for TicketId: $ticketId")
                        }
                    } else {
                        activateButton.visibility = View.VISIBLE
                        showButton.visibility = View.GONE
                        activateButton.setOnClickListener {
                            val clickedTicketId = it.tag as? Int
                            if (clickedTicketId != null) {
                                updateTicketActivationTime(clickedTicketId, ticketTime)
                                showToast("Ticket activated successfully!")
                            }
                        }
                    }

                    ticketsContainer.addView(ticketView)
                }
            }
        }
    }

    private fun updateTicketActivationTime(ticketId: Int, ticketTime: Int) {
        GlobalScope.launch(Dispatchers.IO) {
            val currentDate = Date()
            val expirationDate = calculateExpirationDate(currentDate, ticketTime)

            ticketsCollection.updateOne(
                Document("TicketId", ticketId),
                Document("\$set",
                    Document("TimeActivation", currentDate)
                        .append("TimeExpiration", expirationDate)
                        .append("IsActive", true)
                )
            )

            withContext(Dispatchers.Main) {
                fetchAndDisplayTickets()
            }
        }
    }

    private fun calculateExpirationDate(startDate: Date, ticketTime: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.time = startDate
        calendar.add(Calendar.MINUTE, ticketTime)
        return calendar.time
    }

    private fun fetchUserBalance() {
        val userId = intent.getIntExtra("userId", -1)

        GlobalScope.launch(Dispatchers.IO) {
            val userDocument = usersCollection.find(Document("UserId", userId)).first()

            if (userDocument != null) {
                val currentBalance = userDocument.getDouble("Balance") ?: 0.0

                withContext(Dispatchers.Main) {
                    balanceTextView.text = "Balance: $currentBalance zł"
                }
            }
        }
    }

    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.menuInflater.inflate(R.menu.profile_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_logout -> {
                    logout()
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }

    private fun logout() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mongoClient.isInitialized) {
            mongoClient.close()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}