package org.onap.dcaegen2.collectors.hvves

import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import java.io.File
import java.io.FileWriter
import java.io.IOException

/**
 * @author <a href="mailto:piotr.jaszczyk@nokia.com">Piotr Jaszczyk</a>
 * @since March 2019
 */
@Mojo(name = "hello")
class JaszczurMojo : AbstractMojo() {

    @Parameter(property = "msg", defaultValue = "from maven")
    var msg: String? = null

    @Throws(MojoExecutionException::class)
    override fun execute() {

        log.info("Hello " + msg!!)

    }

}
