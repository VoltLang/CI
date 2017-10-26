@Library('volt-build')
import org.volt_lang.Builder

def b = new Builder(steps, null)

stage('Setup') {
	b.setupVolta()
}
stage('Checkout') {
	b.checkout()
}
stage('Prepare') {
	b.prepare()
}
stage('Build') {
	b.build()
}
stage('Test') {
	b.test()
}
stage('Archive') {
	b.archive()
}
stage('Toolchain') {
	b.dispatch(this.&doToolchain, false, null)
}

// Archiving the toolchain is destructive, run it last.
def doToolchain(root, arch, plat, arg)
{
	def name = "${arch}-${plat}"
	def d = "${root}/toolchain"
	dir(d) {
		def tarFile = "toolchain-${name}.tar.gz"
		deleteDir()
		sh """
		mkdir -p bin
		mkdir -p lib
		cp ../${name}/volta bin
		mv ../bin/battery bin
		mv ../src/volta/rt lib
		mv ../src/watt lib
		mv ../src/amp lib
		rm -rf lib/rt/test
		rm -rf `find lib/watt -type d -name test`
		rm -rf lib/amp/test
		tar -czf ${tarFile} *
		"""
		archiveArtifacts artifacts: tarFile, fingerprint: true, onlyIfSuccessful: true
	}
}
