package me.rpgz.treetools.models

enum class PlanetInferModel(val displayName: String, val value: String) {
    THIRD_PARTY_API("第三方API", "third_party_api"),
    PRIVATE_MODEL("私有模型", "private_model");
    
    companion object {
        fun fromValue(value: String): PlanetInferModel {
            return entries.find { it.value == value } ?: PRIVATE_MODEL
        }
        
        fun fromDisplayName(displayName: String): PlanetInferModel {
            return entries.find { it.displayName == displayName } ?: PRIVATE_MODEL
        }
    }
}