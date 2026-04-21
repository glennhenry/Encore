package encore.datastore

import com.mongodb.client.model.Filters
import encore.account.model.Profile
import encore.datastore.collection.PlayerAccount
import encore.datastore.collection.ServerObjects
import encore.datastore.collection.ServerObjectsId
import org.bson.conversions.Bson

// This file contains constants of Kotlin's data class fields' name
// that is used in Mongo queries.

/** `playerId`*/
val FieldPlayerId = PlayerAccount::playerId.name

/** `username`*/
val FieldUsername = PlayerAccount::username.name

/** `email`*/
val FieldEmail = PlayerAccount::email.name

/** `password`*/
val FieldPassword = PlayerAccount::hashedPassword.name

/** `profile`*/
val FieldProfile = PlayerAccount::profile.name

/** `profile.lastActiveAt`*/
val FieldProfileLastActive = "$FieldProfile.${Profile::lastActiveAt.name}"

/** `acts`*/
val FieldActs = ServerObjects::acts.name

/** `acts.playerId`*/
val FieldActsPlayerId = "$FieldActs.$FieldPlayerId"

/** `dbId`*/
val ServerObjectsDbId = ServerObjects::dbId.name

/** Mongo filters for `dbId` equals [ServerObjectsId]*/
val ServerObjectsFilter: Bson = Filters.eq(ServerObjectsDbId, ServerObjectsId)
