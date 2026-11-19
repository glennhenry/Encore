/**
 * The following file contains pieces of devtools and utility code.
 *
 * Access to the tools are written in tests as main function instead of
 * independent dev endpoint for simplicity.
 *
 * It should only contain interfaces, not actual tools.
 * The actual tools are written in the main packages.
 *
 * How to create the interface?
 * - Create `object` with a `main` function and annotate with `@JvmStatic`
 * - The `main` function contains the code you want to run.
 *   Use it, for example, to run certain tools with the input args.
 *
 * How to run?
 * - Click the run button next to the main function.
 * - Use from terminal like `.\gradlew runTool -Ptool=<toolname>`
 * - Optionally, supply args with `--args=...`
 */
