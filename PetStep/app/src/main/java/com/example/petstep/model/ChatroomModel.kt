package com.example.petstep.model

import com.google.firebase.Timestamp

data class ChatroomModel(
    var chatroomId: String = "",
    var userIds: List<String> = listOf(),
    var lastMessageTimestamp: Timestamp = Timestamp.now(),
    var lastMessageSenderId: String = ""
)
