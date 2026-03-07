package user.auth

import user.model.UserSession

/**
 * Define component that provide authentication functionality.
 */
interface AuthProvider {
    /**
     * Register a new account with [username] and [password].
     *
     * @return [UserSession] of the newly created account for further authentication.
     *         Returns `null` if registration is failed.
     */
    suspend fun register(username: String, password: String): Result<UserSession>

    /**
     * Login with [username] and [password].
     *
     * @return [UserSession] which is used for further authentication.
     *         Returns `null` if login is failed (i.e., wrong credentials or user doesn't exist).
     */
    suspend fun login(username: String, password: String): Result<UserSession>

    /**
     * Login with admin account, should always succeed.
     */
    suspend fun adminLogin(): UserSession

    /**
     * Check whether a user with [username] exists.
     */
    suspend fun doesUsernameExist(username: String): Boolean

    /**
     * Check whether [username] is viable for registration.
     */
    suspend fun isUsernameAvailable(username: String): Boolean

    /**
     * Check whether a user with [email] exists.
     */
    suspend fun doesEmailExist(email: String): Boolean

    /**
     * Check whether [email] is viable for registration.
     */
    suspend fun isEmailAvailable(email: String): Boolean
}
