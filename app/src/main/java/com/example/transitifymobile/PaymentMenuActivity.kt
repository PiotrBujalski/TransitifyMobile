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
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PaymentMenuActivity : AppCompatActivity() {

    private lateinit var editTextAmount: EditText
    private lateinit var btnAddFunds: Button
    private lateinit var balanceTextView: TextView
    private lateinit var profileImageButton: ImageButton
    private lateinit var ticketsImageButton: ImageButton
    private lateinit var shopImageButton: ImageButton
    private lateinit var walletImageButton: ImageButton

    private lateinit var mongoClient: MongoClient
    private lateinit var mongoDatabase: MongoDatabase
    private lateinit var usersCollection: MongoCollection<Document>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_menu)

        editTextAmount = findViewById(R.id.EditText)
        btnAddFunds = findViewById(R.id.myButton)
        balanceTextView = findViewById(R.id.balanceTextView)
        profileImageButton = findViewById(R.id.profileImageButton)
        ticketsImageButton = findViewById(R.id.ticketsImageButton)
        shopImageButton = findViewById(R.id.shopImageButton)
        walletImageButton = findViewById(R.id.walletImageButton)

        try {
            mongoClient = MongoDBHelper.connect()
            mongoDatabase = MongoDBHelper.getDatabase(mongoClient)
            usersCollection = mongoDatabase.getCollection("Users")
        } catch (e: Exception) {
            e.printStackTrace()
            showToast("Error connecting to the database: ${e.message}")
        }

        fetchUserBalance()

        btnAddFunds.setOnClickListener {
            val amountText = editTextAmount.text.toString().trim()

            if (amountText.isNotEmpty()) {
                val amount = amountText.toDouble()

                if (amount > 0) {
                    addFundsToUserBalance(amount)
                } else {
                    showToast("Please enter amount greater than 0.")
                }
            } else {
                showToast("Please enter a valid amount.")
            }
        }

        profileImageButton.setOnClickListener {
            showPopupMenu(it)
        }

        val userId = intent.getIntExtra("userId", -1)

        ticketsImageButton.setOnClickListener {
            val intent = Intent(this@PaymentMenuActivity, TicketsMenuActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
            finish()
        }

        shopImageButton.setOnClickListener {
            val intent = Intent(this@PaymentMenuActivity, ShopMenuActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
            finish()
        }

        walletImageButton.setOnClickListener {
            val intent = Intent(this@PaymentMenuActivity, PaymentMenuActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
            finish()
        }
    }

    private fun addFundsToUserBalance(amount: Double) {
        val userId = intent.getIntExtra("userId", -1)

        GlobalScope.launch(Dispatchers.IO) {
            val userDocument = usersCollection.find(Document("UserId", userId)).first()

            if (userDocument != null) {
                val currentBalance = userDocument.getDouble("Balance") ?: 0.0
                val newBalance = currentBalance + amount
                usersCollection.updateOne(Document("UserId", userId), Document("\$set", Document("Balance", newBalance)))

                withContext(Dispatchers.Main) {
                    showToast("Funds added successfully.")
                    fetchUserBalance()
                }
            } else {
                withContext(Dispatchers.Main) {
                    showToast("User not found.")
                }
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
