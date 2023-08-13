class Bashpile < Formula
  desc "Bashpile: The Bash Transpiler - Write in a modern language and run in a Bash5 shell!"
  homepage "https://github.com/michael-amiethyst/homebrew-bashpile"
  url "https://github.com/michael-amiethyst/homebrew-bashpile/raw/feature/brew/deploy/bashpile.tar.gz"
  version "0.10.0"
#   sha256 "76a3423b1f2ba4dabcbf7f0aef7b54dd678e187faa9f7c2d011202ba8e13ff7d"
  license "MIT"

  depends_on "openjdk"
  depends_on "bc"
  depends_on "shellcheck"

  def install
    bin.install "bin/bashpile.jar"
    # todo register shell
    bin.install "bin/bpc"
    bin.install "bin/bpr"
  end

  test do
    system "true"
  end
end
