    package com.example.transitifymobile

    import android.content.Intent
    import androidx.appcompat.app.AppCompatActivity
    import com.mongodb.client.MongoClient
    import com.mongodb.client.MongoCollection
    import com.mongodb.client.MongoDatabase
    import org.bson.Document
    import android.os.Bundle
    import android.widget.Button
    import android.widget.EditText
    import android.widget.Toast
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
                val username = editTextUsername.text.toString()
                val enteredPassword = editTextPassword.text.toString()

                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        val userDocument = usersCollection.find(Document("username", username)).first()

                        withContext(Dispatchers.Main) {
                            if (userDocument != null) {
                                val storedPassword = userDocument.getString("password")

                                if (enteredPassword == storedPassword) {
                                    showToast("Login successful")
                                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                } else {
                                    showToast("Incorrect password")
                                }
                            } else {
                                showToast("Username not found")
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