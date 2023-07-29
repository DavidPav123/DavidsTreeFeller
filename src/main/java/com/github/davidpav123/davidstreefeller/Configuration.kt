package com.github.davidpav123.davidstreefeller

import java.io.*
import java.util.*

class Configuration(file: String?, header: String?) : File(file.toString()), Cloneable {
    private val header: String?
    private val hm: HashMap<String, String> = HashMap()
    private val info: HashMap<String, String> = HashMap()

    init {
        this.header = header
    }

    fun setValue(key: String, value: Any) {
        hm[key] = value.toString()
    }

    fun getString(key: String, defaultValue: String): String {
        return hm.getOrDefault(key, defaultValue)
    }

    fun getInt(key: String, defaultValue: Int): Int {
        val str = hm.getOrDefault(key, defaultValue.toString())
        return try {
            str.toInt()
        } catch (e: NumberFormatException) {
            defaultValue
        }
    }

    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return when (hm.getOrDefault(key, defaultValue.toString())) {
            "true", "yes" -> {
                true
            }

            "false", "no" -> {
                false
            }

            else -> {
                defaultValue
            }
        }
    }

    fun setInfo(key: String, info: String) {
        this.info[key] = info
    }

    @Throws(IOException::class)
    fun saveConfig() {
        val configTxt = StringBuilder(if (header == null) "" else "#\t$header\n\n")
        val keys: Set<String> = hm.keys
        for (key in keys) {
            val value = hm[key]
            val info = info[key]
            if (info != null) {
                configTxt.append("#").append(info).append("\n")
            }
            configTxt.append(key).append(": ").append(value).append("\n\n")
        }
        if (exists()) {
            delete()
        }
        try {
            getParentFile().mkdirs()
        } catch (ignored: NullPointerException) {
        }
        createNewFile()
        val writer = BufferedWriter(FileWriter(this))
        writer.write(configTxt.toString())
        writer.close()
    }

    fun reloadConfig() {
        try {
            val reader = BufferedReader(FileReader(this))
            var line: String
            var cont = 0
            while (reader.readLine().also { line = it } != null) {
                cont++
                line = line.trim { it <= ' ' }
                if (!line.startsWith("#") && line.trim { it <= ' ' }.isNotEmpty()) {
                    val st = StringTokenizer(line, ":")
                    if (st.countTokens() != 2) {
                        reader.close()
                        throw IOException("Looks like the file content is not correct. Broken line " + cont + " (" + st.countTokens() + " tokens, should be 2)")
                    }
                    val key = st.nextToken().trim { it <= ' ' }
                    val value = st.nextToken().trim { it <= ' ' }
                    setValue(key, value)
                }
            }
            reader.close()
        } catch (_: FileNotFoundException) {
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (_: NullPointerException) {
        }
    }

    public override fun clone(): Configuration {
        return try {
            super.clone() as Configuration
        } catch (e: CloneNotSupportedException) {
            throw AssertionError()
        }
    }

    companion object {
        @Serial
        private val serialVersionUID = 115L
    }
}
