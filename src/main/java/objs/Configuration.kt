package objs

import exc.ConfigValueNotFound
import exc.ConfigValueNotParsed
import java.io.*
import java.util.*

class Configuration : File, Cloneable {
    var header: String? = null
    private var hm: HashMap<String, String>
    private var info: HashMap<String, String>

    constructor(file: String?) : super(file) {
        hm = HashMap()
        info = HashMap()
    }

    constructor(file: String?, header: String?) : super(file) {
        hm = HashMap()
        info = HashMap()
        this.header = header
    }

    fun setValue(key: String, value: Any) {
        hm[key] = value.toString()
    }

    operator fun setValue(key: String, value: Any, info: String) {
        hm[key] = value.toString()
        this.info[key] = info
    }

    fun getString(key: String, defaultValue: String): String {
        return hm.getOrDefault(key, defaultValue)
    }

    @Throws(ConfigValueNotFound::class)
    fun getString(key: String): String {
        return hm[key] ?: throw ConfigValueNotFound("The key \"$key\" was never set in the config file.")
    }

    fun getInt(key: String, defaultValue: Int): Int {
        val str = hm.getOrDefault(key, defaultValue.toString() + "")
        return try {
            str.toInt()
        } catch (e: NumberFormatException) {
            System.err.println("Error trying to get integer value from config file")
            System.err.println("(Value \"$str\" could not be parsed to integer)")
            defaultValue
        }
    }

    @Throws(ConfigValueNotFound::class, ConfigValueNotParsed::class)
    fun getInt(key: String): Int {
        val str = hm[key] ?: throw ConfigValueNotFound("The key \"$key\" was never set in the config file.")
        return try {
            str.toInt()
        } catch (e: NumberFormatException) {
            System.err.println("")
            System.err.println("")
            throw ConfigValueNotParsed(
                "Error trying to get integer value from config file (Value \"" + str
                        + "\" could not be parsed to integer)"
            )
        }
    }

    fun getDouble(key: String, defaultValue: Double): Double {
        val str = hm.getOrDefault(key, defaultValue.toString() + "")
        return try {
            str.replace(",", ".").toDouble()
        } catch (e: NumberFormatException) {
            System.err.println("Error trying to get double value from config file")
            System.err.println("(Value \"$str\" could not be parsed to double)")
            defaultValue
        }
    }

    @Throws(ConfigValueNotParsed::class, ConfigValueNotFound::class)
    fun getDouble(key: String): Double {
        val str = hm[key] ?: throw ConfigValueNotFound("The key \"$key\" was never set in the config file.")
        return try {
            str.replace(",", ".").toDouble()
        } catch (e: NumberFormatException) {
            throw ConfigValueNotParsed(
                "Error trying to get double value from config file (Value \"" + str
                        + "\" could not be parsed to double)"
            )
        }
    }

    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        val str = hm.getOrDefault(key, defaultValue.toString() + "")
        return when (str) {
            "true", "yes" -> true
            "false", "no" -> false
            else -> {
                System.err.println("Error trying to get boolean value from config file")
                System.err.println("(Value \"$str\" could not be parsed to boolean)")
                defaultValue
            }
        }
    }

    @Throws(ConfigValueNotFound::class, ConfigValueNotParsed::class)
    fun getBoolean(key: String): Boolean {
        val str = hm[key] ?: throw ConfigValueNotFound("The key \"$key\" was never set in the config file.")
        return when (str) {
            "true", "yes" -> true
            "false", "no" -> false
            else -> throw ConfigValueNotParsed(
                "Error trying to get boolean value from config file (Value \"" + str
                        + "\" could not be parsed to boolean)"
            )
        }
    }

    fun setInfo(key: String, info: String) {
        this.info[key] = info
    }

    /**
     * @throws IOException
     */
    @Throws(IOException::class)
    fun saveConfig() {
        var configTxt = if (header == null) "" else "#\t$header\n\n"
        val keys: Set<String> = hm.keys
        for (key in keys) {
            val value = hm[key]
            val info = info[key]
            if (info != null) {
                configTxt += "#$info\n"
            }
            configTxt += "$key: $value\n\n"
        }
        if (exists()) {
            delete()
        }
        try {
            parentFile.mkdirs()
        } catch (e: NullPointerException) {
        }
        createNewFile()
        val writer = BufferedWriter(FileWriter(this))
        writer.write(configTxt)
        writer.close()
    }

    /**
     * @throws IOException
     */
    fun reloadConfig() {
        try {
            val reader = BufferedReader(FileReader(this))
            var line: String
            var cont = 0
            while (reader.readLine().also { line = it } != null) {
                cont++
                line = line.trim { it <= ' ' }
                if (!line.startsWith("#") && !line.trim { it <= ' ' }.isEmpty()) {
                    val st = StringTokenizer(line, ":")
                    if (st.countTokens() != 2) {
                        reader.close()
                        throw IOException(
                            "Looks like the file content is not correct. Broken line " + cont + " ("
                                    + st.countTokens() + " tokens, should be 2)"
                        )
                    }
                    val key = st.nextToken().trim { it <= ' ' }
                    val value = st.nextToken().trim { it <= ' ' }
                    setValue(key, value)
                }
            }
            reader.close()
        } catch (e: FileNotFoundException) {
            System.err.println("Configuration file not created yet. Skipping load.")
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val serialVersionUID = 115L
    }
}
