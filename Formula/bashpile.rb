class Bashpile < Formula
  desc "Bashpile - The Bash Transpiler: Write in a modern language and run in a Bash5 shell!"
  homepage "https://github.com/michael-amiethyst/homebrew-bashpile"
  url "https://github.com/michael-amiethyst/homebrew-bashpile", using: :git, branch: "main", tag: "0.10.1"
  head "https://github.com/michael-amiethyst/homebrew-bashpile", using: :git, branch: "feature/conditionals"
  license "MIT"

  depends_on "gnu-sed" => :build
  depends_on "maven" => :build
  depends_on "bc"
  depends_on "openjdk"
  depends_on "shellcheck"

  def install
    system "mvn", "clean", "verify", "-Dskip.failsafe.tests=true"
    bin.install "bin/bashpile.jar"
    bin.install "bin/bpc"
    system "cp", "#{bin}/bpc", "#{bin}/bashpilec"
    bin.install "bin/bpr"
    system "cp", "#{bin}/bpr", "#{bin}/bashpile"
  end

  test do
    system "true"
  end
end
