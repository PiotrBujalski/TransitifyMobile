package com.example.transitifymobile

import androidx.appcompat.app.AppCompatActivity
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import org.bson.Document
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PaymentMenuActivity : AppCompatActivity() {

    private lateinit var editTextAmount: EditText
    private lateinit var btnAddFunds: Button
    private lateinit var balanceTextView: TextView

    private lateinit var mongoClient: MongoClient
    private lateinit var mongoDatabase: MongoDatabase
    private lateinit var usersCollection: MongoCollection<Document>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_menu)

        editTextAmount = findViewById(R.id.EditText)
        btnAddFunds = findViewById(R.id.myButton)
        balanceTextView = findViewById(R.id.balanceTextView)

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
                addFundsToUserBalance(amount)
            } else {
                showToast("Please enter a valid amount.")
            }
        }
    }

    private fun addFundsToUserBalance(amount: Double) {
        val userId = intent.getIntExtra("userId", -1)

        GlobalScope.launch(Dispatchers.IO) {
            // Fetch the user document from the database
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
                    // Update the TextView with the user balance
                    balanceTextView.text = "Balance: $currentBalance zł"
                }
            }
        }
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
