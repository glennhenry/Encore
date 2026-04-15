package testHelper

import encore.datastore.collection.PlayerAccount
import encore.account.model.Profile
import encore.account.model.ServerMetadata
import encore.utils.hash
import io.ktor.util.date.*

fun createAccount(playerId: String, username: String, password: String): PlayerAccount {
    return PlayerAccount(
        playerId = playerId,
        username = username,
        email = "$username@email.com",
        hashedPassword = hash(password),
        profile = createProfile(playerId),
        metadata = ServerMetadata()
    )
}

fun createProfile(playerId: String): Profile {
    val now = getTimeMillis()
    return Profile(
        playerId = playerId,
        createdAt = now,
        lastActiveAt = now
    )
}
