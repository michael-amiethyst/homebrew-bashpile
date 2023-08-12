# Documentation: https://docs.brew.sh/Formula-Cookbook
#                https://rubydoc.brew.sh/Formula
# PLEASE REMOVE ALL GENERATED COMMENTS BEFORE SUBMITTING YOUR PULL REQUEST!

class Bashpile < Formula
  desc "Bashpile: The Bash Transpiler - Write in a modern language and run in a Bash5 shell!"
  homepage "https://github.com/michael-amiethyst/homebrew-bashpile"
  url "https://github.com/michael-amiethyst/homebrew-bashpile/raw/feature/brew/deploy/bashpile.tar.gz"
  version "0.10.0"
  sha256 "d8323d8f69d9a85055c8604fda22ab0649c444b9acadef7f9ba2d67dd011443e"
  license "MIT"

  # depends_on "cmake" => :build

  def install
    # ENV.deparallelize  # if your formula fails when building in parallel
    # Remove unrecognized options if warned by configure
    # https://rubydoc.brew.sh/Formula.html#std_configure_args-instance_method
    # system "./configure", *std_configure_args, "--disable-silent-rules"
    # system "cmake", "-S", ".", "-B", "build", *std_cmake_args

    # inreplace "brew/srcclr", "##PREFIX##", "#{prefix}"
    system "cd bashpile && mvn clean verify"
    prefix.install "target/bashpile.jar"
    # todo register shell
    bin.install "bin/bpc"
    bin.install "bin/bpr"
  end

  test do
    # `test do` will create, run in and delete a temporary directory.
    #
    # This test will fail and we won't accept that! For Homebrew/homebrew-core
    # this will need to be a test that verifies the functionality of the
    # software. Run the test with `brew test homebrew-bashpile`. Options passed
    # to `brew install` such as `--HEAD` also need to be provided to `brew test`.
    #
    # The installed folder is not in the path, so use the entire path to any
    # executables being tested: `system "#{bin}/program", "do", "something"`.
    system "true"
  end
end
