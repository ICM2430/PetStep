package com.example.petstep.model

import com.google.type.DateTime
import kotlin.properties.Delegates

class ReviewWalker {
    private lateinit var id: String
    private lateinit var idWalker: String
    private lateinit var idUser: String
    private lateinit var review: String
    private var calification : Int by Delegates.notNull()
    private lateinit var date: DateTime

    constructor()

    constructor(
        id: String,
        idWalker: String,
        idUser: String,
        review: String,
        calification: Int,
        date: DateTime
    ) {
        this.id = id
        this.idWalker = idWalker
        this.idUser = idUser
        this.review = review
        this.calification = calification
        this.date = date
    }


}