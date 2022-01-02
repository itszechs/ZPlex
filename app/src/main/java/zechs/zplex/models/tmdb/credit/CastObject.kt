package zechs.zplex.models.tmdb.credit

import androidx.annotation.Keep
import zechs.zplex.models.tmdb.entities.Media

@Keep
data class CastObject(
    val gender: Int?,
    val known_for: List<Media>,
    val name: String?,
    val profile_path: String?,
    val biography: String?,
    val birthday: String?,
    val deathday: String?,
    val place_of_birth: String?,
    val job: String?
) {
    val genderName
        get() = when (gender) {
            0 -> "Others"
            1 -> "Female"
            2 -> "Male"
            else -> "Unknown"
        }
}