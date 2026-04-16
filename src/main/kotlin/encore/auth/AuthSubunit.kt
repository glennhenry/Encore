package encore.auth

import com.toxicbakery.bcrypt.Bcrypt
import encore.account.AccountSubunit
import encore.account.PlayerCreationSubunit
import encore.fancam.Fancam
import encore.session.SessionSubunit
import encore.session.UserSession
import encore.subunit.Subunit
import encore.subunit.scope.ServerScope
import encore.utils.Outcome
import encore.utils.fold
import game.AdminData
import kotlin.io.encoding.Base64

/**
 * Represent a server-scoped subunit that handles authentication.
 *
 * `AuthSubunit` is high-level subunit that requires other subunits to function:
 * - [AccountSubunit] to get account information for registration or login.
 * - [PlayerCreationSubunit] to create account during registration.
 * - [SessionSubunit] to create session after successful registration or login.
 */
class AuthSubunit(
    private val accountSubunit: AccountSubunit,
    private val creationSubunit: PlayerCreationSubunit,
    private val sessionSubunit: SessionSubunit
) : Subunit<ServerScope> {
    /**
     * Register an account with [username] and [password].
     *
     * This would also create a session by calling [SessionSubunit.create].
     *
     * Returns:
     * - [Outcome.Fail] if there is an internal repository error.
     * - Otherwise [Outcome.Ok] with [UserSession].
     */
    suspend fun register(username: String, password: String): Outcome<UserSession> {
        try {
            val playerId = creationSubunit.createPlayer(username, password)
            Fancam.trace { "Registration success for '$username'" }
            val session = sessionSubunit.create(playerId)
            return Outcome.Ok(session)
        } catch (e: Throwable) {
            Fancam.error(e) { "Registration failed for '$username'" }
            return Outcome.Fail
        }
    }

    /**
     * Login to the account of [username] with [password].
     *
     * On successful login, this would produce a [UserSession]
     * obtained from calling [SessionSubunit.create].
     *
     * Returns:
     * - [Outcome.Fail] if there is an internal repository error.
     * - [Outcome.Ok] with [LoginResult.AccountNotFound] if the associated
     *   player account of [username] is not found.
     * - [Outcome.Ok] with [LoginResult.InvalidCredentials] if the password
     *   does not match.
     * - Otherwise [Outcome.Ok] with [UserSession].
     */
    suspend fun login(username: String, password: String): Outcome<LoginResult> {
        val outcome = accountSubunit.getCredentials(username)
        return outcome.fold(
            onOk = { credentials ->
                if (credentials == null) {
                    Fancam.warn { "Login failed: account not found for '$username'" }
                    return Outcome.Ok(LoginResult.AccountNotFound("Account not found for '$username'"))
                }

                if (verifyPassword(password, credentials.hashedPassword)) {
                    Fancam.trace { "Login success for '$username'" }
                    val session = sessionSubunit.create(credentials.playerId)
                    Outcome.Ok(LoginResult.Success(session))
                } else {
                    Fancam.trace { "Login failed: wrong password for '$username'" }
                    Outcome.Ok(LoginResult.InvalidCredentials("Wrong password for '$username'"))
                }
            },
            onFail = {
                Fancam.error { "Login failed for '$username'" }
                Outcome.Fail
            }
        )
    }

    /**
     * Login as admin (always succeed).
     *
     * This would also create a reserved admin session.
     *
     * @return [UserSession] with fixed token of [AdminData.TOKEN].
     */
    fun loginAsAdmin(): UserSession {
        return sessionSubunit.create(AdminData.PLAYER_ID)
    }

    private fun verifyPassword(password: String, hashed: String): Boolean {
        return Bcrypt.verify(password, Base64.decode(hashed))
    }

    /**
     * Check whether the [username] is available.
     *
     * Returns:
     * - [Outcome.Fail] if there is an internal repository error.
     * - [Outcome.Ok]` = false` if it is already taken.
     * - [Outcome.Ok]` = false` if it contains some prohibited words.
     * - Otherwise [Outcome.Ok]` = true`.
     */
    suspend fun isUsernameAvailable(username: String): Outcome<Boolean> {
        val outcome = accountSubunit.usernameExists(username)
        return outcome.fold(
            onOk = { exists ->
                if (exists) {
                    Fancam.trace { "Username '$username' is already taken" }
                    return Outcome.Ok(false)
                }

                // username does not exist, check prohibited words
                val prohibitedWords = emptySet<String>()
                val triggeredWord = prohibitedWords.firstOrNull { word ->
                    username.contains(word, ignoreCase = true)
                }

                if (triggeredWord != null) {
                    Fancam.trace {
                        "Prohibited words triggered on '$username' by word $triggeredWord"
                    }
                    return Outcome.Ok(false)
                }

                Fancam.trace { "Username '$username' is available" }
                Outcome.Ok(true)
            },
            onFail = { Outcome.Fail }
        )
    }

    /**
     * Check whether the [email] is available.
     *
     * Returns:
     * - [Outcome.Fail] if there is an internal repository error.
     * - [Outcome.Ok]` = false` if it is already taken.
     * - [Outcome.Ok]` = false` if it contains some prohibited words.
     * - Otherwise [Outcome.Ok]` = true`.
     *
     * Note: depending on the context, duplicate email may be allowed.
     */
    suspend fun isEmailAvailable(email: String): Outcome<Boolean> {
        val outcome = accountSubunit.emailExists(email)
        return outcome.fold(
            onOk = { exists ->
                // depending on the context, duplicate email may be allowed
                if (exists) {
                    Fancam.trace { "Email '$email' is already taken" }
                    return Outcome.Ok(false)
                }

                // email does not exist, check email validity
                val isEmailValid = email.contains("@")

                if (!isEmailValid) {
                    Fancam.trace { "Invalid email '$email'" }
                    return Outcome.Ok(false)
                }

                Fancam.trace { "Email '$email' is available" }
                Outcome.Ok(true)
            },
            onFail = { Outcome.Fail }
        )
    }

    override suspend fun debut(scope: ServerScope): Result<Unit> = Result.success(Unit)
    override suspend fun disband(scope: ServerScope): Result<Unit> = Result.success(Unit)

    companion object {
        /**
         * Creates a test instance of [AuthSubunit].
         *
         * @param accountSubunit created via [AccountSubunit.createForTest].
         * @param creationSubunit created via [PlayerCreationSubunit.createForTest].
         * @param sessionSubunit created via [SessionSubunit.createForTest].
         */
        fun createForTest(
            accountSubunit: AccountSubunit = AccountSubunit.createForTest(),
            creationSubunit: PlayerCreationSubunit = PlayerCreationSubunit.createForTest(),
            sessionSubunit: SessionSubunit = SessionSubunit.createForTest()
        ): AuthSubunit {
            return AuthSubunit(accountSubunit, creationSubunit, sessionSubunit)
        }
    }
}
