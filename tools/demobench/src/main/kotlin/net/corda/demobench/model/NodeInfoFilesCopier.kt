package net.corda.demobench.model

import net.corda.cordform.CordformNode
import net.corda.core.internal.createDirectories
import net.corda.core.internal.isRegularFile
import net.corda.core.internal.list
import net.corda.core.utilities.loggerFor
import rx.Observable
import rx.Scheduler
import rx.schedulers.Schedulers
import tornadofx.*
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit

/**
 * Utility class which copies nodeInfo files across a set of running nodes.
 *
 * This class will create paths that it needs to poll and to where it needs to copy files in case those
 * don't exist yet.
 */
class NodeInfoFilesCopier(scheduler: Scheduler = Schedulers.io()): Controller() {

    companion object {
        private val logger = loggerFor<NodeInfoFilesCopier>()
    }

    private val nodeDataMap = mutableMapOf<Path, NodeData>()

    init {
        Observable.interval(5, TimeUnit.SECONDS, scheduler)
                .subscribe { poll() }
    }

    /**
     * @param nodeConfig the configuration to be added.
     * Add a [NodeConfig] for a node which is about to be started.
     * Its nodeInfo file will be copied to other nodes' additional-node-infos directory, and conversely,
     * other nodes' nodeInfo files will be copied to this node additional-node-infos directory.
     */
    @Synchronized
    fun addConfig(nodeConfig: NodeConfig) {
        val newNodeFile = NodeData(nodeConfig.nodeDir)
        nodeDataMap[nodeConfig.nodeDir] = newNodeFile

        for (previouslySeenFile in allPreviouslySeenFiles()) {
            copy(previouslySeenFile, newNodeFile.destination.resolve(previouslySeenFile.fileName))
        }
        logger.info("Now watching: ${nodeConfig.nodeDir}")
    }

    /**
     * @param nodeConfig the configuration to be removed.
     * Remove the configuration of a node which is about to be stopped or already stopped.
     * No files written by that node will be copied to other nodes, nor files from other nodes will be copied to this
     * one.
     */
    @Synchronized
    fun removeConfig(nodeConfig: NodeConfig) {
        nodeDataMap.remove(nodeConfig.nodeDir) ?: return
        logger.info("Stopped watching: ${nodeConfig.nodeDir}")
    }

    @Synchronized
    fun reset() {
        nodeDataMap.clear()
    }

    private fun allPreviouslySeenFiles() = nodeDataMap.values.map { it.previouslySeenFiles }.flatten()

    @Synchronized
    private fun poll() {
        for (nodeFile in nodeDataMap.values) {
            val path = nodeFile.nodeDir
            path.list { paths ->
                paths.filter { it.isRegularFile() }
                        .filter { it.fileName.toString().startsWith("nodeInfo-") }
                        .forEach { path ->
                            nodeFile.previouslySeenFiles.add(path)
                            for (destination in nodeDataMap.values.map { it.destination }) {
                                val fullDestinationPath = destination.resolve(path.fileName)
                                copy(path, fullDestinationPath)
                            }
                        }
            }
        }
    }

    private fun copy(source: Path, destination: Path) {
        try {
            // REPLACE_EXISTING is needed in case we copy a file being written and we need to overwrite it with the
            // "full" file.
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING)
        } catch (exception: Exception) {
            logger.warn("Couldn't copy $source to $destination.", exception)
        }
    }

    /**
     * Convenience holder for all the paths and files relative to a single node.
     */
    private class NodeData(val nodeDir: Path) {
        val destination: Path = nodeDir.resolve(CordformNode.NODE_INFO_DIRECTORY)
        val previouslySeenFiles = mutableSetOf<Path>()

        init {
            try {
                destination.createDirectories()
            } catch (e: IOException) {
                logger.warn("Couldn't create $destination", e)
            }
        }
    }
}
