package com.example.lab4

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.Date

class FirebaseManager {
    private val db = Firebase.firestore

    data class PlayerRecord(
        val userName: String = "",
        val highScore: Int = 0,
        val highestLevel: Int = 1,
        val gamesPlayed: Int = 0,
        val lastPlayed: Date = Date()
    )

    // Сохранение результата игрока
    suspend fun savePlayerScore(player: PlayerRecord): Boolean {
        return try {
            db.collection("players")
                .document(player.userName)
                .set(player)
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Получение топ-10 игроков
    suspend fun getTopPlayers(limit: Int = 10): List<PlayerRecord> {
        return try {
            val querySnapshot = db.collection("players")
                .orderBy("highScore", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            querySnapshot.documents.mapNotNull { document ->
                document.toObject(PlayerRecord::class.java)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // Получение статистики конкретного игрока
    suspend fun getPlayerStats(userName: String): PlayerRecord? {
        return try {
            val document = db.collection("players")
                .document(userName)
                .get()
                .await()

            document.toObject(PlayerRecord::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Обновление данных игрока
    suspend fun updatePlayerStats(
        userName: String,
        score: Int,
        level: Int
    ): Boolean {
        return try {
            val playerRef = db.collection("players").document(userName)

            db.runTransaction { transaction ->
                val snapshot = transaction.get(playerRef)

                val currentHighScore = snapshot.getLong("highScore")?.toInt() ?: 0
                val currentHighestLevel = snapshot.getLong("highestLevel")?.toInt() ?: 1
                val currentGamesPlayed = snapshot.getLong("gamesPlayed")?.toInt() ?: 0

                val updatedData = hashMapOf<String, Any>(
                    "userName" to userName,
                    "highScore" to maxOf(currentHighScore, score),
                    "highestLevel" to maxOf(currentHighestLevel, level),
                    "gamesPlayed" to (currentGamesPlayed + 1),
                    "lastPlayed" to Date()
                )

                transaction.set(playerRef, updatedData)
                null
            }.await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}