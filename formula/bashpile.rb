class Bashpile < Formula
  desc "The Bash Transpiler - Write in a modern language and run in a Bash5 shell!"
  homepage "https://github.com/michael-amiethyst/homebrew-bashpile"
  url "https://github.com/michael-amiethyst/homebrew-bashpile/raw/feature/brew/deploy/bashpile.tar.gz"
  version "0.10.0"
  sha256 "2d99036ef73aa7ad11d208db6938692176986fa90dfc08fd475f6bac59371809"
  license "MIT"

  depends_on "bc"
  depends_on "openjdk"
  depends_on "shellcheck"

  # build depends on sed

  def install
    bin.install "bin/bashpile.jar"
    # TODO: register shell
    bin.install "bin/bpc"
    bin.install "bin/bpr"
  end

  test do
    system "true"
  end
end
