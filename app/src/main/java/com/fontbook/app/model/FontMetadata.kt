package com.fontbook.app.model

/**
 * Rich font metadata parsed from the font file's tables (via FontBox).
 * Every text field is nullable; the UI hides whatever a given font omits —
 * the bundled system fonts populate these inconsistently.
 */
data class FontMetadata(
    val format: String?,
    val numGlyphs: Int?,
    val unitsPerEm: Int?,
    // `name` table
    val family: String?,
    val subfamily: String?,
    val fullName: String?,
    val postScriptName: String?,
    val version: String?,
    val copyright: String?,
    val manufacturer: String?,
    val designer: String?,
    val description: String?,
    val vendorUrl: String?,
    val designerUrl: String?,
    val license: String?,
    val licenseUrl: String?,
    val trademark: String?,
    // OS/2 table
    val vendorId: String?,
    val weightClass: String?,
    val widthClass: String?,
    val embedding: String?,
    // `head` table
    val created: String?,
    val modified: String?,
    // file
    val fileName: String,
    val filePath: String,
    val fileSizeBytes: Long,
)
