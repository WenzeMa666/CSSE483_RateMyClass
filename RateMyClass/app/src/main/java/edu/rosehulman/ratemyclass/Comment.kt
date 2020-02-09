package edu.rosehulman.ratemyclass

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Exclude

data class Comment (var author: String = "",
                    var content: String = "",
                    var courseNumber: String = "",
                    var dept: String = "",
                    var difficulty: String = "",
                    var learning: String = "",
                    var overall: String = "",
                    var professor: String = "",
                    var workload: String = "") {
    @get:Exclude
    var id = ""

    companion object {
        fun fromSnapshot(snapshot: DocumentSnapshot): Comment{
            val comment = snapshot.toObject(Comment::class.java)!!
            comment.id = snapshot.id
            return comment
        }
    }
}