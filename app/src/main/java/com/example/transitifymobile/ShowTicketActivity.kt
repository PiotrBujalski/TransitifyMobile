package com.example.transitifymobile

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class ShowTicketActivity : AppCompatActivity() {

    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_ticket)

        val qrCodeBytes = intent.getByteArrayExtra("qrCodeBytes")
        userId = intent.getIntExtra("userId", -1)

        if (qrCodeBytes != null) {
            val imageView: ImageView = findViewById(R.id.qrCodeImageView)
            val bitmap = BitmapFactory.decodeByteArray(qrCodeBytes, 0, qrCodeBytes.size)
            imageView.setImageBitmap(bitmap)
        }
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this@ShowTicketActivity, TicketsMenuActivity::class.java)
        intent.putExtra("userId", userId)
        startActivity(intent)
        finish()
    }
}