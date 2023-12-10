package com.example.transitifymobile

import com.mongodb.client.MongoClients
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoDatabase

object MongoDBHelper {

    private const val DATABASE_NAME = "TransitifyMobile"
    private const val CONNECTION_STRING = "mongodb://10.0.2.2:27017"

    fun connect(): MongoClient {
        return MongoClients.create(CONNECTION_STRING)
    }

    fun getDatabase(mongoClient: MongoClient): MongoDatabase {
        return mongoClient.getDatabase(DATABASE_NAME)
    }
}
