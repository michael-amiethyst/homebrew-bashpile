# Temporary notes - to go to wiki

## Getting Started
Pull the code with HTTPS and commit with GitHub Desktop or personal token

## Setup FileTypes
Python syntax highlighting for now?

## Register bps, bpr with /usr/bin/env
TODO

## Setup Run Configuration
`clean verify -f pom.xml` on WSL
Maven Home on WSL
Java JRE on WSL, Modify Settings to get Environment variables, set JAVA_HOME=Linux style

## Deploy with Brew

Just run bin/deploy

Created with
`brew create --set-version 0.10.0 --tap michael-amiethyst/bashpile https://github.com/michael-amiethyst/homebrew-bashpile/raw/feature/brew/deploy/bashpile-project.tar.gz`
so
`brew test --set-version 0.10.0 --tap michael-amiethyst/bashpile`
to test and
`brew install Formula/homebrew-bashpile.rb`
to install.

Fresh pull: `brew cleanup --prune=all && brew reinstall Formula/bashpile.rb`
Audit: `brew audit --new bashpile`

Interactive: `brew install --interactive Formula/homebrew-bashpile.rb`
Debug: `HOMEBREW_NO_INSTALL_FROM_API=1 brew install --build-from-source --verbose --debug --HEAD Formula/homebrew-bashpile.rb`