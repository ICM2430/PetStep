// MyPet.kt
package com.example.petstep.model

import android.os.Parcel
import android.os.Parcelable

data class MyPet(
    var id: String = "",
    var nombre: String = "",
    var raza: String = "",
    var edad: String = "",
    var peso: String = "",
    var userId: String = "",
    var photoUrl: String = ""
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "", 
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(nombre)
        parcel.writeString(raza)
        parcel.writeString(edad)
        parcel.writeString(peso)
        parcel.writeString(userId)
        parcel.writeString(photoUrl)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MyPet> {
        override fun createFromParcel(parcel: Parcel): MyPet {
            return MyPet(parcel)
        }

        override fun newArray(size: Int): Array<MyPet?> {
            return arrayOfNulls(size)
        }
    }

    override fun toString(): String {
        return nombre ?: "Sin nombre"
    }
}