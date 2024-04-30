class Bashpile < Formula
  desc "The Bash Transpiler: Write in a modern language and run in a Bash5 shell!"
  homepage "https://github.com/michael-amiethyst/homebrew-bashpile"
  license "MIT"
  url "https://github.com/michael-amiethyst/homebrew-bashpile", using: :git, branch: "main", tag: "0.21.1"
  head "https://github.com/michael-amiethyst/homebrew-bashpile", using: :git, branch: "development"

  depends_on "gnu-sed"
  depends_on "maven" => :build
  depends_on "bc"
  depends_on "openjdk"
  depends_on "shfmt"
  depends_on "shellcheck"
  # TODO add gnu-getopt for OSX or FreeBSD only

  def install
    system "mvn", "clean", "verify", "-Dmaven.test.skip=true"
    bin.install "bin/bashpile.jar"
    bin.install "bin/bpc"
    FileUtils.cp "#{bin}/bpc", "#{bin}/bashpilec"
    bin.install "bin/bpr"
    FileUtils.cp "#{bin}/bpr", "#{bin}/bashpile"
  end

  # TODO multiline STDIN test
  test do
    assert_match "Hello Bash", shell_output("echo \"print('Hello Bash')\" | bpr -c")
    assert_match "6.28", shell_output("echo \"print(3.14 + 3.14)\" | bpr -c")
  end
end
