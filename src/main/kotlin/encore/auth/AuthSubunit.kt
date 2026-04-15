package encore.auth

import com.toxicbakery.bcrypt.Bcrypt
import encore.fancam.Fancam
import encore.session.SessionSubunit
import encore.subunit.Subunit
import encore.subunit.scope.ServerScope
import encore.account.AccountRepository
import game.AdminData
import encore.account.BlankAccountRepository
import encore.account.PlayerCreationSubunit
import encore.session.UserSession
import encore.utils.Outcome
import encore.utils.toOutcome
import kotlin.io.encoding.Base64

/**
 * Represent a server-scoped subunit that handles authentication.
 *
 * Dependency:
 * - [AccountRepository] for access to the account store.
 * - [PlayerCreationSubunit] to create account during registration.
 * - [SessionSubunit] to create session after successful registration or login.
 */
class AuthSubunit(
    private val accountRepository: AccountRepository,
    private val creationSubunit: PlayerCreationSubunit,
    private val sessionSubunit: SessionSubunit
): Subunit<ServerScope> {
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
            Fancam.trace { "Registration success for $username" }
            val session = sessionSubunit.create(playerId)
            return Outcome.Ok(session)
        } catch (e: Throwable) {
            Fancam.error(e) { "Registration failed for $username" }
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
        val result = accountRepository.getCredentials(username)
        return result
            .onFailure {
                Fancam.error(it) { "Login failed: internal repository error for $username" }
                return Outcome.Fail
            }
            .toOutcome { credentials ->
                if (credentials == null) {
                    Fancam.warn { "Login failed: account not found for $username: " }
                    return Outcome.Ok(LoginResult.AccountNotFound("Account not found for $username"))
                }

                if (verifyPassword(password, credentials.hashedPassword)) {
                    Fancam.trace { "Login success for $username" }
                    val session = sessionSubunit.create(credentials.playerId)
                    return Outcome.Ok(LoginResult.Success(session))
                } else {
                    Fancam.trace { "Login failed: wrong password for $username" }
                    return Outcome.Ok(LoginResult.InvalidCredentials("Wrong password for $username"))
                }
            }
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
     * - `false` if there is an internal repository error.
     * - `false` if it is already taken.
     * - `false` if it contains some prohibited words.
     * - Otherwise `true`.
     */
    suspend fun isUsernameAvailable(username: String): Boolean {
        val exists = accountRepository.usernameExists(username).getOrElse {
            Fancam.error(it) {
                "Username check failed: internal repository error for $username"
            }
            return false
        }

        if (exists) {
            Fancam.trace { "Username $username is already taken" }
            return false
        }

        // username does not exist, check prohibited words
        val prohibitedWords = emptySet<String>()
        val triggeredWord = prohibitedWords.firstOrNull { word ->
            username.contains(word, ignoreCase = true)
        }

        if (triggeredWord != null) {
            Fancam.trace {
                "Prohibited words triggered on $username by word $triggeredWord"
            }
            return false
        }

        Fancam.trace { "Username $username is available" }
        return true
    }

    /**
     * Check whether the [email] is available.
     *
     * Returns:
     * - `false` if there is an internal repository error.
     * - `false` if it is already taken.
     * - `false` if it isn't a valid email.
     * - Otherwise `true`.
     *
     * Note: depending on the context, duplicate email may be allowed.
     */
    suspend fun isEmailAvailable(email: String): Boolean {
        val exists = accountRepository.emailExists(email).getOrElse {
            Fancam.error(it) {
                "Email check failed: internal repository error for $email"
            }
            return false
        }

        // depending on the context, duplicate email may be allowed
        if (exists) {
            Fancam.trace { "Email $email is already taken" }
            return false
        }

        // email does not exist, check email validity
        val isEmailValid = email.contains("@")

        if (!isEmailValid) {
            Fancam.trace { "Invalid email: $email" }
            return false
        }

        Fancam.trace { "Email $email is available" }
        return true
    }

    override suspend fun debut(scope: ServerScope): Result<Unit> = Result.success(Unit)
    override suspend fun disband(scope: ServerScope): Result<Unit> = Result.success(Unit)

    companion object {
        /**
         * Creates a test instance of [AuthSubunit].
         *
         * @param accountRepository use [BlankAccountRepository] when not under test.
         * @param creationSubunit created via [PlayerCreationSubunit.createForTest].
         * @param sessionSubunit created via [SessionSubunit.createForTest].
         */
        fun createForTest(
            accountRepository: AccountRepository = BlankAccountRepository(),
            creationSubunit: PlayerCreationSubunit = PlayerCreationSubunit.createForTest(),
            sessionSubunit: SessionSubunit = SessionSubunit.createForTest()
        ): AuthSubunit {
            return AuthSubunit(accountRepository, creationSubunit, sessionSubunit)
        }
    }
}
