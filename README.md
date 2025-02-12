## snow
A library for doing nostr things with scala. Used by [Nostr Army Knife](https://nak.nostr.com).

### Building/testing locally

> note: this project is currently scalajs only, and is not yet cross built for jvm or native

1. install [Nix](https://nixos.org) the package manager, and make sure [flakes](https://wiki.nixos.org/wiki/Flakes) are enabled
2. checkout this repo and `cd` into it
3. `nix develop` will get you into a dev environment with all the things (`sbt`, `node`)
4. `sbt esInstall && cp -a target/esbuild/. ./` will make sure that the right npm
    modules are installed locally for testing
5. `sbt test` or `sbt testOnly snow.NIP19Test` to test specific things (`NIP19Test` in this case)

#### Github Workflows

1. `sbt githubWorkflowGenerate`
2.  `git add .github/workflows/ci.yml && git commit -m "update github workflow"`