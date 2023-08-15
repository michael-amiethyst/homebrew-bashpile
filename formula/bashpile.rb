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
  sha256 "fe007a1a96f59a82b0c261c0eaeee0b1fd489de8ea93f69186818eef40760e2f"
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
