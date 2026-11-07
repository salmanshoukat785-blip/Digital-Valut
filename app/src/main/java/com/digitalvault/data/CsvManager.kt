package com.digitalvault.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvManager {

    class Parsed(val accounts: List<Account>, val skippedRows: Int)

    fun export(accounts: List<Account>, categoryName: (Long) -> String): String {
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val sb = StringBuilder("title,username,email,password,url,notes,category,date_added\r\n")
        accounts.forEach { a ->
            sb.append(
                listOf(
                    a.title, a.username, a.email, a.password, a.url, a.notes,
                    categoryName(a.categoryId), df.format(Date(a.createdAt))
                ).joinToString(",") { quote(it) }
            ).append("\r\n")
        }
        return sb.toString()
    }

    private fun quote(s: String): String =
        if (s.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + s.replace("\"", "\"\"") + "\"" else s

    fun parse(input: String): List<List<String>> {
        val text = input.removePrefix("\uFEFF")
        val rows = ArrayList<List<String>>()
        var row = ArrayList<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length && text[i + 1] == '"') {
                        cell.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    cell.append(c)
                }
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    ',' -> { row.add(cell.toString()); cell.setLength(0) }
                    '\r' -> {}
                    '\n' -> {
                        row.add(cell.toString()); cell.setLength(0)
                        rows.add(row); row = ArrayList()
                    }
                    else -> cell.append(c)
                }
            }
            i++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) {
            row.add(cell.toString())
            rows.add(row)
        }
        return rows.filter { r -> r.any { it.isNotBlank() } }
    }

    /** Chrome, Bitwarden wagaira ke aam CSV headers bhi samajhta hai. Na-qabil-e-fahm file par null. */
    fun toAccounts(rows: List<List<String>>, categoryIdFor: (String) -> Long): Parsed? {
        if (rows.size < 2) return null
        val header = rows[0].map { it.trim().lowercase() }
        fun idx(vararg names: String) = header.indexOfFirst { it in names }
        val iTitle = idx("title", "name", "account", "site")
        val iUser = idx("username", "login", "login_username", "user")
        val iEmail = idx("email", "e-mail")
        val iPass = idx("password", "login_password", "pass")
        val iUrl = idx("url", "website", "login_uri")
        val iNotes = idx("notes", "note", "extra")
        val iCat = idx("category", "folder", "grouping", "type")
        if (iPass < 0 || (iTitle < 0 && iUrl < 0)) return null

        val out = ArrayList<Account>()
        var skipped = 0
        for (r in rows.drop(1)) {
            fun cell(i: Int): String = if (i >= 0 && i < r.size) r[i].trim() else ""
            val password = if (iPass < r.size) r[iPass] else ""
            val url = cell(iUrl)
            val title = cell(iTitle).ifBlank {
                url.substringAfter("://").substringBefore('/').removePrefix("www.")
            }
            if (password.isEmpty() || title.isBlank()) { skipped++; continue }
            out.add(
                Account(
                    title = title, username = cell(iUser), email = cell(iEmail), password = password,
                    url = url, notes = cell(iNotes), categoryId = categoryIdFor(cell(iCat))
                )
            )
        }
        return Parsed(out, skipped)
    }
}
