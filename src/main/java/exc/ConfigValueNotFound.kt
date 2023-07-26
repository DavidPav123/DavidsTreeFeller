package exc

class ConfigValueNotFound : ConfigurationException {
    constructor() : super()
    constructor(message: String?) : super(message)

    companion object {
        private const val serialVersionUID = 1L
    }
}
