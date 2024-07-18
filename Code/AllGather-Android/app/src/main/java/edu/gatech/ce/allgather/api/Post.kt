package edu.gatech.ce.allgather.api


class Post {
    var userId: Int = 0
    var id: Int = 0
    var title: String? = null
    var body: String? = null

    override fun toString(): String {
        return "Post(userId=$userId, id=$id, title=$title, body=$body)"
    }
}