package com.stacktoheap.thepill

import org.neo4j.kernel.configuration.Config
import org.neo4j.kernel.extension.ExtensionType
import org.neo4j.kernel.extension.KernelExtensionFactory
import org.neo4j.kernel.impl.spi.KernelContext
import org.neo4j.kernel.lifecycle.Lifecycle
import org.neo4j.kernel.lifecycle.LifecycleAdapter
import org.neo4j.logging.internal.LogService

class ThePillExtensionFactory : KernelExtensionFactory<ThePillExtensionFactory.Dependencies>(ExtensionType.DATABASE, "thepill") {
    override fun newInstance(context: KernelContext, dependencies: Dependencies): Lifecycle {
        return ThePillLifecycle(dependencies.config(), dependencies.log())
    }

    interface Dependencies {
        fun config(): Config
        fun log(): LogService
    }
}

class ThePillLifecycle(val configuration: Config, private val log: LogService): LifecycleAdapter() {
    private val logger = log.getUserLog(ThePillLifecycle::class.java)
    override fun start() {
        logger.info("Intializing thepill procedure")
        val settings = ThePillProcedure.initSettings(configuration.raw)
        logger.info("Thepill using settings - $settings")
        logger.info("Intialized thepill procedure")

    }
}