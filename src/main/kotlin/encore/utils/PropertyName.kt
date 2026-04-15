package encore.utils

/**
 * Used to create chain of property name of nested Kotlin class.
 *
 * This is used to produce `property1.property2` when relying on
 * `KProperty` operator `::`.
 *
 * For example:
 * ```
 * data class Account(
 *     val id: String,
 *     val profile: Profile
 * )
 *
 * data class Profile(
 *     val lastActive: Long
 * )
 *
 * Account::profile    // "profile"
 * Profile::lastActive // "lastActive"
 *
 * Account::profile.name
 *     .then(Profile::lastActive.name) // "profile.lastActive"
 * ```
 */
fun String.then(next: String): String {
    return "$this.$next"
}
