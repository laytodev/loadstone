package dev.loadstone.toolbox.deobfuscator

import dev.loadstone.toolbox.asm.tree.ClassGroup
import dev.loadstone.toolbox.asm.tree.ignored
import dev.loadstone.toolbox.deobfuscator.transformer.ControlFlowTransformer
import dev.loadstone.toolbox.deobfuscator.transformer.DeadCodeTransformer
import dev.loadstone.toolbox.deobfuscator.transformer.RenameTransformer
import dev.loadstone.toolbox.deobfuscator.transformer.RuntimeExceptionTransformer
import org.tinylog.kotlin.Logger
import java.io.File
import kotlin.reflect.full.createInstance

class Deobfuscator(
    private val inputJar: File,
    private val outputJar: File,
    private val runTestClient: Boolean = false
) {

    private val group = ClassGroup()
    private val transformers = mutableListOf<Transformer>()

    private fun init() {
        Logger.info("Initializing deobfuscator.")
        group.clear()

        /*
         * Register bytecode transformers.
         */
        transformers.clear()

        register<RuntimeExceptionTransformer>()
        register<DeadCodeTransformer>()
        register<ControlFlowTransformer>()
        register<RenameTransformer>()

        Logger.info("Registered ${transformers.size} bytecode transformers.")
    }

    fun run() {
        init()
        Logger.info("Starting deobfuscator.")

        /*
         * Read classes from input jar into class group.
         */
        Logger.info("Loading classes from jar: ${inputJar.name}.")
        group.readJar(inputJar) { cls ->
            if(arrayOf("org.bouncycastle", "org.json").any { cls.name.startsWith(it) }) {
                cls.ignored = true
            }
        }
        Logger.info("Successfully read ${group.size} classes from input jar.")

        /*
         * Run all bytecode transformers.
         */
        val start = System.currentTimeMillis()
        transformers.forEach { transformer ->
            Logger.info("Running bytecode transformer: ${transformer::class.simpleName}.")
            transformer.run(group)
            transformer.postRun(group)
        }
        val delta = System.currentTimeMillis() - start
        Logger.info("Successfully finished all bytecode transformers in ${delta/1000}s.")

        /*
         * Write the deobfuscated classes from class group to the output jar file.
         */
        Logger.info("Writing deobfuscated classes to jar: ${outputJar.name}.")
        group.writeJar(outputJar)
        Logger.info("Successfully wrote ${group.size} classes to output jar.")

        /*
         * If test client mode is enabled, run the test client.
         */
        if(runTestClient) {
            TestClient(outputJar, inputJar).start()
        }

        Logger.info("Successfully completed deobfuscation.")
    }

    private inline fun <reified T : Transformer> register() {
        transformers.add(T::class.createInstance())
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            if(args.size < 2) throw IllegalArgumentException("Usage deobfuscator.jar <input-jar> <output-jar> [-test]")
            val inputJar = File(args[0])
            val outputJar = File(args[1])
            val runTestClient = (args.size == 3 && args[2] == "-test")
            Deobfuscator(inputJar, outputJar, runTestClient).run()
        }
    }
}