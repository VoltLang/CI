
def func(file)
{
	def debfile = '*.deb'
	def repo = 'https://github.com/VoltLang/Package'

	// Clear out the current working directory.
	sh """rm -f *.deb *.build *.changes"""

	// Go into a directory as the packages ends up in ..
	dir('package') {
		// Clone the repo with package information
		git poll: false, url: repo

		// Get the binary from the Volt project
		step([$class: 'CopyArtifact',
		     filter: file,
		     fingerprintArtifacts: true,
		     projectName: 'Volt'])

		// Unpack the binary and make the package
		sh """
		tar xfv ${file}
		make jenkins
		"""
	}

	archiveArtifacts artifacts: debfile, fingerprint: true, onlyIfSuccessful: true
}

stage('Build')

parallel 'ubuntu-16.04-x86_64': {
	node('ubuntu-16.04-x86_64') {
		func('battery-x86_64-linux.tar.gz')
	}
}, 'ubuntu-16.04-i386': {
	node('ubuntu-16.04-i386') {
		func('battery-x86-linux.tar.gz')
	}
}
