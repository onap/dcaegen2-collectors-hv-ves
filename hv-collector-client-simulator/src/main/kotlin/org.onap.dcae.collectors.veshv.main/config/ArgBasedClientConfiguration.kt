package org.onap.dcae.collectors.veshv.main.config

import org.apache.commons.cli.Option
import org.apache.commons.cli.Options
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.HelpFormatter


internal object DefaultValues {
    const val MESSAGES_AMOUNT = 1
}

/**
 * @author Jakub Dudycz <jakub.dudycz@nokia.com>
 * @since June 2018
 */
internal object ArgBasedClientConfiguration {

    private val OPT_VES_PORT = Option.builder("p")
            .longOpt("port")
            .required()
            .hasArg()
            .desc("VesHvCollector port")
            .build()

    private val OPT_VES_HOST = Option.builder("h")
            .longOpt("host")
            .required()
            .hasArg()
            .desc("VesHvCollector host")
            .build()

    private val OPT_MESSAGES_AMOUNT = Option.builder("m")
            .longOpt("messages")
            .hasArg()
            .desc("Amount of messages to send")
            .build()

    private val options by lazy {
        val options = Options()
        options.addOption(OPT_VES_PORT)
        options.addOption(OPT_VES_HOST)
        options.addOption(OPT_MESSAGES_AMOUNT)
        options
    }

    fun parse(args: Array<out String>): ClientConfiguration {
        val parser = DefaultParser()

        try {
            parser.parse(options, args).run {
                return ClientConfiguration(
                        stringValue(OPT_VES_HOST),
                        intValue(OPT_VES_PORT),
                        intValueOrDefault(OPT_MESSAGES_AMOUNT, DefaultValues.MESSAGES_AMOUNT))
            }
        } catch (ex: Exception) {
            throw WrongArgumentException(ex)
        }
    }

    private fun CommandLine.intValueOrDefault(option: Option, default: Int) =
            getOptionValue(option.opt)?.toInt() ?: default

    private fun CommandLine.intValue(option: Option) =
            getOptionValue(option.opt).toInt()

    private fun CommandLine.stringValue(option: Option) =
            getOptionValue(option.opt)


    class WrongArgumentException(parent: Exception) : Exception(parent.message, parent) {
        fun printMessage() {
            println(message)
        }

        fun printHelp(programName: String) {
            val formatter = HelpFormatter()
            formatter.printHelp(programName, options)
        }
    }
}
