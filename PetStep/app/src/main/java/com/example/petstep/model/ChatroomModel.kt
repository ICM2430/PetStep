package com.example.petstep.model

import com.google.firebase.Timestamp

class ChatroomModel() {
    lateinit var chatroomId : String
    var userIds: MutableList<String> = mutableListOf()
    lateinit var lastMessageTimestamp : Timestamp
    lateinit var lastMessageSenderId : String

    constructor(chatroomId: String, userIds: MutableList<String>, lastMessageTimestamp: Timestamp, lastMessageSenderId: String) : this() {
        this.chatroomId = chatroomId
        this.userIds = userIds
        this.lastMessageTimestamp = lastMessageTimestamp
        this.lastMessageSenderId = lastMessageSenderId
    }

}