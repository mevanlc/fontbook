package com.fontbook.app.data

import android.content.Context
import com.fontbook.app.model.ScriptTagInfo
import org.json.JSONObject

object ScriptTagCatalog {

    private const val ASSET_NAME = "scripttag_map.json"

    fun load(context: Context): Map<String, ScriptTagInfo> = runCatching {
        val json = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        buildMap {
            val keys = root.keys()
            while (keys.hasNext()) {
                val tag = keys.next()
                put(tag.lowercase(), root.getJSONObject(tag).toInfo(tag.lowercase()))
            }
        }
    }.getOrDefault(emptyMap())

    private fun JSONObject.toInfo(tag: String): ScriptTagInfo {
        val alternates = optJSONArray("more")?.let { array ->
            buildList {
                for (i in 0 until array.length()) {
                    add(array.getJSONObject(i).toInfo(tag))
                }
            }
        }.orEmpty()

        return ScriptTagInfo(
            tag = tag,
            langNameEnglish = optText("langNameEnglish"),
            langName = optText("langName"),
            scriptName = optText("scriptName"),
            scriptNameEnglish = optText("scriptNameEnglish"),
            qbfText = optText("qbfText"),
            isoLang = optText("isoLang"),
            isoBcp47 = optText("isoBcp47"),
            alternates = alternates,
        )
    }

    private fun JSONObject.optText(name: String): String? =
        optString(name).takeIf { it.isNotBlank() }
}
