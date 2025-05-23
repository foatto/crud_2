package foatto.core.model.response.form

import kotlinx.serialization.Serializable

@Serializable
class FormCellComboDependedValues(
    val name: String,
    val values: List<Pair<Set<String>, List<Pair<String, String>>>>,
)