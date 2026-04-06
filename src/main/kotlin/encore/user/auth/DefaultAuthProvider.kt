package encore.user.auth

import encore.datastore.PlayerCreationSubunit
import encore.fancam.Fancam
import encore.user.AdminData
import encore.user.PlayerAccountRepository
import encore.user.model.UserSession

/**
 * Default auth provider where authentication is handled typically.
 *
 * This should be replaced or modified later.
 */
class DefaultAuthProvider(
    private val creationSubunit: PlayerCreationSubunit,
    private val playerAccountRepository: PlayerAccountRepository,
    private val sessionManager: SessionManager
) : AuthProvider {
    override suspend fun register(username: String, password: String): Result<UserSession> {
        val pid = creationSubunit.createPlayer(username, password)
        return Result.success(sessionManager.create(userId = pid))
    }

    override suspend fun login(username: String, password: String): Result<UserSession> {
        val result = playerAccountRepository.verifyCredentials(username, password)
        result.onFailure {
            Fancam.error { "Failure on verifyCredentials for username=$username: ${it.message}" }
            return Result.failure(it)
        }
        return Result.success(sessionManager.create(result.getOrThrow()))
    }

    override suspend fun adminLogin(): UserSession {
        return sessionManager.create(AdminData.PLAYER_ID)
    }

    override suspend fun doesUsernameExist(username: String): Boolean {
        val result = playerAccountRepository.doesUsernameExist(username)
        return result.getOrElse {
            Fancam.error { "Failure on doesUsernameExist for username=$username: ${it.message}" }
            true // check error -> assume username exists
        }
    }

    override suspend fun isUsernameAvailable(username: String): Boolean {
        val result = playerAccountRepository.isUsernameAvailable(username)
        return result.getOrElse {
            Fancam.error { "Failure on isUsernameAvailable for username=$username: ${it.message}" }
            false // check error -> assume username not available
        }
    }

    override suspend fun doesEmailExist(email: String): Boolean {
        val result = playerAccountRepository.doesEmailExist(email)
        return result.getOrElse {
            Fancam.error { "Failure on doesEmailExist for email=$email: ${it.message}" }
            true // check error -> assume email exists
        }
    }

    override suspend fun isEmailAvailable(email: String): Boolean {
        val result = playerAccountRepository.isEmailAvailable(email)
        return result.getOrElse {
            Fancam.error { "Failure on isEmailAvailable for email=$email: ${it.message}" }
            false // check error -> assume email not available
        }
    }
}
