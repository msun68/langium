//@ts-check
import * as esbuild from 'esbuild';

const watch = process.argv.includes('--watch');

const ctx = await esbuild.context({
    entryPoints: ['out/rest-api/server.js'],
    outdir: 'out',
    bundle: true,
    target: "ES2020",
    format: 'esm',
    platform: 'node',
    sourcemap: true,
    external: ['express']
});

if (watch) {
    await ctx.watch();
} else {
    await ctx.rebuild();
    ctx.dispose();
}
