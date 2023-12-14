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
import android.widget.Toast
import at.favre.lib.crypto.bcrypt.BCrypt
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var editTextUsername: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var btnLogin: Button

    private lateinit var mongoClient: MongoClient
    private lateinit var mongoDatabase: MongoDatabase
    private lateinit var usersCollection: MongoCollection<Document>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val tilUsername = findViewById<TextInputLayout>(R.id.tilUsername)
        val tilPassword = findViewById<TextInputLayout>(R.id.tilPassword)

        editTextUsername = findViewById(R.id.editTextUsername)
        editTextPassword = findViewById(R.id.editTextPassword)
        btnLogin = findViewById(R.id.btnLogin)

        try {
            mongoClient = MongoDBHelper.connect()
            mongoDatabase = MongoDBHelper.getDatabase(mongoClient)
            usersCollection = mongoDatabase.getCollection("Users")
        } catch (e: Exception) {
            e.printStackTrace()
            showToast("Error connecting to the database: ${e.message}")
        }

        btnLogin.setOnClickListener {
            val username = editTextUsername.text.toString().trim()
            val enteredPassword = editTextPassword.text.toString()

            if (validateInputs(username, enteredPassword, tilUsername, tilPassword)) {
                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        val userDocument = usersCollection.find(Document("Username", username)).first()

                        withContext(Dispatchers.Main) {
                            if (userDocument != null) {
                                val storedPasswordHash = userDocument.getString("PasswordHash")

                                if (BCrypt.verifyer().verify(enteredPassword.toCharArray(), storedPasswordHash).verified) {
                                    val userId = userDocument.getInteger("UserId")
                                    showToast("Login successful")
                                    val intent = Intent(this@LoginActivity, PaymentMenuActivity::class.java)
                                    intent.putExtra("userId", userId)
                                    startActivity(intent)
                                    finish()
                                } else {
                                    tilPassword.error = "Incorrect password"
                                    tilUsername.error = null
                                }
                            } else {
                                tilUsername.error = "Username not found"
                                tilPassword.error = null
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            showToast("Error while trying to log in")
                        }
                    }
                }
            }
        }
    }

    private fun validateInputs(username: String, password: String, tilUsername: TextInputLayout, tilPassword: TextInputLayout): Boolean {
        var isValid = true

        if (username.isEmpty()) {
            tilUsername.error = "Username is required"
            isValid = false
        } else {
            tilUsername.error = null
        }

        if (password.isEmpty()) {
            tilPassword.error = "Password is required"
            isValid = false
        } else {
            tilPassword.error = null
        }

        return isValid
    }

    fun goToRegister(view: View) {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
        finish()
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        super.onBackPressed()
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
