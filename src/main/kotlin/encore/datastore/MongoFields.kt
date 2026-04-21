package encore.datastore

import com.mongodb.client.model.Filters
import encore.account.model.Profile
import encore.datastore.collection.PlayerAccount
import encore.datastore.collection.ServerObjects
import encore.datastore.collection.ServerObjectsId
import org.bson.conversions.Bson

// This file contains constants of Kotlin's data class fields' name
// that is used in Mongo queries.

// this is same for most playerId fields
val FieldPlayerId = PlayerAccount::playerId.name
val FieldUsername = PlayerAccount::username.name
val FieldEmail = PlayerAccount::email.name
val FieldPassword = PlayerAccount::hashedPassword.name
val FieldProfile = PlayerAccount::profile.name
val FieldProfileLastActive = "$FieldProfile.${Profile::lastActiveAt.name}"

val ServerObjectsDbId = ServerObjects::dbId.name
val ServerObjectsFilter: Bson = Filters.eq(ServerObjectsDbId, ServerObjectsId)
