package de.thm.mni.microservices.gruppe6.user.model.persistence

import de.thm.mni.microservices.gruppe6.user.model.message.UserDTO
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Table("users")
data class User(
    @Id var id: UUID? = null,
    var username: String,
    var name: String,
    var lastName: String,
    var email: String,
    var dateOfBirth: LocalDate,
    var createTime: LocalDateTime,
    var globalRole: String,
    var lastLogin: LocalDateTime?
) {
    constructor(userDTO: UserDTO): this(
         null
        ,userDTO.username!!
        ,userDTO.name!!
        ,userDTO.lastName!!
        ,userDTO.email!!
        ,userDTO.dateOfBirth!!
        ,LocalDateTime.now()
        ,userDTO.globalRole!!
        ,null
    )

}

