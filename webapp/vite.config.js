import { spawnSync } from "child_process";
import { defineConfig } from "vite";
import wasm from "vite-plugin-wasm"
import topLevelAwait from "vite-plugin-top-level-await"

function isDev() {
  return process.env.NODE_ENV !== "production";
}

function printSbtTask(task) {
  const args = ["--error", "--batch", `print ${task}`];
  const options = {
    stdio: [
      "pipe", // StdIn.
      "pipe", // StdOut.
      "inherit", // StdErr.
    ],
    cwd: "../"
  };
  const result = process.platform === 'win32'
    ? spawnSync("sbt.bat", args.map(x => `"${x}"`), {shell: true, ...options})
    : spawnSync("sbt", args, options);

  if (result.error)
    throw result.error;
  if (result.status !== 0)
    throw new Error(`sbt process failed with exit code ${result.status}`);
  return result.stdout.toString('utf8').trim();
}

const replacementForPublic = isDev()
  ? printSbtTask("webapp/publicDev")
  : printSbtTask("webapp/publicProd");

export default defineConfig({
  plugins: [topLevelAwait(), wasm()],
  root : "./src/main/html",
  // This is only necessary if you are using `SharedWorker` or `WebWorker`, as
  // documented in https://vitejs.dev/guide/features.html#import-with-constructors
  worker: {
    format: "es",
    plugins: [topLevelAwait(), wasm()]
  },

  optimizeDeps: {
    // This is necessary because otherwise `vite dev` includes two separate
    // versions of the JS wrapper. This causes problems because the JS
    // wrapper has a module level variable to track JS side heap
    // allocations, initializing this twice causes horrible breakage
    exclude: ["@automerge/automerge-wasm"]
  },
  resolve: {
    alias: [
      {
        find: "@public",
        replacement: replacementForPublic,
      },
    ],
  },
});
