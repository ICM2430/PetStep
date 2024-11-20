package com.example.petstep.model

import com.google.firebase.Timestamp

data class MessageModel(
    var message: String = "",
    var senderId: String = "",
    var timestamp: Timestamp = Timestamp.now()
)