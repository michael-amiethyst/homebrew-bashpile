class Bashpile < Formula
  desc "The Bash Transpiler - Write in a modern language and run in a Bash5 shell!"
  homepage "https://github.com/michael-amiethyst/homebrew-bashpile"
  url "https://github.com/michael-amiethyst/homebrew-bashpile/raw/main/deploy/bashpile.tar.gz"
  head do
    url "https://github.com/michael-amiethyst/homebrew-bashpile", using: :git, branch: "feature/brew" #, tag: "0.10.0"
    depends_on "gnu-sed" => :build
    depends_on "maven" => :build
  end
  version "0.10.0"
  sha256 "4da06d4f6b01fe331d1d78ce0f224022f0275bd637b86180ecbaa93e378c7c8f"
  license "MIT"

  depends_on "bc"
  depends_on "openjdk"
  depends_on "shellcheck"

  def install
    if build.head?
      system "mvn", "clean", "verify", "-Dskip.update.formula=true", "-Dskip.failsafe.tests=true"
    end
    bin.install "bin/bashpile.jar"
    bin.install "bin/bpc"
    bin.install "bin/bpr"
  end

  test do
    system "true"
  end
end
