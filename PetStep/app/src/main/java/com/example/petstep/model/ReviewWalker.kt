package com.example.petstep.model

import com.google.type.DateTime
import java.util.Date
import kotlin.properties.Delegates

class ReviewWalker {
    var id: String = ""
        get() = field
        set(value) { field = value }

    var idWalker: String = ""
        get() = field
        set(value) { field = value }

    var idUser: String = ""
        get() = field
        set(value) { field = value }

    var review: String = ""
        get() = field
        set(value) { field = value }

    var calification: Int = 0
        get() = field
        set(value) { field = value }

    var date: Date = Date()
        get() = field
        set(value) { field = value }

    constructor()

    constructor(
        id: String,
        idWalker: String,
        idUser: String,
        review: String,
        calification: Int,
        date: Date
    ) {
        this.id = id
        this.idWalker = idWalker
        this.idUser = idUser
        this.review = review
        this.calification = calification
        this.date = date
    }


}