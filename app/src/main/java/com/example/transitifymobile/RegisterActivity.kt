package com.example.transitifymobile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bson.Document
import java.util.regex.Pattern
import at.favre.lib.crypto.bcrypt.BCrypt

class RegisterActivity : AppCompatActivity() {

    private lateinit var editTextName: EditText
    private lateinit var editTextSurname: EditText
    private lateinit var editTextUsername: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var editTextRepeatPassword: EditText
    private lateinit var btnRegister: Button

    private lateinit var mongoClient: MongoClient
    private lateinit var mongoDatabase: MongoDatabase
    private lateinit var usersCollection: MongoCollection<Document>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val tilName = findViewById<TextInputLayout>(R.id.tilName)
        val tilSurname = findViewById<TextInputLayout>(R.id.tilSurname)
        val tilUsername = findViewById<TextInputLayout>(R.id.tilUsername)
        val tilPassword = findViewById<TextInputLayout>(R.id.tilPassword)
        val tilRepeatPassword = findViewById<TextInputLayout>(R.id.tilRepeatPassword)

        editTextName = findViewById(R.id.editTextName)
        editTextSurname = findViewById(R.id.editTextSurname)
        editTextUsername = findViewById(R.id.editTextUsername)
        editTextPassword = findViewById(R.id.editTextPassword)
        editTextRepeatPassword = findViewById(R.id.editTextRepeatPassword)
        btnRegister = findViewById(R.id.btnRegister)

        try {
            mongoClient = MongoDBHelper.connect()
            mongoDatabase = MongoDBHelper.getDatabase(mongoClient)
            usersCollection = mongoDatabase.getCollection("Users")
        } catch (e: Exception) {
            e.printStackTrace()
            showToast("Error connecting to the database: ${e.message}")
        }

        btnRegister.setOnClickListener {
            val name = editTextName.text.toString().trim()
            val surname = editTextSurname.text.toString().trim()
            val username = editTextUsername.text.toString().trim()
            val password = editTextPassword.text.toString()
            val repeatPassword = editTextRepeatPassword.text.toString()

            if (validateName(name, tilName) && validateSurname(surname, tilSurname) && validateUsername(username, tilUsername) &&
                validatePassword(password, repeatPassword, tilPassword, tilRepeatPassword)) {
                GlobalScope.launch(Dispatchers.IO) {
                    val existingUser = usersCollection.find(Document("Username", username)).first()
                    if (existingUser == null) {
                        val highestUserId = usersCollection.find().sort(Document("UserId", -1)).limit(1).first()?.getInteger("UserId") ?: 0
                        val userId = highestUserId + 1
                        val newUser = Document()
                            .append("UserId", userId)
                            .append("Username", username)
                            .append("PasswordHash", hashPassword(password))
                            .append("Name", name)
                            .append("Surname", surname)
                            .append("Balance", 0.0)

                        try {
                            usersCollection.insertOne(newUser)
                            withContext(Dispatchers.Main) {
                                showToast("Registration successful!")
                                val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                showToast("Error registering user: ${e.message}")
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            showToast("Username already exists. Please choose a different username.")
                        }
                    }
                }
            }
        }
    }

    private fun validateName(name: String, tilName: TextInputLayout): Boolean {
        return if (name.length >= 2 && Pattern.matches("[a-zA-Z]+", name)) {
            tilName.error = null
            true
        } else {
            tilName.error = "Name should be at least 2 characters long and contain only letters."
            false
        }
    }

    private fun validateSurname(surname: String, tilSurname: TextInputLayout): Boolean {
        return if (surname.length >= 2 && Pattern.matches("[a-zA-Z]+", surname)) {
            tilSurname.error = null
            true
        } else {
            tilSurname.error = "Surname should be at least 2 characters long and contain only letters."
            false
        }
    }

    private fun validateUsername(username: String, tilUsername: TextInputLayout): Boolean {
        return if (username.length >= 5) {
            tilUsername.error = null
            true
        } else {
            tilUsername.error = "Username should be at least 5 characters long."
            false
        }
    }

    private fun validatePassword(password: String, repeatPassword: String, tilPassword: TextInputLayout, tilRepeatPassword: TextInputLayout): Boolean {
        return if (password.length >= 5 && password.matches(Regex(".*\\d.*")) && password.matches(Regex(".*[a-zA-Z].*"))) {
            if (password == repeatPassword) {
                tilPassword.error = null
                tilRepeatPassword.error = null
                true
            } else {
                tilPassword.error = "Passwords do not match."
                tilRepeatPassword.error = "Passwords do not match."
                false
            }
        } else {
            tilPassword.error = "Password should be at least 5 characters long and contain at least one letter and one digit."
            tilRepeatPassword.error = "Password should be at least 5 characters long and contain at least one letter and one digit."
            false
        }
    }

    private fun hashPassword(password: String): String {
        return BCrypt.withDefaults().hashToString(12, password.toCharArray())
    }

    fun goToLogin(view: View) {
        val intent = Intent(this, LoginActivity::class.java)
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
