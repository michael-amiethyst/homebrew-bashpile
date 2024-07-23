class Bashpile < Formula
  desc "The Bash Transpiler: Write in a modern language and run in a Bash5 shell!"
  homepage "https://github.com/michael-amiethyst/homebrew-bashpile"
  license "MIT"
  url "https://github.com/michael-amiethyst/homebrew-bashpile", using: :git, branch: "main", tag: "0.21.8"
  # TODO change head back to develop
  head "https://github.com/michael-amiethyst/homebrew-bashpile", using: :git, branch: "hotfix/debian-PATH-fix"

  # foundational dependencies
  depends_on "openjdk"
  depends_on "bash"
  depends_on "maven" => :build

  # tooling dependencies for compilation
  depends_on "shfmt"
  depends_on "shellcheck"

  # tooling dependencies for generated scripts
  depends_on "gnu-sed"
  depends_on "bc"
  depends_on "gnu-getopt" # needed for OSX and FreeBSD, kept as generic dependency for consistency

  def install
    system "mvn", "clean", "verify"
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

  def caveats
    <<~EOS
      OSX Only: By default, the Bash5 and GNU-getopt dependencies will be installed to:
      	#{HOMEBREW_PREFIX}/Cellar/bash/<VERSION>/bin
      	and
      	#{HOMEBREW_PREFIX}/Cellar/gnu-getopt/<VERSION>/bin

      You will need to add these to the front of your PATH for Bashpile to work correctly.
    EOS
  end
end
