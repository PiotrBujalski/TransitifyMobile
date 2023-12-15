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
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShopMenuActivity : AppCompatActivity() {

    private lateinit var balanceTextView: TextView
    private lateinit var profileImageButton: ImageButton
    private lateinit var ticketsImageButton: ImageButton
    private lateinit var shopImageButton: ImageButton
    private lateinit var walletImageButton: ImageButton

    private lateinit var myButtonDiscounted20: Button
    private lateinit var myButtonDiscounted40: Button
    private lateinit var myButtonNormal20: Button
    private lateinit var myButtonNormal40: Button

    private lateinit var mongoClient: MongoClient
    private lateinit var mongoDatabase: MongoDatabase
    private lateinit var usersCollection: MongoCollection<Document>
    private lateinit var ticketsCollection: MongoCollection<Document>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shop_menu)

        balanceTextView = findViewById(R.id.balanceTextView)
        profileImageButton = findViewById(R.id.profileImageButton)
        ticketsImageButton = findViewById(R.id.ticketsImageButton)
        shopImageButton = findViewById(R.id.shopImageButton)
        walletImageButton = findViewById(R.id.walletImageButton)

        myButtonDiscounted20 = findViewById(R.id.myButtonDiscounted20)
        myButtonDiscounted40 = findViewById(R.id.myButtonDiscounted40)
        myButtonNormal20 = findViewById(R.id.myButtonNormal20)
        myButtonNormal40 = findViewById(R.id.myButtonNormal40)

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

        profileImageButton.setOnClickListener {
            showPopupMenu(it)
        }

        val userId = intent.getIntExtra("userId", -1)

        ticketsImageButton.setOnClickListener {
            val intent = Intent(this@ShopMenuActivity, TicketsMenuActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
            finish()
        }

        shopImageButton.setOnClickListener {
            val intent = Intent(this@ShopMenuActivity, ShopMenuActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
            finish()
        }

        walletImageButton.setOnClickListener {
            val intent = Intent(this@ShopMenuActivity, PaymentMenuActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
            finish()
        }

        myButtonDiscounted20.setOnClickListener {
            buyTicket("Ulgowy", 20, 2.20)
        }

        myButtonDiscounted40.setOnClickListener {
            buyTicket("Ulgowy", 40, 2.80)
        }

        myButtonNormal20.setOnClickListener {
            buyTicket("Normalny", 20, 4.40)
        }

        myButtonNormal40.setOnClickListener {
            buyTicket("Normalny", 40, 5.60)
        }
    }

    private fun buyTicket(type: String, time: Int, price: Double) {
        val userId = intent.getIntExtra("userId", -1)

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val userDocument = usersCollection.find(Document("UserId", userId)).first()

                if (userDocument != null) {
                    val currentBalance = userDocument.getDouble("Balance") ?: 0.0

                    if (currentBalance >= price) {
                        val highestTicketId =
                            ticketsCollection.find().sort(Document("TicketId", -1)).limit(1)
                                .first()?.getInteger("TicketId") ?: 0
                        val ticketId = highestTicketId + 1

                        val ticketDocument = Document("UserId", userId)
                            .append("TicketId", ticketId)
                            .append("Type", type)
                            .append("Time", time)
                            .append("Price", price)
                            .append("TimeActivation", null)
                            .append("TimeExpiration", null)
                            .append("IsActive", false)

                        insertTicket(ticketDocument)
                        updateBalance(userId, price)

                    } else {
                        withContext(Dispatchers.Main) {
                            showToast("Not enough balance to buy the ticket!")
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Error buying ticket: ${e.message}")
            }
        }
    }

    private fun insertTicket(ticketDocument: Document) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val ticketsCollection = mongoDatabase.getCollection("Tickets")
                ticketsCollection.insertOne(ticketDocument)

                withContext(Dispatchers.Main) {
                    showToast("Ticket bought successfully!")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Error inserting ticket: ${e.message}")
            }
        }
    }

    private fun updateBalance(userId: Int, amount: Double) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val userDocument = usersCollection.find(Document("UserId", userId)).first()

                if (userDocument != null) {
                    val currentBalance = userDocument.getDouble("Balance") ?: 0.0
                    val newBalance = currentBalance - amount

                    usersCollection.updateOne(
                        Document("UserId", userId),
                        Document("\$set", Document("Balance", newBalance))
                    )

                    withContext(Dispatchers.Main) {
                        val formattedBalance = String.format("%.2f", newBalance)
                        balanceTextView.text = "Balance: $formattedBalance zł"
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Error updating balance: ${e.message}")
            }
        }
    }

    private fun fetchUserBalance() {
        val userId = intent.getIntExtra("userId", -1)

        GlobalScope.launch(Dispatchers.IO) {
            val userDocument = usersCollection.find(Document("UserId", userId)).first()

            if (userDocument != null) {
                val currentBalance = userDocument.getDouble("Balance") ?: 0.0

                withContext(Dispatchers.Main) {
                    val formattedBalance = String.format("%.2f", currentBalance)
                    balanceTextView.text = "Balance: $formattedBalance zł"
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
        val intent = Intent(this, LoginAndRegisterActivity::class.java)
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