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

Created with
`brew create --set-version 0.10.0 --tap michael-amiethyst/bashpile target/bashpile.jar`
so
`brew test --HEAD --set-version 0.10.0 --tap michael-amiethyst/bashpile`
to test and
`brew install --HEAD Formula/homebrew-bashpile.rb`
to install.

Interactive: `HOMEBREW_NO_INSTALL_FROM_API=1 brew install --interactive --HEAD Formula/homebrew-bashpile.rb`
Debug: `HOMEBREW_NO_INSTALL_FROM_API=1 brew install --build-from-source --verbose --debug --HEAD Formula/homebrew-bashpile.rb`