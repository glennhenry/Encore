package testHelper

import encore.datastore.collection.PlayerAccount
import encore.account.model.Profile
import encore.datastore.collection.PlayerId
import encore.utils.hash
import io.ktor.util.date.*

fun createAccount(playerId: PlayerId, username: String, password: String): PlayerAccount {
    return PlayerAccount(
        playerId = playerId,
        username = username,
        email = "$username@email.com",
        hashedPassword = hash(password),
        profile = createProfile(playerId)
    )
}

fun createProfile(playerId: PlayerId): Profile {
    val now = getTimeMillis()
    return Profile(
        playerId = playerId,
        createdAt = now,
        lastActiveAt = now
    )
}
